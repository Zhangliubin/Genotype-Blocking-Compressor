package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.compressor.IDecompressor;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNodes;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantQC;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @Data        :2021/08/30
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :二级树表
 */

class SecondLevelTree {
    int blockSize;
    final int[] counts;
    final int[] pointer;
    final boolean[] endOfChromosome;
    SmartList<Integer> chromosomeList;

    final GTBManager[] managers;
    final VariantQC variantQC;
    HashMap<Integer, MergePositionGroup> cacheVariant = new HashMap<>();

    VolumeByteStream undecompressedCache = new VolumeByteStream();
    VolumeByteStream decompressedPosCache;
    VolumeByteStream decompressedAllelesCache;
    FileStream[] fileStreams;
    IDecompressor[] decompressors;

    int initMaxBlockSize(GTBManager[] managers) {
        int maxSize = 0;
        for (GTBManager manager : managers) {
            int currentSize = manager.getBlockSize();
            if (currentSize > maxSize) {
                maxSize = currentSize;
            }
        }

        return maxSize;
    }

    int initMaxOriginAllelesSize(GTBManager[] managers) {
        int maxSize = 0;
        for (GTBManager manager : managers) {
            int currentSize = manager.getMaxDecompressedAllelesSize();
            if (currentSize > maxSize) {
                maxSize = currentSize;
            }
        }

        return maxSize;
    }

    public SecondLevelTree(GTBManager[] managers, VariantQC variantQC, int blockSize) throws IOException {
        this.counts = new int[managers.length];
        this.pointer = new int[managers.length];
        this.endOfChromosome = new boolean[managers.length];
        this.managers = managers;
        this.variantQC = variantQC;
        this.blockSize = blockSize;

        // 获取所有的染色体编号，并合并
        HashSet<Integer> chromosomeSet = new HashSet<>(24);
        for (GTBManager manager : managers) {
            chromosomeSet.addAll(ArrayUtils.toSet(manager.getChromosomeList()));
        }
        chromosomeList = new SmartList<>(chromosomeSet.size());
        chromosomeList.addAll(chromosomeSet);

        this.decompressedPosCache = new VolumeByteStream(initMaxBlockSize(managers) * 4);
        this.decompressedAllelesCache = new VolumeByteStream(initMaxOriginAllelesSize(managers) * 4);

        // 压缩器
        HashMap<Integer, IDecompressor> globalDecompressors = new HashMap<>();
        for (GTBManager gtbManager : managers) {
            if (!globalDecompressors.containsKey(gtbManager.getCompressorIndex())) {
                globalDecompressors.put(gtbManager.getCompressorIndex(), IDecompressor.getInstance(gtbManager.getCompressorIndex()));
            }
        }

        this.fileStreams = new FileStream[managers.length];
        this.decompressors = new IDecompressor[managers.length];
        for (int i = 0; i < managers.length; i++) {
            fileStreams[i] = managers[i].getFileStream();
            decompressors[i] = globalDecompressors.get(managers[i].getCompressorIndex());
        }
    }

