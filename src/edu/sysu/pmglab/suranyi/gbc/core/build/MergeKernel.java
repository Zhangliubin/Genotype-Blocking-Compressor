package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.compressor.IDecompressor;
import edu.sysu.pmglab.suranyi.container.Pair;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.container.ShareCache;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.suranyi.gbc.coder.encoder.MBEGEncoder;
import edu.sysu.pmglab.suranyi.gbc.coder.BEGTransfer;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.common.block.VariantAbstract;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.*;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantQC;
import edu.sysu.pmglab.suranyi.threadPool.Block;
import edu.sysu.pmglab.suranyi.threadPool.DynamicPipeline;
import edu.sysu.pmglab.suranyi.threadPool.ThreadPool;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

/**
 * @Data        :2021/02/14
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :Merge 核心任务
 */

class MergeKernel {
    /**
     * 一个 kernel 只能绑定一个任务
     */
    final MergeTask task;

    /**
     * 编码器
     */
    final BEGEncoder encoder;
    final MBEGEncoder groupEncoder;

    /**
     * IO 数据管道
     */
    final DynamicPipeline<Boolean, Task> uncompressedPipLine;

    /**
     * 合并文件标记信息
     */
    final int[] indexOffsets;
    final int[] indexLengths;
    final int[] phasedTransfers;
    final int validSubjectNum;
    final int blockSize;
    final int maxOriginMBEGsSize;

    /**
     * 根数据管理器
     */
    final FileBaseInfoManager baseInfoManager;
    final ArrayList<GTBNode> GTBNodeCache = new ArrayList<>(1024);
    int maxEstimateSize = 0;

    /**
     * 输出数据文件
     */
    final FileStream outputFile;
    final GTBManager[] managers;

    /**
     * 位点控制器、等位基因水平过滤器
     */
    final VariantQC variantQC;
    final AlleleQC alleleQC;

    boolean status;

    /**
     * 对外的提交方法，将任务提交至本类，进行压缩任务
     */
    static void submit(MergeTask task) throws IOException {
        new MergeKernel(task);
    }

    GTBSubjectManager initSubjectManager(GTBManager[] managers) {
        // 加载全部的样本，并构建双向索引表
        VolumeByteStream vbs = new VolumeByteStream(2 << 20);
        vbs.writeSafety(managers[0].getSubjects());
        for (int i = 1; i < managers.length; i++) {
            vbs.writeSafety(ByteCode.TAB);
            vbs.writeSafety(managers[i].getSubjects());
        }

        return new GTBSubjectManager(vbs);
    }

    int[] initIndexLength(GTBManager[] managers) {
        // 传入所有的管理器
        int[] lengths = new int[managers.length];
        for (int i = 0; i < managers.length; i++) {
            lengths[i] = managers[i].getSubjectNum();
        }

        return lengths;
    }

    int[] initIndexOffsets(int[] lengths) {
        // 传入所有的管理器
        int[] offsets = new int[lengths.length];
        offsets[0] = 0;
        for (int i = 1; i < offsets.length; i++) {
            offsets[i] = offsets[i - 1] + lengths[i - 1];
        }

        return offsets;
    }

    int[] initPhasedTransfer(boolean targetPhased, GTBManager[] managers) {
        // 传入所有的管理器
        if (targetPhased) {
            return null;
        }

        ArrayList<Integer> transferIndex = new ArrayList<>(managers.length);
        for (int i = 0; i < managers.length; i++) {
            if (managers[i].isPhased()) {
                transferIndex.add(i);
            }
        }

        return transferIndex.size() == 0 ? null : ArrayUtils.toIntegerArray(transferIndex);
    }

    int initMaxOriginMBEGsSize(GTBManager[] managers) {
        int maxSize = 0;
        for (GTBManager manager : managers) {
            int currentSize = manager.getMaxDecompressedMBEGsSize();
            if (currentSize > maxSize) {
                maxSize = currentSize;
            }
        }

        return maxSize;
    }

