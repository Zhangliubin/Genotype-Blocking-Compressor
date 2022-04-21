package edu.sysu.pmglab.gbc.core.calculation.ld;

import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNodes;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Pointer;
import edu.sysu.pmglab.check.Value;
import edu.sysu.pmglab.container.Pair;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.GTBReader;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.threadPool.Block;
import edu.sysu.pmglab.threadPool.DynamicPipeline;
import edu.sysu.pmglab.threadPool.ThreadPool;
import edu.sysu.pmglab.unifyIO.clm.MultiThreadsWriter;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author suranyi
 * @description LD 核心方法，仅允许由 LDTask 发起调用
 */

class LDKernel {
    final LDTask task;
    final GTBManager manager;
    final ILDContext ldModel;
    final int validSampleNum;
    final int maxCacheBlockSize;
    int totalThreadsNum;
    MultiThreadsWriter outputFile;

    /**
     * IO 数据管道
     */
    DynamicPipeline<Boolean, TaskNode> inputPipeline;

    /**
     * 单个线程处理的块个数
     */
    static final int MAX_THREAD_PROCESS_BLOCK_NUM = 10;

    LDKernel(LDTask task) {
        this.task = task;
        this.manager = this.task.getManager();
        if (this.task.getLdModel() instanceof GenotypeLD) {
            this.ldModel = GenotypeLD.GENOTYPE_LD;
        } else if (this.task.getLdModel() instanceof HaplotypeLD) {
            this.ldModel = HaplotypeLD.HAPLOTYPE_LD;
        } else {
            throw new UnsupportedOperationException(this.task.getLdModel().toString());
        }

        // 有效样本个数, 位点个数
        this.validSampleNum = this.task.getSubjects() == null ? this.manager.getSubjectNum() : this.task.getSubjects().size();

        // 至多同时缓冲的位点个数
        this.maxCacheBlockSize = Value.of(task.getWindowSizeBp() / 10,
                Math.min(((Integer.MAX_VALUE >> 1) - 2) / manager.getSubjectNum(), manager.getBlockSize() << 1),
                Math.min(((Integer.MAX_VALUE >> 1) - 2) / manager.getSubjectNum(), manager.getBlockSize() * MAX_THREAD_PROCESS_BLOCK_NUM));
    }