    /**
     * 创建合并树
     */
    public void loadNext(short managerIndex, int chromosomeIndex) throws IOException {
        GTBNodes nodes = managers[managerIndex].getGTBNodes(chromosomeIndex);
        if (nodes == null || nodes.numOfNodes() == pointer[managerIndex]) {
            endOfChromosome[managerIndex] = true;
            return;
        }

        for (int j = pointer[managerIndex]; j < nodes.numOfNodes(); j++) {
            GTBNode node = nodes.get(j);
            decompressedAllelesCache.reset();
            decompressedPosCache.reset();
            undecompressedCache.reset();
            undecompressedCache.makeSureCapacity(node.compressedAlleleSize, node.compressedPosSize);

            // 读取压缩后的位置数据
            fileStreams[managerIndex].seek(node.blockSeek + node.compressedGenotypesSize);
            fileStreams[managerIndex].read(undecompressedCache, node.compressedPosSize);
            decompressors[managerIndex].decompress(undecompressedCache, decompressedPosCache);
            undecompressedCache.reset();

            // 读取压缩后的位置数据
            fileStreams[managerIndex].read(undecompressedCache, node.compressedAlleleSize);
            decompressors[managerIndex].decompress(undecompressedCache, decompressedAllelesCache);

            int startPos;
            int endPos = -1;

            // 还原位置数据，并构建红黑树表
            for (int k = 0; k < node.numOfVariants(); k++) {
                // 设置等位基因数据
                startPos = endPos;
                endPos = decompressedAllelesCache.indexOfN(ByteCode.SLASH, startPos + 1, 1);

                // 还原位置值
                int position = ValueUtils.byteArray2IntegerValue(decompressedPosCache.cacheOf(k << 2),
                        decompressedPosCache.cacheOf(1 + (k << 2)),
                        decompressedPosCache.cacheOf(2 + (k << 2)),
                        decompressedPosCache.cacheOf(3 + (k << 2)));

                if (!cacheVariant.containsKey(position)) {
                    cacheVariant.put(position, new MergePositionGroup(position, managers.length));
                }

                byte[] alleles = decompressedAllelesCache.cacheOf(startPos + 1, endPos);

                // 记录等位基因数
                byte allelesNum = 2;
                for (int l = 3; l < alleles.length; l++) {
                    if (alleles[l] == ByteCode.COMMA) {
                        allelesNum += 1;
                    }
                }

                if (!this.variantQC.filter(null, allelesNum, 0, 0, 0, 0)) {
                    // i 表示文件索引，j 表示对应的节点编号, k 表示在该节点内的第 k 个位点
                    cacheVariant.get(position).add(alleles, allelesNum, managerIndex, j, k);
                    counts[managerIndex]++;
                }
            }

            pointer[managerIndex]++;
            // 已经达到解压数量目标，则往后挪动
            if (counts[managerIndex] >= blockSize) {
                break;
            }
        }
    }

    public MergeKernel.Task get() throws IOException {
        int currentChromosome = chromosomeList.get(0);
        for (short i = 0; i < pointer.length; i++) {
            if (!endOfChromosome[i] && counts[i] < blockSize) {
                loadNext(i, currentChromosome);
            }
        }

        if (cacheVariant.size() > 0) {
            // 只要还有位点就取出来
            int[] positions = ArrayUtils.getIntegerKey(cacheVariant);
            int count = positions.length;
            int totalVariantsNum = 0;
            Arrays.sort(positions);

            for (int i = 0; i < positions.length; i++) {
                totalVariantsNum += cacheVariant.get(positions[i]).getSize();
                if (totalVariantsNum > blockSize) {
                    count = i;
                    break;
                } else {
                    cacheVariant.get(positions[i]).finish();
                }
            }

            MergePositionGroup[] task = new MergePositionGroup[count];
            for (int i = 0; i < count; i++) {
                task[i] = cacheVariant.get(positions[i]);

                for (MergePositionGroup.MultiAlleleGroup alleleGroups : task[i].groups) {
                    for (VariantCoordinate variant : alleleGroups) {
                        counts[variant.managerIndex] -= 1;
                    }

                }
                cacheVariant.remove(positions[i]);
            }

            return new MergeKernel.Task(currentChromosome, task);
        } else {
            // 没有位点了
            if (chromosomeList.size() > 1) {
                chromosomeList.popFirst();
                init(pointer, 0);
                init(counts, 0);
                init(endOfChromosome, false);
                return get();
            } else {
                // 结束
                for (FileStream fs : this.fileStreams) {
                    if (!fs.isClosed()) {
                        fs.close();
                    }
                }
                return null;
            }
        }
    }

    private void init(int[] arrs, int value) {
        Arrays.fill(arrs, value);
    }

    private void init(boolean[] arrs, boolean value) {
        Arrays.fill(arrs, value);
    }
}