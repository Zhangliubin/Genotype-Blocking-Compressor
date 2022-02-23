package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.compressor.IDecompressor;
import edu.sysu.pmglab.suranyi.container.Pair;
import edu.sysu.pmglab.suranyi.container.ShareCache;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.coder.BEGTransfer;
import edu.sysu.pmglab.suranyi.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.common.block.VariantAbstract;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.*;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantQC;
import edu.sysu.pmglab.suranyi.threadPool.Block;
import edu.sysu.pmglab.suranyi.threadPool.DynamicPipeline;
import edu.sysu.pmglab.suranyi.threadPool.ThreadPool;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;

import java.io.IOException;
import java.util.*;

/**
 * @Data :2021/02/14
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :排序方法，由 RebuildTask 调用
 */

class RebuildKernel {
    /**
     * 一个 kernel 只能绑定一个任务
     */
    final RebuildTask task;

    /**
     * 编码器
     */
    final BEGEncoder encoder;

    /**
     * IO 数据管道
     */
    final DynamicPipeline<Boolean, Task> uncompressedPipLine;

    /**
     * 样本索引对
     */
    final IndexPair[] indexPairs;
    final int validSubjectNum;
    final int blockSize;

    /**
     * 根数据管理器
     */
    final FileBaseInfoManager baseInfoManager;
    final ArrayList<GTBNode> GTBNodeCache = new ArrayList<>(1024);
    int maxEstimateSize = 0;

    /**
     * 是否需要向型转换
     */
    final boolean phasedTransfer;
    final int maxOriginMBEGsSize;
    final int maxOriginAllelesSize;

    /**
     * 输出数据文件
     */
    final FileStream outputFile;
    final GTBManager manager;

    /**
     * VariantQC
     */
    final VariantQC variantQC;
    final AlleleQC alleleQC;

    /**
     * 线程池
     */
    final ThreadPool threadPool;

    boolean status;