    /**
     * 指定染色体 LD 计算
     * @param task LD 任务
     * @param chromosomeIndexes 计算的染色体编号
     */
    static void calculate(LDTask task, final int... chromosomeIndexes) throws IOException {
        LDKernel kernel = new LDKernel(task);

        // 对染色体数据排序
        Arrays.sort(chromosomeIndexes);

        // 总块数
        int totalBlocksNum = 0;
        for (int chromosomeIndex : chromosomeIndexes) {
            GTBNodes nodes = kernel.manager.getGTBNodes(chromosomeIndex);
            if (nodes != null) {
                totalBlocksNum += nodes.numOfNodes();
            }
        }

        kernel.totalThreadsNum = Value.of(task.getThreads(), 1, Math.max(1, totalBlocksNum));
        kernel.outputFile = new MultiThreadsWriter(task.outputFileName, task.getOutputParam(), kernel.totalThreadsNum);
        kernel.outputFile.write(kernel.ldModel.getHeader().getBytes());

        if (totalBlocksNum > 0) {
            // 开启线程池
            ThreadPool threadPool = new ThreadPool(kernel.totalThreadsNum + 1);

            // 数据管道
            kernel.inputPipeline = new DynamicPipeline<>(kernel.totalThreadsNum << 1);

            // 提交多线程计算任务，关闭线程池
            threadPool.submit(() -> {
                try {
                    kernel.calculateLD();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, kernel.totalThreadsNum);

            // 分发任务线程
            threadPool.submit(() -> {
                try {
                    if (kernel.totalThreadsNum == 1) {
                        // 单线程运行，不分块
                        for (int chromosome : chromosomeIndexes) {
                            GTBNodes nodes = kernel.manager.getGTBNodes(chromosome);
                            if (nodes != null) {
                                int totalBlockNum = nodes.numOfNodes();
                                if (totalBlockNum >= 1) {
                                    kernel.inputPipeline.put(true, TaskNode.of(chromosome, nodes.get(0).minPos, nodes.get(-1).maxPos));
                                }
                            }
                        }
                    } else {
                        // 多线程运行，分块
                        for (int chromosome : chromosomeIndexes) {
                            GTBNodes nodes = kernel.manager.getGTBNodes(chromosome);
                            if (nodes != null) {
                                int totalBlockNum = nodes.numOfNodes();

                                if (totalBlockNum >= 1) {
                                    // 多线程运行, 每个线程至多处理 MAX_THREAD_PROCESS_BLOCK_SIZE 个 block
                                    int eachThreadProcessNum = Math.max(Math.min(2, totalBlockNum), Math.min((int) Math.ceil((float) totalBlockNum / kernel.totalThreadsNum), MAX_THREAD_PROCESS_BLOCK_NUM));
                                    int submittedNum = 0;
                                    while (submittedNum < totalBlockNum) {
                                        int blockToProcess = Math.min(totalBlockNum, submittedNum + eachThreadProcessNum);

                                        kernel.inputPipeline.put(true, TaskNode.of(chromosome,
                                                nodes.get(submittedNum).minPos,
                                                nodes.get(blockToProcess - 1).maxPos,
                                                Math.min(nodes.get(blockToProcess - 1).maxPos + task.getWindowSizeBp(), nodes.get(-1).maxPos)));
                                        submittedNum = blockToProcess;
                                    }
                                }
                            }
                        }
                    }

                    kernel.inputPipeline.putStatus(kernel.totalThreadsNum, false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            threadPool.close();
        }

        // 资源收尾
        kernel.outputFile.close();
    }

    /**
     * 指定染色体区域，计算 LD 系数
     * @param task LD 任务
     * @param chromosomeIndex 染色体编号
     * @param startPos 起点
     * @param endPos 终点
     */
    static void calculate(LDTask task, int chromosomeIndex, int startPos, int endPos) throws IOException {
        // 该情况比较复杂，以单线程运行
        LDKernel kernel = new LDKernel(task);

        // 提取出节点
        GTBNodes nodes = kernel.manager.getGTBNodes(chromosomeIndex);

        if (nodes == null || nodes.numOfNodes() == 0 || startPos > endPos || !nodes.intersectPos(startPos, endPos)) {
            kernel.outputFile = new MultiThreadsWriter(task.outputFileName, task.getOutputParam(), 1);
            kernel.outputFile.write(kernel.ldModel.getHeader().getBytes());
        } else {
            // 校正位置
            final int finalStartPos = Math.max(startPos, nodes.get(0).minPos);
            final int finalEndPos = Math.min(endPos, nodes.get(-1).maxPos);

            // 起始块索引
            int startBlockIndex = nodes.find(finalStartPos);

            // 终点块索引
            int endBlockIndex = nodes.find(finalEndPos);

            // 校正块个数
            int totalBlockNum = endBlockIndex - startBlockIndex + 1;

            // 计算估计的总线程数
            kernel.totalThreadsNum = Value.of(task.getThreads(), 1, (int) Math.ceil((float) (totalBlockNum) / MAX_THREAD_PROCESS_BLOCK_NUM));

            kernel.outputFile = new MultiThreadsWriter(task.outputFileName, task.getOutputParam(), kernel.totalThreadsNum);
            kernel.outputFile.write(kernel.ldModel.getHeader().getBytes());

            // 开启线程池
            ThreadPool threadPool = new ThreadPool(kernel.totalThreadsNum + 1);
            // 数据管道
            kernel.inputPipeline = new DynamicPipeline<>(kernel.totalThreadsNum << 1);
            // 提交计算任务
            threadPool.submit(() -> {
                try {
                    kernel.calculateLD();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, kernel.totalThreadsNum);

            // 提交线程任务
            threadPool.submit(() -> {
                try {
                    if (kernel.totalThreadsNum == 1) {
                        // 单线程运行
                        kernel.inputPipeline.put(true, TaskNode.of(chromosomeIndex, finalStartPos, finalEndPos));
                    } else {
                        // 多线程运行, 每个线程至多处理 MAX_THREAD_PROCESS_BLOCK_SIZE 个block
                        int eachThreadProcessNum = Math.min((int) Math.ceil((float) totalBlockNum / kernel.totalThreadsNum), MAX_THREAD_PROCESS_BLOCK_NUM);
                        int submittedNum = 0;
                        while (submittedNum < totalBlockNum) {
                            int blockToProcess = Math.min(totalBlockNum, submittedNum + eachThreadProcessNum);
                            kernel.inputPipeline.put(true, TaskNode.of(chromosomeIndex,
                                    Math.max(nodes.get(startBlockIndex + submittedNum).minPos, startPos),
                                    Math.min(nodes.get(startBlockIndex + blockToProcess - 1).maxPos, endPos),
                                    endPos));
                            submittedNum = blockToProcess;
                        }
                    }
                    kernel.inputPipeline.putStatus(kernel.totalThreadsNum, false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            threadPool.close();
        }
        kernel.outputFile.close();
    }

    /**
     * 计算 LD 系数的核心方法
     */
    private void calculateLD() throws Exception {
        try {
            // 确保按照线程 id 执行任务
            Pair<Integer, Block<Boolean, TaskNode>> initInfo = this.outputFile.getContextId(() -> inputPipeline.get());
            int localId = initInfo.key;
            Block<Boolean, TaskNode> block = initInfo.value;

            // 创建文件读取器
            if (block.getStatus()) {
                VolumeByteStream lineCache = new VolumeByteStream(128);

                GTBReader reader = new GTBReader(this.task.manager);

                // 筛选样本
                if (task.getSubjects() != null) {
                    reader.selectSubjects(task.getSubjects());
                }

                // 创建位点缓冲区, 缓冲区大小由样本个数决定
                Array<Variant> variants = new Array<>(this.maxCacheBlockSize);
                Array<Variant> processedVariants = new Array<>(this.maxCacheBlockSize + 1);

                // 指针
                boolean endOfChromosome;

                // 进行计算
                do {
                    // 限定染色体范围
                    reader.limit(block.getData().chromosomeIndex);

                    // 跳转指针
                    reader.seek(block.getData().chromosomeIndex, block.getData().minPos);

                    int endPosition = block.getData().maxPos;
                    int searchEndPosition = block.getData().maxSearchPos;

                    // 加载初始化位点
                    endOfChromosome = fillCache(localId, variants, processedVariants, reader, searchEndPosition);

                    // 位点池不为空
                    out:
                    while (!variants.isEmpty() || !endOfChromosome) {
                        Variant variant1 = variants.popFirst();
                        if (variant1.position > endPosition) {
                            // 清除所有数据
                            processedVariants.add(variant1);
                            processedVariants.addAll(variants);
                            variants.clear();
                            break;
                        }

                        for (Variant variant2 : variants) {
                            if (variant1.position < variant2.position) {
                                if ((variant2.position - variant1.position > task.getWindowSizeBp())) {
                                    processedVariants.add(variant1);
                                    continue out;
                                }

                                if (ldModel.calculateLDR2(lineCache, variant1, variant2, task.getMinR2()) != 0) {
                                    this.outputFile.write(localId, ByteCode.NEWLINE);
                                    this.outputFile.write(localId, lineCache);
                                }
                            }
                        }

                        // 顺利跳转出 for 循环，此时填充后续位点信息，并再次进行估算
                        if (!endOfChromosome) {
                            // 刷新缓冲区
                            int mark = variants.size();
                            variants.flush();
                            endOfChromosome = fillCache(localId, variants, processedVariants, reader, searchEndPosition);

                            for (int i = mark; i < variants.size(); i++) {
                                Variant variant2 = variants.get(i);

                                if (variant1.position < variant2.position) {
                                    if ((variant2.position - variant1.position > task.getWindowSizeBp())) {
                                        processedVariants.add(variant1);
                                        continue out;
                                    }

                                    if (ldModel.calculateLDR2(lineCache, variant1, variant2, task.getMinR2()) != 0) {
                                        this.outputFile.write(localId, ByteCode.NEWLINE);
                                        this.outputFile.write(localId, lineCache);
                                    }
                                }
                            }


                            // 仍然没有结束，此时就需要借助外部位点信息
                            if (!endOfChromosome) {
                                Pointer pointer = reader.tell();
                                // 创建临时位点及其附属缓冲区
                                Variant cacheVariant = new Variant();
                                cacheVariant.property = ldModel.getProperty(validSampleNum);
                                while (reader.readVariant(cacheVariant) && (cacheVariant.position <= searchEndPosition) && (cacheVariant.position - variant1.position <= task.getWindowSizeBp())) {
                                    if ((variant1.position < cacheVariant.position) && (cacheVariant.getAlternativeAlleleNum() == 2)) {
                                        IVariantProperty property = (IVariantProperty) cacheVariant.property;
                                        property.fillBitCodes(cacheVariant);

                                        if (property.checkMaf(task.getMAF())) {
                                            if (ldModel.calculateLDR2(lineCache, variant1, cacheVariant, task.getMinR2()) != 0) {
                                                this.outputFile.write(localId, ByteCode.NEWLINE);
                                                this.outputFile.write(localId, lineCache);
                                            }
                                        }
                                    }
                                }

                                cacheVariant = null;
                                reader.seek(pointer);
                            }
                        }
                        // 该位点很靠近最后
                        processedVariants.add(variant1);
                    }

                    block = this.outputFile.flush(localId, () -> this.inputPipeline.get());
                } while (block.getStatus());

                reader.close();
                variants.close();
                processedVariants.close();
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean fillCache(int localIndex, Array<Variant> variants, Array<Variant> processedVariants, GTBReader reader, int searchEndPosition) throws IOException, InterruptedException {
        // 加载初始化位点
        Variant variant;
        while (variants.size() < variants.getCapacity()) {
            if (processedVariants.size() == 0) {
                variant = new Variant();
                variant.property = ldModel.getProperty(validSampleNum);
            } else {
                variant = processedVariants.popFirst();
            }

            if (!reader.readVariant(variant) || (variant.position > searchEndPosition)) {
                processedVariants.add(variant);
                return true;
            }

            if (variant.getAlternativeAlleleNum() == 2) {
                // 确保所有的 input 位点都是二等位基因位点，并进行编码转换
                IVariantProperty property = (IVariantProperty) variant.property;
                property.fillBitCodes(variant);

                if (property.checkMaf(task.getMAF())) {
                    variants.add(variant);
                } else {
                    processedVariants.add(variant);
                }
            } else {
                processedVariants.add(variant);
            }
        }
        return false;
    }
}