    /**
     * 标准构造器，传入 EditTask，根据该提交任务执行工作
     * @param task 待执行任务
     */
    MergeKernel(MergeTask task) throws IOException {
        // 加载 MBEG 编码表
        this.encoder = BEGEncoder.getEncoder(task.isPhased());
        this.groupEncoder = MBEGEncoder.getEncoder(task.isPhased());

        // 设置任务
        this.task = task;

        // 设定管理器
        this.managers = this.task.getManagers().toArray();
        this.variantQC = task.getVariantQC();
        this.alleleQC = task.getAlleleQC();

        // 创建数据管道
        this.uncompressedPipLine = new DynamicPipeline<>(task.getThreads() << 2);

        // 总体样本管理器，该步骤也能保证文件没有交集
        GTBSubjectManager subjectManager = initSubjectManager(this.managers);

        // 设定任务索引对及样本偏移量
        this.indexLengths = initIndexLength(managers);
        this.indexOffsets = initIndexOffsets(indexLengths);

        // 向型转换器
        this.phasedTransfers = initPhasedTransfer(task.isPhased(), managers);

        // 总样本个数
        this.validSubjectNum = ValueUtils.sum(indexLengths);

        // 验证块大小参数
        int blockSizeType = BlockSizeParameter.getSuggestBlockSizeType(task.getBlockSizeType(), validSubjectNum);
        this.blockSize = BlockSizeParameter.getBlockSize(blockSizeType);
        this.baseInfoManager = FileBaseInfoManager.of(task);
        this.baseInfoManager.setBlockSizeType(blockSizeType);

        // 解压基本参数
        this.maxOriginMBEGsSize = initMaxOriginMBEGsSize(managers);

        // 设置输出数据文件
        this.outputFile = new FileStream(task.isInplace() ? this.task.getOutputFileName() + ".~$temp" : this.task.getOutputFileName(), FileOptions.CHANNEL_WRITER);

        // 写入初始头信息
        this.outputFile.write(ValueUtils.value2ByteArray(0, 5));

        // 参考地址以 0 号文件为准
        if (this.managers[0].getReference().size() != 0) {
            // 写入 refer 网址
            this.outputFile.write(this.managers[0].getReference());
        }

        // 写入换行符
        this.outputFile.write(ByteCode.NEWLINE);

        // 写入样本名
        VolumeByteStream subjectSeq = ICompressor.compress(task.getCompressor(), task.getCompressionLevel(), subjectManager.getSubjects(), 0, subjectManager.getSubjects().length);
        this.outputFile.writeIntegerValue(subjectSeq.size());
        this.outputFile.write(subjectSeq);

        // 创建线程池
        ThreadPool threadPool = new ThreadPool(this.task.getThreads() + 1);

        // 创建 Input 线程
        threadPool.submit(() -> {
            try {
                SecondLevelTree secondLevelTree = new SecondLevelTree(this.managers, this.variantQC, blockSize);
                MergeKernel.Task tasks;
                while ((tasks = secondLevelTree.get()) != null) {
                    // System.out.println(tasks.candidateNodes[0].position);
                    this.uncompressedPipLine.put(true, tasks);
                }

                this.uncompressedPipLine.putStatus(this.task.getThreads(), false);
            } catch (InterruptedException | IOException e) {
                // e.printStackTrace();
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        // 创建工作线程及其上下文数据
        IndexPair[][] indexPairs = new IndexPair[managers.length][];
        int[][] eachLineSize = new int[managers.length][2];
        int[] groupDecoderIndexes = new int[managers.length];
        for (int i = 0; i < indexPairs.length; i++) {
            int groupNum = managers[i].isPhased() ? 3 : 4;
            indexPairs[i] = new IndexPair[this.indexLengths[i]];

            for (int j = 0; j < indexPairs[i].length; j++) {
                indexPairs[i][j] = new IndexPair(this.indexOffsets[i] + j, j, j / groupNum, j % groupNum);
            }

            int resBlockCodeGenotypeNum = managers[i].getSubjectNum() % groupNum;
            eachLineSize[i][0] = (managers[i].getSubjectNum() / groupNum) + (resBlockCodeGenotypeNum == 0 ? 0 : 1);
            eachLineSize[i][1] = managers[i].getSubjectNum();
            groupDecoderIndexes[i] = managers[i].isPhased() ? 1 : 0;
        }

        threadPool.submit(() -> {
                    try {
                        // 创建解压器
                        HashMap<Integer, IDecompressor> decompressors = new HashMap<>();
                        for (GTBManager gtbManager : managers) {
                            if (!decompressors.containsKey(gtbManager.getCompressorIndex())) {
                                decompressors.put(gtbManager.getCompressorIndex(), IDecompressor.getInstance(gtbManager.getCompressorIndex()));
                            }
                        }

                        if (task.getAlleleQC().size() > 0) {
                            mergeBlockWithFilter(indexPairs, eachLineSize, groupDecoderIndexes, decompressors);
                        } else {
                            mergeBlock(indexPairs, eachLineSize, groupDecoderIndexes, decompressors);
                        }
                    } catch (Exception e) {
                        // e.printStackTrace();
                        throw new UnsupportedOperationException(e.getMessage());
                    }
                }
                , this.task.getThreads());

        // 关闭线程池，等待任务完成
        threadPool.close();

        // 生成最后的 gtb 文件
        generateGTBFile();

        // 标记为完成任务
        this.status = true;

        // 关闭数据管道
        release();
    }

    static class Task {
        int chromosomeIndex;
        MergePositionGroup[] candidateNodes;

        public Task(int chromosomeIndex, MergePositionGroup[] candidateNodes) {
            this.chromosomeIndex = chromosomeIndex;
            this.candidateNodes = candidateNodes;
        }

    }

    /**
     * 解除修改锁定，解除后无法保证数据安全，直接清除本类数据
     */
    void release() throws IOException {
        // 如果是错误结束的，则需要删除文件
        if (!this.status) {
            this.outputFile.delete();
        }
    }

    /**
     * 合并多个 block 的信息
     */
    void mergeBlock(IndexPair[][] indexPairs, int[][] eachLineSize, int[] groupDecoderIndexes, HashMap<Integer, IDecompressor> decompressors) {
        try {
            Block<Boolean, Task> taskBlock = this.uncompressedPipLine.get();

            if (taskBlock.getStatus()) {
                // 文件流
                FileStream[] fileStreams = new FileStream[this.managers.length];

                // 打开文件
                for (int i = 0; i < this.managers.length; i++) {
                    // 文件操作流
                    fileStreams[i] = this.managers[i].getFileStream();
                }

                // 未解压数据缓冲区
                VolumeByteStream unDecompressedCache = new VolumeByteStream((blockSize * Math.max(20, validSubjectNum)) >> 1);

                // 解压数据缓冲区
                VolumeByteStream genotypeCache = new VolumeByteStream(this.maxOriginMBEGsSize);

                // 创建本地编码缓冲区
                VolumeByteStream encodedCache = new VolumeByteStream(this.validSubjectNum * this.blockSize);

                // 未压缩的块
                UncompressedBlock uncompressedBlock = new UncompressedBlock(this.validSubjectNum, this.task, this.blockSize, encodedCache);

                // 创建压缩上下文
                ShareCache caches = new ShareCache(encodedCache, unDecompressedCache);
                GTBCompressionContext ctx = new GTBCompressionContext(this.task, this.validSubjectNum, caches);

                do {
                    // 提取任务
                    MergePositionGroup[] candidateVariants = taskBlock.getData().candidateNodes;
                    int chromosomeIndex = taskBlock.getData().chromosomeIndex;

                    // 当前解压的节点索引，如果索引一致，则不会进行解压
                    int currentNodeIndex;

                    IDecompressor decompressor;
                    VariantCoordinate info;
                    int secondBlockStart;
                    byte[] encodedCacheBase = encodedCache.getCache();

                    // 先填充所有的位点碱基序列
                    uncompressedBlock.seek = 0;
                    for (MergePositionGroup positionGroup : candidateVariants) {
                        for (MergePositionGroup.MultiAlleleGroup alleleGroup : positionGroup.groups) {
                            VariantAbstract variant = uncompressedBlock.getCurrentVariant();
                            variant.position = positionGroup.position;
                            variant.setAllele(alleleGroup.allelesInfo, 0, alleleGroup.allelesInfo.length);
                            variant.encoderIndex = alleleGroup.allelesNum == 2 ? 0 : 1;
                            uncompressedBlock.seek++;
                        }
                    }

                    for (int managerIndex = 0; managerIndex < managers.length; managerIndex++) {
                        // 切换管理器时必须重置索引编号
                        GTBNode node = null;
                        currentNodeIndex = -1;
                        secondBlockStart = -1;
                        decompressor = decompressors.get(managers[managerIndex].getCompressorIndex());
                        uncompressedBlock.seek = 0;

                        // 依次对每一个候选位点进行处理
                        for (MergePositionGroup candidateVariant : candidateVariants) {
                            for (MergePositionGroup.MultiAlleleGroup alleleGroups : candidateVariant.groups) {
                                VariantAbstract variant = uncompressedBlock.getCurrentVariant();
                                MergePositionGroup.AlleleGroup alleleGroup = alleleGroups.getAlleleGroup(managerIndex);

                                // 包含此位点时，进行复原
                                if (alleleGroup != null && (info = alleleGroup.getVariantCoordinate(managerIndex)) != null) {
                                    // 若当前的任务节点一致
                                    if (currentNodeIndex != info.nodeIndex) {
                                        unDecompressedCache.reset();
                                        decompressor.reset();
                                        genotypeCache.reset();
                                        node = managers[managerIndex].getGTBNodes(chromosomeIndex).get(info.nodeIndex);
                                        unDecompressedCache.makeSureCapacity(node.compressedGenotypesSize);

                                        fileStreams[managerIndex].seek(node.blockSeek);
                                        fileStreams[managerIndex].read(unDecompressedCache, node.compressedGenotypesSize);
                                        decompressor.decompress(unDecompressedCache, genotypeCache);
                                        currentNodeIndex = info.nodeIndex;
                                        secondBlockStart = eachLineSize[managerIndex][0] * node.subBlockVariantNum[0];
                                    }

                                    byte[] transCode = alleleGroup.transCode;
                                    // 还原基因型数据
                                    if (transCode.length == 2) {
                                        // 还原基因型数据
                                        if (alleleGroup.trans) {
                                            for (IndexPair indexPair : indexPairs[managerIndex]) {
                                                byte code = BEGTransfer.groupDecode(groupDecoderIndexes[managerIndex], genotypeCache.cacheOf(eachLineSize[managerIndex][0] * info.variantIndex + indexPair.groupIndex) & 0xFF, indexPair.codeIndex);
                                                encodedCacheBase[variant.encodedStart + indexPair.seqIndex] = code == 0 ? 0 : this.encoder.encode(transCode[BEGDecoder.decodeHaplotype(0, code)], transCode[BEGDecoder.decodeHaplotype(1, code)]);
                                            }
                                        } else {
                                            for (IndexPair indexPair : indexPairs[managerIndex]) {
                                                encodedCacheBase[variant.encodedStart + indexPair.seqIndex] = BEGTransfer.groupDecode(groupDecoderIndexes[managerIndex], genotypeCache.cacheOf(eachLineSize[managerIndex][0] * info.variantIndex + indexPair.groupIndex) & 0xFF, indexPair.codeIndex);
                                            }
                                        }
                                    } else {
                                        // 基因型从 Pm  开始
                                        int srcStart = secondBlockStart + eachLineSize[managerIndex][1] * (info.variantIndex - node.subBlockVariantNum[0]);
                                        int dstStart = variant.encodedStart + indexOffsets[managerIndex];

                                        if (alleleGroup.trans) {
                                            for (int i = 0; i < indexLengths[managerIndex]; i++) {
                                                byte code = genotypeCache.cacheOf(srcStart + i);
                                                encodedCacheBase[dstStart + i] = code == 0 ? 0 : this.encoder.encode(transCode[BEGDecoder.decodeHaplotype(0, code)], transCode[BEGDecoder.decodeHaplotype(1, code)]);
                                            }
                                        } else {
                                            for (int i = 0; i < indexLengths[managerIndex]; i++) {
                                                encodedCacheBase[dstStart + i] = genotypeCache.cacheOf(srcStart + i);
                                            }
                                        }

                                    }

                                } else {
                                    // 不包含此位点，填充空基因型
                                    for (IndexPair indexPair : indexPairs[managerIndex]) {
                                        encodedCacheBase[variant.encodedStart + indexPair.seqIndex] = this.encoder.encodeMiss();
                                    }
                                }

                                // 编码完成，挪动指针
                                uncompressedBlock.seek++;
                            }
                        }
                    }

                    uncompressedBlock.chromosomeIndex = chromosomeIndex;

                    for (MergePositionGroup variant : candidateVariants) {
                        variant.freeMemory();
                    }
                    processGTBBlock(ctx, uncompressedBlock);
                    taskBlock = this.uncompressedPipLine.get();
                } while (taskBlock.getStatus());

                // 关闭上下文
                genotypeCache.close();
                ctx.close();
                caches.freeMemory();

                // 关闭文件
                for (int i = 0; i < this.managers.length; i++) {
                    // 文件操作流
                    fileStreams[i].close();
                }
            }
        } catch (IOException | InterruptedException e) {
            // e.printStackTrace();  // 调试使用
            throw new UnsupportedOperationException(e.getMessage());
        }
    }

    /**
     * 合并多个 block 的信息
     */
    void mergeBlockWithFilter(IndexPair[][] indexPairs, int[][] eachLineSize, int[] groupDecoderIndexes, HashMap<Integer, IDecompressor> decompressors) {
        try {
            Block<Boolean, Task> taskBlock = this.uncompressedPipLine.get();

            if (taskBlock.getStatus()) {
                // 文件流
                FileStream[] fileStreams = new FileStream[this.managers.length];

                // 打开文件
                for (int i = 0; i < this.managers.length; i++) {
                    // 文件操作流
                    fileStreams[i] = this.managers[i].getFileStream();
                }

                // 未解压数据缓冲区
                VolumeByteStream unDecompressedCache = new VolumeByteStream((blockSize * Math.max(20, validSubjectNum)) >> 1);

                // 解压数据缓冲区
                VolumeByteStream genotypeCache = new VolumeByteStream(this.maxOriginMBEGsSize);

                // 创建本地编码缓冲区
                VolumeByteStream encodedCache = new VolumeByteStream(this.validSubjectNum * this.blockSize);

                // 未压缩的块
                UncompressedBlock uncompressedBlock = new UncompressedBlock(this.validSubjectNum, this.task, this.blockSize, encodedCache);

                // 创建压缩上下文
                ShareCache caches = new ShareCache(encodedCache, unDecompressedCache);
                GTBCompressionContext ctx = new GTBCompressionContext(this.task, this.validSubjectNum, caches);

                do {
                    // 提取任务
                    MergePositionGroup[] candidateVariants = taskBlock.getData().candidateNodes;
                    int chromosomeIndex = taskBlock.getData().chromosomeIndex;

                    // 当前解压的节点索引，如果索引一致，则不会进行解压
                    int currentNodeIndex;

                    IDecompressor decompressor;
                    VariantCoordinate info;
                    int secondBlockStart;
                    byte[] encodedCacheBase = encodedCache.getCache();

                    // 先填充所有的位点碱基序列
                    uncompressedBlock.seek = 0;
                    for (MergePositionGroup positionGroup : candidateVariants) {
                        for (MergePositionGroup.MultiAlleleGroup alleleGroup : positionGroup.groups) {
                            VariantAbstract variant = uncompressedBlock.getCurrentVariant();
                            variant.position = positionGroup.position;
                            variant.setAllele(alleleGroup.allelesInfo, 0, alleleGroup.allelesInfo.length);
                            variant.encoderIndex = alleleGroup.allelesNum == 2 ? 0 : 1;
                            uncompressedBlock.seek++;
                        }
                    }

                    // 对每个管理器解压数据
                    for (int managerIndex = 0; managerIndex < managers.length; managerIndex++) {
                        // 切换管理器时必须重置索引编号
                        GTBNode node = null;
                        currentNodeIndex = -1;
                        secondBlockStart = -1;
                        decompressor = decompressors.get(managers[managerIndex].getCompressorIndex());
                        uncompressedBlock.seek = 0;

                        // 依次对每一个候选位点进行处理
                        for (MergePositionGroup candidateVariant : candidateVariants) {
                            for (MergePositionGroup.MultiAlleleGroup alleleGroups : candidateVariant.groups) {
                                VariantAbstract variant = uncompressedBlock.getCurrentVariant();
                                MergePositionGroup.AlleleGroup alleleGroup = alleleGroups.getAlleleGroup(managerIndex);

                                // 包含此位点时，进行复原
                                if (alleleGroup != null && (info = alleleGroup.getVariantCoordinate(managerIndex)) != null) {
                                    // 若当前的任务节点一致
                                    if (currentNodeIndex != info.nodeIndex) {
                                        unDecompressedCache.reset();
                                        decompressor.reset();
                                        genotypeCache.reset();
                                        node = managers[managerIndex].getGTBNodes(chromosomeIndex).get(info.nodeIndex);
                                        unDecompressedCache.makeSureCapacity(node.compressedGenotypesSize);

                                        fileStreams[managerIndex].seek(node.blockSeek);
                                        fileStreams[managerIndex].read(unDecompressedCache, node.compressedGenotypesSize);
                                        decompressor.decompress(unDecompressedCache, genotypeCache);
                                        currentNodeIndex = info.nodeIndex;
                                        secondBlockStart = eachLineSize[managerIndex][0] * node.subBlockVariantNum[0];
                                    }

                                    // 还原基因型数据
                                    byte[] transCode = alleleGroup.transCode;
                                    // 还原基因型数据
                                    if (transCode.length == 2) {
                                        // 还原基因型数据
                                        if (alleleGroup.trans) {
                                            for (IndexPair indexPair : indexPairs[managerIndex]) {
                                                byte code = BEGTransfer.groupDecode(groupDecoderIndexes[managerIndex], genotypeCache.cacheOf(eachLineSize[managerIndex][0] * info.variantIndex + indexPair.groupIndex) & 0xFF, indexPair.codeIndex);
                                                encodedCacheBase[variant.encodedStart + indexPair.seqIndex] = code == 0 ? 0 : this.encoder.encode(transCode[BEGDecoder.decodeHaplotype(0, code)], transCode[BEGDecoder.decodeHaplotype(1, code)]);
                                            }
                                        } else {
                                            for (IndexPair indexPair : indexPairs[managerIndex]) {
                                                encodedCacheBase[variant.encodedStart + indexPair.seqIndex] = BEGTransfer.groupDecode(groupDecoderIndexes[managerIndex], genotypeCache.cacheOf(eachLineSize[managerIndex][0] * info.variantIndex + indexPair.groupIndex) & 0xFF, indexPair.codeIndex);
                                            }
                                        }
                                    } else {
                                        // 基因型从 Pm  开始
                                        int srcStart = secondBlockStart + eachLineSize[managerIndex][1] * (info.variantIndex - node.subBlockVariantNum[0]);
                                        int dstStart = variant.encodedStart + indexOffsets[managerIndex];

                                        if (alleleGroup.trans) {
                                            for (int i = 0; i < indexLengths[managerIndex]; i++) {
                                                byte code = genotypeCache.cacheOf(srcStart + i);
                                                encodedCacheBase[dstStart + i] = code == 0 ? 0 : this.encoder.encode(transCode[BEGDecoder.decodeHaplotype(0, code)], transCode[BEGDecoder.decodeHaplotype(1, code)]);
                                            }
                                        } else {
                                            for (int i = 0; i < indexLengths[managerIndex]; i++) {
                                                encodedCacheBase[dstStart + i] = genotypeCache.cacheOf(srcStart + i);
                                            }
                                        }
                                    }
                                } else {
                                    // 不包含此位点，填充空基因型
                                    for (IndexPair indexPair : indexPairs[managerIndex]) {
                                        encodedCacheBase[variant.encodedStart + indexPair.seqIndex] = this.encoder.encodeMiss();
                                    }
                                }

                                // 编码完成，挪动指针
                                uncompressedBlock.seek++;
                            }
                        }
                    }

                    int ploidy = ChromosomeInfo.getPloidy(chromosomeIndex);

                    // 检验 alleleFreq
                    for (int i = 0; i < uncompressedBlock.seek; i++) {
                        int alleleCounts = 0;
                        int validAllelesNum = 0;
                        VariantAbstract variant = uncompressedBlock.variants[i];

                        for (int j = 0; j < validSubjectNum; j++) {
                            byte code = uncompressedBlock.encodedCache.cacheOf(variant.encodedStart + j);
                            if (code != 0) {
                                validAllelesNum += 1;
                                alleleCounts += 2 - this.encoder.scoreOf(code);
                            }
                        }

                        if (ploidy == 1) {
                            alleleCounts = alleleCounts >> 1;
                        } else {
                            // 二倍型
                            validAllelesNum = validAllelesNum << 1;
                        }

                        if (task.alleleQC.filter(alleleCounts, validAllelesNum)) {
                            // 被过滤掉的位点
                            variant.encoderIndex = -1;
                        }
                    }
                    Arrays.sort(uncompressedBlock.variants, 0, uncompressedBlock.seek, Comparator.comparing(o -> -o.encoderIndex));
                    for (int i = 0; i < uncompressedBlock.seek; i++) {
                        if (uncompressedBlock.variants[i].encoderIndex == -1) {
                            uncompressedBlock.seek = i;
                            break;
                        }
                    }

                    for (MergePositionGroup variant : candidateVariants) {
                        variant.freeMemory();
                    }
                    uncompressedBlock.chromosomeIndex = chromosomeIndex;
                    processGTBBlock(ctx, uncompressedBlock);
                    taskBlock = this.uncompressedPipLine.get();
                } while (taskBlock.getStatus());

                // 关闭上下文
                genotypeCache.close();
                ctx.close();
                caches.freeMemory();

                // 关闭文件
                for (int i = 0; i < this.managers.length; i++) {
                    // 文件操作流
                    fileStreams[i].close();
                }
            }
        } catch (IOException | InterruptedException e) {
            // e.printStackTrace();  // 调试使用
            throw new UnsupportedOperationException(e.getMessage());
        }
    }


    /**
     * 处理 基因组 block
     */
    void processGTBBlock(GTBCompressionContext ctx, UncompressedBlock uncompressedBlock) throws IOException {
        if (uncompressedBlock.seek == 0) {
            return;
        }

        // 向型转换
        if (this.phasedTransfers != null) {
            VariantAbstract variant;
            for (int phasedTransfer : this.phasedTransfers) {
                for (int i = this.indexOffsets[phasedTransfer]; i < this.indexOffsets[phasedTransfer] + this.indexLengths[phasedTransfer]; i++) {
                    for (int j = 0; j < uncompressedBlock.seek; j++) {
                        variant = uncompressedBlock.variants[j];

                        uncompressedBlock.encodedCache.cacheWrite(variant.encodedStart + i, BEGTransfer.toUnphased(uncompressedBlock.encodedCache.cacheOf(variant.encodedStart + i)));
                    }
                }
            }
        }

        // 绑定 inputBlock 数据
        Pair<GTBNode, VolumeByteStream> processedBlock = ctx.process(uncompressedBlock);

        // 获取估计大小
        int estimateSize = processedBlock.key.getEstimateDecompressedSize(validSubjectNum);

        // 写入数据和节点信息
        synchronized (this.GTBNodeCache) {
            this.outputFile.write(processedBlock.value);
            this.GTBNodeCache.add(processedBlock.key);

            if (estimateSize > maxEstimateSize) {
                maxEstimateSize = estimateSize;
            }
        }

        // 关闭上下文
        processedBlock.value.reset();
        uncompressedBlock.reset();
    }

    /**
     * 生成最终的 GTB 文件
     */
    void generateGTBFile() throws IOException {
        // 写入块头信息
        VolumeByteStream headerInfo = new VolumeByteStream(this.GTBNodeCache.size() * 25);
        for (GTBNode node : this.GTBNodeCache) {
            node.toTransFormat(headerInfo);
        }
        this.outputFile.write(headerInfo);

        // 修改文件标志信息
        GTBTree tree = new GTBTree(this.GTBNodeCache);
        this.baseInfoManager.setOrderedGTB(tree.isOrder());
        this.baseInfoManager.setEstimateDecompressedBlockSize(maxEstimateSize);
        this.outputFile.seek(0);
        this.outputFile.write(this.baseInfoManager.build());
        this.outputFile.write(ValueUtils.value2ByteArray(this.GTBNodeCache.size(), 3));
        this.outputFile.close();

        if (this.task.isInplace()) {
            this.outputFile.rename(this.task.getOutputFileName());
        }
    }
}