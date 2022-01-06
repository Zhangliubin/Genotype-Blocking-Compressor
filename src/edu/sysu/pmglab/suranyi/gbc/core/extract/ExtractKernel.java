package edu.sysu.pmglab.suranyi.gbc.core.extract;

import edu.sysu.pmglab.suranyi.container.ShareCache;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.threadPool.Block;
import edu.sysu.pmglab.suranyi.threadPool.DynamicPipeline;
import edu.sysu.pmglab.suranyi.threadPool.ThreadPool;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;
import edu.sysu.pmglab.suranyi.unifyIO.partwriter.BGZIPBlockWriter;
import edu.sysu.pmglab.suranyi.unifyIO.partwriter.IBlockWriter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Data        :2021/02/14
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :Extract 核心任务，由 ExtractTask 调用
 */

class ExtractKernel {
    /**
     * 一个 kernel 只能绑定一个任务
     */
    final ExtractTask task;

    /**
     * IO 数据管道
     */
    final DynamicPipeline<Boolean, TaskGTBNode> inputPipeline;

    /**
     * 线程池
     */
    final ThreadPool threadPool;

    /**
     * 根数据管理器
     */
    final GTBManager gtbManager;

    /**
     * 输出数据文件
     */
    final FileStream outputFile;

    /**
     * 样本索引
     */
    final int[] subjectIndexes;

    /**
     * 基因型数据重构机
     */
    final VCFFormatterBuilder rebuilder;
    final int maxOriginMBEGsSize;
    final int maxOriginAllelesSize;

    /**
     * 创建的上下文管理器 id
     * 异步输出控制信号量
     */
    private final Semaphore[] semaphores;
    private final AtomicInteger contextId = new AtomicInteger(0);