    /**
     * 对外的提交方法，将任务提交至本类，进行压缩任务
     */
    static void rebuildAll(RebuildTask task) throws IOException {
        RebuildKernel kernel = new RebuildKernel(task);

        // 创建 Input 线程
        kernel.threadPool.submit(() -> {
            try {
                // 遍历染色体列表
                for (int chromosomeIndex : kernel.manager.getChromosomeList()) {
                    // 获取合并后的主根信息
                    RebuildVariant[] root = kernel.createRebuildTree(chromosomeIndex);
                    int resSize = root.length;
                    int offset = 0;
                    while (resSize > 0) {
                        int variantToProcess = Math.min(kernel.blockSize, resSize);
                        kernel.uncompressedPipLine.put(true, new Task(chromosomeIndex, ArrayUtils.copyOfRange(root, offset, offset + variantToProcess)));
                        resSize -= variantToProcess;
                        offset += variantToProcess;
                    }
                }

                kernel.uncompressedPipLine.putStatus(kernel.task.getThreads(), false);
            } catch (InterruptedException | IOException e) {
                // e.printStackTrace();
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        kernel.run();
    }

    /**
     * 对外的提交方法，将任务提交至本类，进行压缩任务
     */
    static void rebuildByChromosome(RebuildTask task, int... chromosomes) throws IOException {
        RebuildKernel kernel = new RebuildKernel(task);

        // 创建 Input 线程
        kernel.threadPool.submit(() -> {
            try {
                // 遍历染色体列表
                for (int chromosomeIndex : chromosomes) {
                    if (kernel.manager.contain(chromosomeIndex)) {
                        // 获取合并后的主根信息
                        RebuildVariant[] root = kernel.createRebuildTree(chromosomeIndex);
                        int resSize = root.length;
                        int offset = 0;
                        while (resSize > 0) {
                            int variantToProcess = Math.min(kernel.blockSize, resSize);
                            kernel.uncompressedPipLine.put(true, new Task(chromosomeIndex, ArrayUtils.copyOfRange(root, offset, offset + variantToProcess)));
                            resSize -= variantToProcess;
                            offset += variantToProcess;
                        }
                    }
                }

                kernel.uncompressedPipLine.putStatus(kernel.task.getThreads(), false);
            } catch (InterruptedException | IOException e) {
                // e.printStackTrace();
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        kernel.run();
    }

    /**
     * 对外的提交方法，将任务提交至本类，进行压缩任务
     */
    static void rebuildByRange(RebuildTask task, int chromosomeIndex, int startPos, int endPos) throws IOException {
        RebuildKernel kernel = new RebuildKernel(task);

        // 创建 Input 线程
        kernel.threadPool.submit(() -> {
            try {
                // 遍历染色体列表
                if (kernel.manager.contain(chromosomeIndex)) {
                    // 获取合并后的主根信息
                    RebuildVariant[] root = kernel.createRebuildTree(chromosomeIndex, startPos, endPos);
                    int resSize = root.length;
                    int offset = 0;
                    while (resSize > 0) {
                        int variantToProcess = Math.min(kernel.blockSize, resSize);
                        kernel.uncompressedPipLine.put(true, new Task(chromosomeIndex, ArrayUtils.copyOfRange(root, offset, offset + variantToProcess)));
                        resSize -= variantToProcess;
                        offset += variantToProcess;
                    }
                }

                kernel.uncompressedPipLine.putStatus(kernel.task.getThreads(), false);
            } catch (InterruptedException | IOException e) {
                // e.printStackTrace();
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        kernel.run();
    }

    /**
     * 对外的提交方法，将任务提交至本类，进行压缩任务
     */
    static <ChromosomeType> void rebuildByPosition(RebuildTask task, Map<ChromosomeType, int[]> chromosomeNodeIndexesUnknownType) throws IOException {
        RebuildKernel kernel = new RebuildKernel(task);
        HashMap<Integer, int[]> chromosomePositions = ChromosomeInfo.identifyChromosome(chromosomeNodeIndexesUnknownType);

        // 创建 Input 线程
        kernel.threadPool.submit(() -> {
            try {
                // 遍历染色体列表
                for (int chromosomeIndex: chromosomePositions.keySet()) {
                    if (kernel.manager.contain(chromosomeIndex)) {
                        // 获取合并后的主根信息
                        RebuildVariant[] root = kernel.createRebuildTree(chromosomeIndex, chromosomePositions.get(chromosomeIndex));
                        int resSize = root.length;
                        int offset = 0;
                        while (resSize > 0) {
                            int variantToProcess = Math.min(kernel.blockSize, resSize);
                            kernel.uncompressedPipLine.put(true, new Task(chromosomeIndex, ArrayUtils.copyOfRange(root, offset, offset + variantToProcess)));
                            resSize -= variantToProcess;
                            offset += variantToProcess;
                        }
                    }
                }

                kernel.uncompressedPipLine.putStatus(kernel.task.getThreads(), false);
            } catch (InterruptedException | IOException e) {
                // e.printStackTrace();
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        kernel.run();
    }

    IndexPair[] initIndexPairs(GTBManager manager, SmartList<String> selectedSubjects) {
        // 初始化索引对
        IndexPair[] pairs;

        // 组合编码情况下，判断其组合地址
        int eachGroupNum = manager.isPhased() ? 3 : 4;

        if (selectedSubjects == null) {
            // 没有选定样本时，索引对长度等于全样本长度
            pairs = new IndexPair[manager.getSubjectNum()];

            for (int i = 0; i < pairs.length; i++) {
                pairs[i] = new IndexPair(i, i, i / eachGroupNum, i % eachGroupNum);
            }
        } else {
            // 根据 selectSubjects 还原全局索引
            int seek = 0;
            int[] indexes = manager.getSubjectIndex(selectedSubjects);

            // 选定了样本时，索引对长度等于选定的样本个数
            pairs = new IndexPair[selectedSubjects.size()];
            for (int i = 0; i < indexes.length; i++) {
                pairs[seek++] = new IndexPair(i, indexes[i], indexes[i] / eachGroupNum, indexes[i] % eachGroupNum);
            }
        }

        return pairs;
    }

    /**
     * 标准构造器，传入 EditTask，根据该提交任务执行工作
     *
     * @param task 待执行任务
     */
    RebuildKernel(RebuildTask task) throws IOException {
        // 加载 MBEG 编码表
        this.encoder = BEGEncoder.getEncoder(task.isPhased());

        // 设置任务
        this.task = task;
        this.baseInfoManager = FileBaseInfoManager.of(task);
        this.variantQC = task.getVariantQC();
        this.alleleQC = task.getAlleleQC();

        // 设定管理器
        this.manager = task.getInputFile();

        // 获取解压数据预估大小的相关信息
        this.maxOriginMBEGsSize = this.manager.getMaxDecompressedMBEGsSize();
        this.maxOriginAllelesSize = this.manager.getMaxDecompressedAllelesSize();

        // 设定任务索引对及样本偏移量
        this.indexPairs = initIndexPairs(this.manager, task.getSubjects());

        // 样本个数
        this.validSubjectNum = task.getSubjects() == null ? manager.getSubjectNum() : task.getSubjects().size();

        // 验证块大小参数
        int blockSizeType = BlockSizeParameter.getSuggestBlockSizeType(task.getBlockSizeType(), validSubjectNum);
        this.blockSize = BlockSizeParameter.getBlockSize(blockSizeType);
        this.baseInfoManager.setBlockSizeType(blockSizeType);

        // 设置输出数据文件
        this.outputFile = new FileStream(task.isInplace() ? this.task.getOutputFileName() + ".~$temp" : this.task.getOutputFileName(), FileOptions.CHANNEL_WRITER);

        // 写入块大小占位符 (值为 0)
        this.outputFile.write(ValueUtils.value2ByteArray(0, 5));

        // 写入 refer 网址
        if (this.manager.getReferenceSize() != 0) {
            this.outputFile.write(this.manager.getReference());
        }

        // 写入换行符
        this.outputFile.write(ByteCode.NEWLINE);

        // 压缩并写入样本名
        if (this.task.getSubjects() == null) {
            // 写入样本名
            VolumeByteStream subjectSeq = ICompressor.compress(task.getCompressor(), task.getCompressionLevel(), this.manager.getSubjects(), 0, this.manager.getSubjects().length);
            this.outputFile.writeIntegerValue(subjectSeq.size());
            this.outputFile.write(subjectSeq);
        } else {
            // 写入样本名
            byte[] subjects = String.join("\t", this.task.getSubjects()).getBytes();
            VolumeByteStream subjectSeq = ICompressor.compress(task.getCompressor(), task.getCompressionLevel(), subjects, 0, subjects.length);
            this.outputFile.writeIntegerValue(subjectSeq.size());
            this.outputFile.write(subjectSeq);
        }

        // 创建数据管道
        this.uncompressedPipLine = new DynamicPipeline<>(task.getThreads() << 2);

        // 创建线程池
        this.threadPool = new ThreadPool(this.task.getThreads() + 1);

        // 确认是否需要进行向型转换
        this.phasedTransfer = !task.isPhased() && this.manager.isPhased();
    }

    void run() throws IOException {
        int[] eachLineSize = new int[2];
        int groupNum = manager.isPhased() ? 3 : 4;

        int resBlockCodeGenotypeNum = manager.getSubjectNum() % groupNum;
        eachLineSize[0] = (manager.getSubjectNum() / groupNum) + (resBlockCodeGenotypeNum == 0 ? 0 : 1);
        eachLineSize[1] = manager.getSubjectNum();
        int groupDecoderIndexes = manager.isPhased() ? 1 : 0;
        threadPool.submit(() -> {
            if (task.getAlleleQC().size() > 0) {
                processGenomeBlockWithFilter(indexPairs, eachLineSize, groupDecoderIndexes);
            } else {
                processGenomeBlock(indexPairs, eachLineSize, groupDecoderIndexes);
            }
        }, this.task.getThreads());

        // 关闭线程池，等待任务完成
        threadPool.close();

        // 生成最后的 gtb 文件
        generateGTBFile();

        // 标记为完成任务
        this.status = true;

        // 关闭数据管道
        release();
    }

    /**
     * 创建合并树
     */
    RebuildVariant[] createRebuildTree(int chromosomeIndex) throws IOException {
        RebuildVariant[] root = new RebuildVariant[manager.getGtbTree().numOfVariants(chromosomeIndex)];
        VolumeByteStream undecompressedCache = new VolumeByteStream();
        VolumeByteStream decompressedPosCache = new VolumeByteStream(blockSize << 2);
        VolumeByteStream decompressedAllelesCache = new VolumeByteStream(maxOriginAllelesSize);
        GTBNodes nodes;

        nodes = manager.getGTBNodes(chromosomeIndex);
        int variantIndex = 0;

        // 位置解压器
        FileStream fileStream = manager.getFileStream();
        IDecompressor decompressor = IDecompressor.getInstance(manager.getCompressorIndex());

        for (int i = 0; i < nodes.numOfNodes(); i++) {
            GTBNode node = nodes.get(i);
            decompressedAllelesCache.reset();
            decompressedPosCache.reset();
            undecompressedCache.reset();
            undecompressedCache.makeSureCapacity(node.compressedAlleleSize, node.compressedPosSize);

            // 读取压缩后的位置数据
            fileStream.seek(node.blockSeek + node.compressedGenotypesSize);
            fileStream.read(undecompressedCache, node.compressedPosSize);
            decompressor.decompress(undecompressedCache, decompressedPosCache);
            undecompressedCache.reset();

            // 读取压缩后的位置数据
            fileStream.read(undecompressedCache, node.compressedAlleleSize);
            decompressor.decompress(undecompressedCache, decompressedAllelesCache);

            int startPos;
            int endPos = -1;

            // 还原位置数据，并构建红黑树表
            for (int j = 0; j < node.numOfVariants(); j++) {
                // 设置等位基因数据
                startPos = endPos;
                endPos = decompressedAllelesCache.indexOfN(ByteCode.SLASH, startPos + 1, 1);

                // 还原位置值
                int position = ValueUtils.byteArray2IntegerValue(decompressedPosCache.cacheOf(j << 2),
                        decompressedPosCache.cacheOf(1 + (j << 2)),
                        decompressedPosCache.cacheOf(2 + (j << 2)),
                        decompressedPosCache.cacheOf(3 + (j << 2)));

                // 执行等位基因过滤
                byte[] alleles = decompressedAllelesCache.cacheOf(startPos + 1, endPos);

                // 需要进行位点过滤
                int allelesNum = 2;
                for (int k = 3; k < alleles.length; k++) {
                    if (alleles[k] == ByteCode.COMMA) {
                        allelesNum += 1;
                    }
                }

                if (!this.variantQC.filter(null, allelesNum, 0, 0, 0, 0)) {
                    root[variantIndex++] = new RebuildVariant(position, allelesNum, alleles, i, j);
                }
            }
        }
        fileStream.close();

        undecompressedCache.close();
        decompressedPosCache.close();
        decompressedAllelesCache.close();

        // root 构建完成后，先确保数据非空，再排序
        if (variantIndex < root.length) {
            root = ArrayUtils.copyOfRange(root, 0, variantIndex);
        }
        ArrayUtils.sort(root, Comparator.comparingInt(o -> o.position));

        return root;
    }


    /**
     * 创建合并树
     */
    RebuildVariant[] createRebuildTree(int chromosomeIndex, int[] positions) throws IOException {
        RebuildVariant[] root = new RebuildVariant[manager.getGtbTree().numOfVariants(chromosomeIndex)];
        VolumeByteStream undecompressedCache = new VolumeByteStream();
        VolumeByteStream decompressedPosCache = new VolumeByteStream(blockSize << 2);
        VolumeByteStream decompressedAllelesCache = new VolumeByteStream(maxOriginAllelesSize);
        HashSet<Integer> validPositions = ArrayUtils.toSet(positions);
        GTBNodes nodes;

        nodes = manager.getGTBNodes(chromosomeIndex);
        int variantIndex = 0;

        // 位置解压器
        FileStream fileStream = manager.getFileStream();
        IDecompressor decompressor = IDecompressor.getInstance(manager.getCompressorIndex());

        for (int i = 0; i < nodes.numOfNodes(); i++) {
            GTBNode node = nodes.get(i);
            decompressedAllelesCache.reset();
            decompressedPosCache.reset();
            undecompressedCache.reset();
            undecompressedCache.makeSureCapacity(node.compressedAlleleSize, node.compressedPosSize);

            // 读取压缩后的位置数据
            fileStream.seek(node.blockSeek + node.compressedGenotypesSize);
            fileStream.read(undecompressedCache, node.compressedPosSize);
            decompressor.decompress(undecompressedCache, decompressedPosCache);
            undecompressedCache.reset();

            // 读取压缩后的位置数据
            fileStream.read(undecompressedCache, node.compressedAlleleSize);
            decompressor.decompress(undecompressedCache, decompressedAllelesCache);

            int startPos;
            int endPos = -1;

            // 还原位置数据，并构建红黑树表
            for (int j = 0; j < node.numOfVariants(); j++) {
                // 设置等位基因数据
                startPos = endPos;
                endPos = decompressedAllelesCache.indexOfN(ByteCode.SLASH, startPos + 1, 1);

                // 还原位置值
                int position = ValueUtils.byteArray2IntegerValue(decompressedPosCache.cacheOf(j << 2),
                        decompressedPosCache.cacheOf(1 + (j << 2)),
                        decompressedPosCache.cacheOf(2 + (j << 2)),
                        decompressedPosCache.cacheOf(3 + (j << 2)));

                if (!validPositions.contains(position)) {
                    continue;
                }

                // 执行等位基因过滤
                byte[] alleles = decompressedAllelesCache.cacheOf(startPos + 1, endPos);

                // 需要进行位点过滤
                int allelesNum = 2;
                for (int k = 3; k < alleles.length; k++) {
                    if (alleles[k] == ByteCode.COMMA) {
                        allelesNum += 1;
                    }
                }

                if (!this.variantQC.filter(null, allelesNum, 0, 0, 0, 0)) {
                    root[variantIndex++] = new RebuildVariant(position, allelesNum, alleles, i, j);
                }
            }
        }
        fileStream.close();

        undecompressedCache.close();
        decompressedPosCache.close();
        decompressedAllelesCache.close();

        // root 构建完成后，先确保数据非空，再排序
        if (variantIndex < root.length) {
            root = ArrayUtils.copyOfRange(root, 0, variantIndex);
        }
        ArrayUtils.sort(root, Comparator.comparingInt(o -> o.position));

        return root;
    }

    /**
     * 创建合并树
     */
    RebuildVariant[] createRebuildTree(int chromosomeIndex, int minPos, int maxPos) throws IOException {
        GTBNodes nodes;
        nodes = manager.getGTBNodes(chromosomeIndex);

        if (!nodes.intersectPos(minPos, maxPos)) {
            return new RebuildVariant[0];
        }

        RebuildVariant[] root = new RebuildVariant[manager.getGtbTree().numOfVariants(chromosomeIndex)];
        VolumeByteStream undecompressedCache = new VolumeByteStream();
        VolumeByteStream decompressedPosCache = new VolumeByteStream(blockSize << 2);
        VolumeByteStream decompressedAllelesCache = new VolumeByteStream(maxOriginAllelesSize);
        int variantIndex = 0;

        // 位置解压器
        FileStream fileStream = manager.getFileStream();
        IDecompressor decompressor = IDecompressor.getInstance(manager.getCompressorIndex());

        for (int i = 0; i < nodes.numOfNodes(); i++) {
            GTBNode node = nodes.get(i);
            decompressedAllelesCache.reset();
            decompressedPosCache.reset();
            undecompressedCache.reset();
            undecompressedCache.makeSureCapacity(node.compressedAlleleSize, node.compressedPosSize);

            // 读取压缩后的位置数据
            fileStream.seek(node.blockSeek + node.compressedGenotypesSize);
            fileStream.read(undecompressedCache, node.compressedPosSize);
            decompressor.decompress(undecompressedCache, decompressedPosCache);
            undecompressedCache.reset();

            // 读取压缩后的位置数据
            fileStream.read(undecompressedCache, node.compressedAlleleSize);
            decompressor.decompress(undecompressedCache, decompressedAllelesCache);

            int startPos;
            int endPos = -1;

            // 还原位置数据，并构建红黑树表
            for (int j = 0; j < node.numOfVariants(); j++) {
                // 设置等位基因数据
                startPos = endPos;
                endPos = decompressedAllelesCache.indexOfN(ByteCode.SLASH, startPos + 1, 1);

                // 还原位置值
                int position = ValueUtils.byteArray2IntegerValue(decompressedPosCache.cacheOf(j << 2),
                        decompressedPosCache.cacheOf(1 + (j << 2)),
                        decompressedPosCache.cacheOf(2 + (j << 2)),
                        decompressedPosCache.cacheOf(3 + (j << 2)));

                if (position < minPos || position > maxPos) {
                    continue;
                }

                // 执行等位基因过滤
                byte[] alleles = decompressedAllelesCache.cacheOf(startPos + 1, endPos);

                // 需要进行位点过滤
                int allelesNum = 2;
                for (int k = 3; k < alleles.length; k++) {
                    if (alleles[k] == ByteCode.COMMA) {
                        allelesNum += 1;
                    }
                }

                if (!this.variantQC.filter(null, allelesNum, 0, 0, 0, 0)) {
                    root[variantIndex++] = new RebuildVariant(position, allelesNum, alleles, i, j);
                }
            }
        }
        fileStream.close();

        undecompressedCache.close();
        decompressedPosCache.close();
        decompressedAllelesCache.close();

        // root 构建完成后，先确保数据非空，再排序
        if (variantIndex < root.length) {
            root = ArrayUtils.copyOfRange(root, 0, variantIndex);
        }
        ArrayUtils.sort(root, Comparator.comparingInt(o -> o.position));

        return root;
    }

    static class Task {
        int chromosomeIndex;
        RebuildVariant[] candidateNodes;

        public Task(int chromosomeIndex, RebuildVariant[] candidateNodes) {
            this.chromosomeIndex = chromosomeIndex;
            this.candidateNodes = candidateNodes;
        }
    }

    /**
     * 解除修改锁定，解除后无法保证数据安全，直接清除本类数据
     */
    void release() {

        // 如果是错误结束的，则需要删除文件
        if (!this.status) {
            try {
                this.outputFile.delete();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 处理 基因组 block
     */
    void processGenomeBlock(IndexPair[] indexPairs, int[] eachLineSize, int groupDecoderIndexes) {
        try {
            Block<Boolean, Task> taskBlock = this.uncompressedPipLine.get();

            // 确认为需要处理的任务块，只有需要这么一些线程的时候，才会创建容器
            if (taskBlock.getStatus()) {
                // 块解压器
                IDecompressor decompressor = IDecompressor.getInstance(manager.getCompressorIndex());

                // 未解压数据缓冲区
                VolumeByteStream unDecompressedCache = new VolumeByteStream((blockSize * Math.max(20, validSubjectNum)) >> 1);

                // 解压数据缓冲区
                VolumeByteStream genotypeCache = new VolumeByteStream(this.maxOriginMBEGsSize);

                // 创建本地编码缓冲区
                VolumeByteStream encodedCache = new VolumeByteStream(this.validSubjectNum * this.blockSize);

                // 未压缩块
                UncompressedBlock uncompressedBlock = new UncompressedBlock(this.validSubjectNum, this.task, this.blockSize, encodedCache);

                // 压缩上下文
                ShareCache shareCaches = new ShareCache(encodedCache, unDecompressedCache);
                GTBCompressionContext ctx = new GTBCompressionContext(this.task, this.validSubjectNum, shareCaches);

                // 获取文件流
                FileStream fileStream = this.manager.getFileStream();

                do {
                    // 提取任务
                    RebuildVariant[] candidateVariants = taskBlock.getData().candidateNodes;

                    // 对任务排序，按照 nodeIndex 排序
                    Arrays.sort(candidateVariants, Comparator.comparingInt(o -> o.nodeIndex));
                    int chromosomeIndex = taskBlock.getData().chromosomeIndex;

                    // 当前解压的节点索引，如果索引一致，则不会进行解压
                    int currentNodeIndex = -1;

                    GTBNode node = null;
                    int secondBlockStart;
                    secondBlockStart = -1;
                    uncompressedBlock.seek = 0;

                    // 依次对每一个候选位点进行处理
                    for (RebuildVariant candidateVariant : candidateVariants) {
                        VariantAbstract variant = uncompressedBlock.getCurrentVariant();

                        // 若当前的任务节点一致
                        if (currentNodeIndex != candidateVariant.nodeIndex) {
                            unDecompressedCache.reset();
                            decompressor.reset();
                            genotypeCache.reset();
                            node = manager.getGTBNodes(chromosomeIndex).get(candidateVariant.nodeIndex);
                            unDecompressedCache.makeSureCapacity(node.compressedGenotypesSize);

                            fileStream.seek(node.blockSeek);
                            fileStream.read(unDecompressedCache, node.compressedGenotypesSize);
                            decompressor.decompress(unDecompressedCache, genotypeCache);
                            currentNodeIndex = candidateVariant.nodeIndex;
                            secondBlockStart = eachLineSize[0] * node.subBlockVariantNum[0];
                        }

                        if (variant.allele.size() == 0) {
                            variant.position = candidateVariant.position;
                            variant.setAllele(candidateVariant.allelesInfo, 0, candidateVariant.allelesInfo.length);
                            variant.encoderIndex = candidateVariant.allelesNum == 2 ? 0 : 1;
                        }

                        // 还原基因型数据
                        if (variant.encoderIndex == 0) {
                            // 还原基因型数据
                            for (IndexPair indexPair : indexPairs) {
                                uncompressedBlock.encodedCache.cacheWrite(variant.encodedStart + indexPair.seqIndex, BEGTransfer.groupDecode(groupDecoderIndexes, genotypeCache.cacheOf(eachLineSize[0] * candidateVariant.variantIndex + indexPair.groupIndex) & 0xFF, indexPair.codeIndex));
                            }
                        } else {
                            // 还原基因型数据
                            for (IndexPair indexPair : indexPairs) {
                                uncompressedBlock.encodedCache.cacheWrite(variant.encodedStart + indexPair.seqIndex, genotypeCache.cacheOf(secondBlockStart + eachLineSize[1] * (candidateVariant.variantIndex - node.subBlockVariantNum[0]) + indexPair.index));
                            }
                        }

                        // 编码完成，挪动指针
                        uncompressedBlock.seek++;
                    }

                    uncompressedBlock.chromosomeIndex = chromosomeIndex;
                    processGTBBlock(ctx, uncompressedBlock);
                    taskBlock = this.uncompressedPipLine.get();
                } while (taskBlock.getStatus());

                // 关闭上下文
                decompressor.close();
                genotypeCache.close();
                ctx.close();
                fileStream.close();
                shareCaches.freeMemory();
            }
        } catch (InterruptedException | IOException e) {
            // e.printStackTrace();  // 调试使用
            throw new UnsupportedOperationException(e.getMessage());
        }
    }

    private void processGenomeBlockWithFilter(IndexPair[] indexPairs, int[] eachLineSize, int groupDecoderIndexes) {
        try {
            Block<Boolean, Task> taskBlock = this.uncompressedPipLine.get();

            // 确认为需要处理的任务块，只有需要这么一些线程的时候，才会创建容器
            if (taskBlock.getStatus()) {
                // 块解压器
                IDecompressor decompressor = IDecompressor.getInstance(manager.getCompressorIndex());

                // 未解压数据缓冲区
                VolumeByteStream unDecompressedCache = new VolumeByteStream((blockSize * Math.max(20, validSubjectNum)) >> 1);

                // 解压数据缓冲区
                VolumeByteStream genotypeCache = new VolumeByteStream(this.maxOriginMBEGsSize);

                // 创建本地编码缓冲区
                VolumeByteStream encodedCache = new VolumeByteStream(this.validSubjectNum * this.blockSize);

                // 未压缩块
                UncompressedBlock uncompressedBlock = new UncompressedBlock(this.validSubjectNum, this.task, this.blockSize, encodedCache);

                // 压缩上下文
                ShareCache caches = new ShareCache(encodedCache, unDecompressedCache);
                GTBCompressionContext ctx = new GTBCompressionContext(this.task, this.validSubjectNum, caches);

                // 获取文件流
                FileStream fileStream = this.manager.getFileStream();

                do {
                    // 提取任务
                    RebuildVariant[] candidateVariants = taskBlock.getData().candidateNodes;
                    Arrays.sort(candidateVariants, Comparator.comparingInt(o -> o.nodeIndex));
                    int chromosomeIndex = taskBlock.getData().chromosomeIndex;

                    // 当前解压的节点索引，如果索引一致，则不会进行解压
                    int currentNodeIndex = -1;

                    GTBNode node = null;
                    byte code;
                    int secondBlockStart = -1;
                    uncompressedBlock.seek = 0;
                    int ploidy = ChromosomeInfo.getPloidy(chromosomeIndex);

                    // 依次对每一个候选位点进行处理
                    for (RebuildVariant candidateVariant : candidateVariants) {
                        VariantAbstract variant = uncompressedBlock.getCurrentVariant();

                        // 若当前的任务节点一致
                        if (currentNodeIndex != candidateVariant.nodeIndex) {
                            unDecompressedCache.reset();
                            decompressor.reset();
                            genotypeCache.reset();
                            node = manager.getGTBNodes(chromosomeIndex).get(candidateVariant.nodeIndex);
                            unDecompressedCache.makeSureCapacity(node.compressedGenotypesSize);

                            fileStream.seek(node.blockSeek);
                            fileStream.read(unDecompressedCache, node.compressedGenotypesSize);
                            decompressor.decompress(unDecompressedCache, genotypeCache);
                            currentNodeIndex = candidateVariant.nodeIndex;
                            secondBlockStart = eachLineSize[0] * node.subBlockVariantNum[0];

                        }

                        if (variant.allele.size() == 0) {
                            variant.position = candidateVariant.position;
                            variant.setAllele(candidateVariant.allelesInfo, 0, candidateVariant.allelesInfo.length);
                            variant.encoderIndex = candidateVariant.allelesNum == 2 ? 0 : 1;
                        }

                        int alleleCounts = 0;
                        int validAllelesNum = 0;

                        // 还原基因型数据
                        if (variant.encoderIndex == 0) {
                            // 还原基因型数据
                            for (IndexPair indexPair : indexPairs) {
                                code = BEGTransfer.groupDecode(groupDecoderIndexes, genotypeCache.cacheOf(eachLineSize[0] * candidateVariant.variantIndex + indexPair.groupIndex) & 0xFF, indexPair.codeIndex);
                                uncompressedBlock.encodedCache.cacheWrite(variant.encodedStart + indexPair.seqIndex, code);
                                if (code != 0) {
                                    validAllelesNum += 1;
                                    alleleCounts += 2 - this.encoder.scoreOf(code);
                                }
                            }
                        } else {
                            // 还原基因型数据
                            for (IndexPair indexPair : indexPairs) {
                                code = genotypeCache.cacheOf(secondBlockStart + eachLineSize[1] * (candidateVariant.variantIndex - node.subBlockVariantNum[0]) + indexPair.index);
                                uncompressedBlock.encodedCache.cacheWrite(variant.encodedStart + indexPair.seqIndex, code);
                                if (code != 0) {
                                    validAllelesNum += 1;
                                    alleleCounts += 2 - this.encoder.scoreOf(code);
                                }
                            }
                        }

                        if (ploidy == 1) {
                            alleleCounts = alleleCounts >> 1;
                        } else {
                            // 二倍型
                            validAllelesNum = validAllelesNum << 1;
                        }

                        if (!task.alleleQC.filter(alleleCounts, validAllelesNum)) {
                            // 编码完成，挪动指针
                            uncompressedBlock.seek++;
                        } else {
                            variant.allele.reset();
                        }
                    }

                    uncompressedBlock.chromosomeIndex = chromosomeIndex;
                    processGTBBlock(ctx, uncompressedBlock);
                    taskBlock = this.uncompressedPipLine.get();
                } while (taskBlock.getStatus());

                // 关闭上下文
                decompressor.close();
                genotypeCache.close();
                ctx.close();
                fileStream.close();
                caches.freeMemory();
            }
        } catch (InterruptedException | IOException e) {
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

        // 执行向型转换
        if (this.phasedTransfer) {
            for (int i = 0; i < uncompressedBlock.seek; i++) {
                for (int j = 0; j < uncompressedBlock.validSubjectNum; j++) {
                    uncompressedBlock.encodedCache.cacheWrite(uncompressedBlock.variants[i].encodedStart + j, BEGTransfer.toUnphased(uncompressedBlock.encodedCache.cacheOf(uncompressedBlock.variants[i].encodedStart + j)));
                }
            }
        }

        // 绑定 inputBlock 数据
        Pair<GTBNode, VolumeByteStream> processedBlock = ctx.process(uncompressedBlock);

        int estimateSize = processedBlock.key.getEstimateDecompressedSize(validSubjectNum);

        // 写入数据和节点信息
        synchronized (this.GTBNodeCache) {
            this.outputFile.write(processedBlock.value);
            this.GTBNodeCache.add(processedBlock.key);

            if (estimateSize > maxEstimateSize) {
                maxEstimateSize = estimateSize;
            }
        }

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