    static void decompressAll(ExtractTask task) throws IOException {
        ExtractKernel kernel = new ExtractKernel(task);

        // 提交任务线程
        kernel.threadPool.submit(() -> {
            TaskGenerator.decompressAll(kernel);
            try {
                kernel.inputPipeline.putStatus(task.getThreads(), false);
            } catch (InterruptedException e) {
                // e.printStackTrace();  // 调试使用
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        kernel.run(kernel.gtbManager.getChromosomeList());
    }

    static void decompressByChromosome(ExtractTask task, int... chromosomes) throws IOException {
        ExtractKernel kernel = new ExtractKernel(task);

        // 提交任务线程
        kernel.threadPool.submit(() -> {
            TaskGenerator.decompress(kernel, chromosomes);
            try {
                kernel.inputPipeline.putStatus(task.getThreads(), false);
            } catch (InterruptedException e) {
                // e.printStackTrace();  // 调试使用
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        kernel.run(chromosomes);
    }

    static void decompressByRange(ExtractTask task, int chromosomeIndex, int startPos, int endPos) throws IOException {
        ExtractKernel kernel = new ExtractKernel(task);

        // 提交任务线程
        kernel.threadPool.submit(() -> {
            TaskGenerator.decompress(kernel, chromosomeIndex, startPos, endPos);
            try {
                kernel.inputPipeline.putStatus(task.getThreads(), false);
            } catch (InterruptedException e) {
                // e.printStackTrace();  // 调试使用
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        kernel.run(chromosomeIndex);
    }

    static <ChromosomeType> void decompressByPosition(ExtractTask task, Map<ChromosomeType, int[]> chromosomeNodeIndexesUnknownType) throws IOException {
        ExtractKernel kernel = new ExtractKernel(task);
        HashMap<Integer, int[]> chromosomePositions = ChromosomeInfo.identifyChromosome(chromosomeNodeIndexesUnknownType);

        // 提交任务线程
        kernel.threadPool.submit(() -> {
            TaskGenerator.decompress(kernel, chromosomePositions);
            try {
                kernel.inputPipeline.putStatus(task.getThreads(), false);
            } catch (InterruptedException e) {
                // e.printStackTrace();  // 调试使用
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        kernel.run(ArrayUtils.getIntegerKey(chromosomePositions));
    }

    static <ChromosomeType> void decompressByNodeIndex(ExtractTask task, Map<ChromosomeType, int[]> chromosomeNodeIndexesUnknownType) throws IOException {
        ExtractKernel kernel = new ExtractKernel(task);
        HashMap<Integer, int[]> chromosomeNodeIndexes = ChromosomeInfo.identifyChromosome(chromosomeNodeIndexesUnknownType);

        // 提交任务线程
        kernel.threadPool.submit(() -> {
            TaskGenerator.decompressByNodeIndex(kernel, chromosomeNodeIndexes);
            try {
                kernel.inputPipeline.putStatus(task.getThreads(), false);
            } catch (InterruptedException e) {
                // e.printStackTrace();  // 调试使用
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        kernel.run(ArrayUtils.getIntegerKey(chromosomeNodeIndexes));
    }

    /**
     * 标准构造器，传入 EditTask，根据该提交任务执行工作
     *
     * @param task 待执行任务
     */
    private ExtractKernel(ExtractTask task) throws IOException {
        // 设置任务
        this.task = task;

        // 读取 gtb 文件，并创建管理器
        this.gtbManager = GTBRootCache.get(task.getInputFileName());

        // 设置输出数据文件
        this.outputFile = new FileStream(this.task.getOutputFileName(), FileOptions.CHANNEL_WRITER);

        // 设置输入数据管道，并初始化任务生成器
        this.inputPipeline = new DynamicPipeline<>(this.task.getThreads() << 2);

        // 创建线程池
        this.threadPool = new ThreadPool(this.task.getThreads() + 1);

        // 重构器加载数据，为 null 说明没有指定顺序
        this.subjectIndexes = this.task.getSubjects() == null ? null : this.gtbManager.getSubjectIndex(this.task.getSubjects());

        // 设定任务索引对
        IndexPair[] pairs;

        // 组合编码需要 `任务对` 辅助解压
        int eachGroupNum = this.gtbManager.isPhased() ? 3 : 4;
        if (this.subjectIndexes == null) {
            pairs = new IndexPair[this.gtbManager.getSubjectNum()];

            for (int i = 0; i < pairs.length; i++) {
                pairs[i] = new IndexPair(i, i / eachGroupNum, i % eachGroupNum);
            }
        } else {
            pairs = new IndexPair[this.subjectIndexes.length];

            for (int i = 0; i < pairs.length; i++) {
                pairs[i] = new IndexPair(this.subjectIndexes[i], this.subjectIndexes[i] / eachGroupNum, this.subjectIndexes[i] % eachGroupNum);
            }
        }

        this.maxOriginMBEGsSize = this.gtbManager.getMaxDecompressedMBEGsSize();
        this.maxOriginAllelesSize = this.gtbManager.getMaxDecompressedAllelesSize();

        // 创建信号量
        this.semaphores = new Semaphore[this.task.getThreads()];
        for (int i = 0; i < this.semaphores.length; i++) {
            this.semaphores[i] = new Semaphore(0);
        }

        this.rebuilder = new VCFFormatterBuilder(BEGDecoder.getDecoder(task.isPhased() == null ? this.gtbManager.isPhased() : task.isPhased()), this.gtbManager.getMBEGDecoder(), pairs, this.gtbManager, this.task, this.maxOriginAllelesSize);
    }

    /**
     * 运行程序: 创建线程池 -> 开启 IO 线程 -> 执行任务 -> 关闭线程池 -> 合并文件
     */
    private void run(int... chromosome) throws IOException {
        // 输出头部信息
        writeHeader(chromosome, this.task.isHideGenotype() ? null : (this.subjectIndexes == null ? this.gtbManager.getSubjects() : String.join("\t", this.gtbManager.getSubject(this.subjectIndexes)).getBytes()));

        // 创建工作线程
        this.threadPool.submit(this::processGenomeBlock, this.task.getThreads());

        // 关闭线程池，等待任务完成
        this.threadPool.close();

        // 清空数据
        this.inputPipeline.clear();

        // 写入块结尾数据
        if (this.task.isCompressToBGZF()) {
            this.outputFile.write(BGZIPBlockWriter.EMPTY_GZIP_BLOCK);
        }

        // 关闭文件资源
        this.outputFile.close();
    }

    /**
     * 处理 基因组 block
     */
    private void processGenomeBlock() {
        try {
            int localId;
            Block<Boolean, TaskGTBNode> block;

            // 确保按照线程 id 执行任务
            synchronized (contextId) {
                // 标记线程 ID
                localId = contextId.getAndAdd(1);

                // 获取第一个块任务 (按照 localId 顺序获得了任务)
                block = this.inputPipeline.get();

                // 当所有当线程都获得了一个任务后才释放初始信号量，避免第一个线程已经解压完成、请求新数据，但是最后一个线程还未获得新数据
                if (localId == (this.semaphores.length - 1)) {
                    this.semaphores[0].release();
                }
            }

            // 确定有非空任务了再进行初始化
            if (block.getStatus()) {
                // 创建上下文环境
                ShareCache globalCache = new ShareCache();
                globalCache.alloc(0);
                globalCache.alloc(maxOriginMBEGsSize);
                globalCache.alloc(maxOriginAllelesSize);

                ExtractContext ctx = new ExtractContext(this.gtbManager, this.rebuilder.getInstance(), globalCache);

                // 提取 input 数据
                do {
                    // 处理 inputBlock 数据
                    ByteBuffer out = ctx.process(block.getData());

                    // 请求信号量
                    this.semaphores[localId].acquire();

                    // 输出数据
                    if ((out != null) && (out.limit() != 0)) {
                        this.outputFile.write(out);
                    }

                    // 避免数据输出很快，被其他线程抢占
                    block = this.inputPipeline.get();

                    // 释放下一个工作线程的信号量
                    this.semaphores[(localId == this.semaphores.length - 1) ? 0 : localId + 1].release();
                } while (block.getStatus());

                // 关闭上下文
                ctx.close();
                globalCache.freeMemory();
            }
        } catch (InterruptedException | IOException e) {
            // e.printStackTrace();  // 调试使用
            throw new UnsupportedOperationException(e.getMessage());
        }
    }

    /**
     * 写入 vcf 文件的头部信息
     */
    private void writeHeader(int[] chromosomes, byte[] subjects) throws IOException {
        // 初始为 2 MB
        VolumeByteStream cache = new VolumeByteStream((1 << 20) + (subjects == null ? 0 : subjects.length));

        // 写入块头
        cache.write(("##fileformat=VCFv4.2" +
                "\n##FILTER=<ID=PASS,Description=\"All filters passed\">" +
                "\n##source=" + this.task.getInputFileName() +
                "\n##Version=<gbc_version=1.1,java_version=" + System.getProperty("java.version") + ",zstd_jni=1.4.9-5>"));

        // 参考序列非空时，写入参考序列
        if (!this.gtbManager.isReferenceEmpty()) {
            cache.write("\n##reference=");
            cache.write(this.gtbManager.getReference());
        }

        // 写入 contig 信息
        for (int chromosomeIndex : chromosomes) {
            cache.write(ChromosomeInfo.getHeader(chromosomeIndex));
        }

        cache.write("\n##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes\">" +
                "\n##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">" +
                "\n##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency\">" +
                "\n##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">" +
                "\n#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");

        if (subjects != null) {
            cache.write(ByteCode.TAB);
            cache.write(subjects);
        }

        // 完成
        IBlockWriter<ByteBuffer> outputStream = IBlockWriter.getByteBufferInstance(this.task.outputParam, cache.size());
        outputStream.write(cache);
        outputStream.finish();
        outputFile.write(outputStream.getCache());
        outputStream.close();
    }
}