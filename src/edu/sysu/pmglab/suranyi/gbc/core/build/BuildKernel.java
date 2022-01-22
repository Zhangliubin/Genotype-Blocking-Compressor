package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.Pair;
import edu.sysu.pmglab.suranyi.container.ShareCache;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.suranyi.gbc.constant.Chromosome;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.common.block.VariantAbstract;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.genotype.GenotypeQC;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantQC;
import edu.sysu.pmglab.suranyi.gbc.core.exception.GBCExceptionOptions;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.FileBaseInfoManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBTree;
import edu.sysu.pmglab.suranyi.threadPool.Block;
import edu.sysu.pmglab.suranyi.threadPool.DynamicPipeline;
import edu.sysu.pmglab.suranyi.threadPool.ThreadPool;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;
import edu.sysu.pmglab.suranyi.unifyIO.partreader.IPartReader;

import java.io.IOException;

/**
 * @Data        :2021/02/14
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :Build 核心任务，由 BuildTask 调用
 */

class BuildKernel {
    /**
     * 一个 kernel 只能绑定一个任务
     */
    final BuildTask task;

    /**
     * IO 数据管道
     */
    DynamicPipeline<Boolean, FileStream> uncompressedPipLine;

    /**
     * 文件基本信息
     */
    int validSubjectNum;
    int blockSize;

    /**
     * 位点控制器
     */
    final VariantQC variantQC;
    final GenotypeQC genotypeQC;
    final AlleleQC alleleQC;

    /**
     * 根数据管理器，压缩后的数据在此处进行校验
     */
    final FileBaseInfoManager baseInfoManager;
    final SmartList<GTBNode> GTBNodeCache = new SmartList<>(1024, true);
    int maxEstimateSize = 0;

    /**
     * 输出数据文件
     */
    final FileStream outputFile;

    /**
     * 状态值
     */
    boolean status;

    /**
     * 编码器, 将文本基因型编码为字节编码
     */
    final BEGEncoder begEncoder;

    /**
     * 对外的提交方法，将任务提交至本类，进行压缩任务
     */
    static void submit(final BuildTask task) throws IOException {
        new BuildKernel(task);
    }

    /**
     * 标准构造器，传入 EditTask，根据该提交任务执行工作
     * @param task 待执行任务
     */
    BuildKernel(final BuildTask task) throws IOException {
        // 加载 BEG 编码表和 MBEG 编码表
        this.begEncoder = BEGEncoder.getEncoder(task.isPhased());

        // 设置任务
        this.task = task;

        // 设置基本信息管理器
        this.baseInfoManager = FileBaseInfoManager.of(task);

        // 位点控制器
        this.variantQC = task.getVariantQC();
        this.genotypeQC = task.getGenotypeQC();
        this.alleleQC = task.getAlleleQC();

        // 设置输出数据文件
        this.outputFile = new FileStream(task.getOutputFileName(), FileOptions.CHANNEL_WRITER);

        // 开始进行工作
        startWork();

        // 生成最后的 gtb 文件
        generateGTBFile();

        // 正常完成任务后设定状态值为 true
        this.status = true;
    }

    void startWork() throws IOException {
        // 创建数据管道
        this.uncompressedPipLine = new DynamicPipeline<>(task.getThreads() << 2);

        // 创建分块读取器 (只有一个文件，因此通过 index=0 获取)
        IPartReader partReader = IPartReader.getInstance(this.task.getInputFileName(0));

        // 检验 VCF 文件的样本名序列是否合法
        checkVcfSubject(partReader);

        // 创建线程池
        ThreadPool threadPool = new ThreadPool(this.task.getThreads() + 1);

        // 创建 Input 线程
        threadPool.submit(() -> {
            try {
                for (FileStream fileStream : partReader.part(this.task.getThreads())) {
                    this.uncompressedPipLine.put(true, fileStream);
                }

                // 发送关闭信号
                this.uncompressedPipLine.putStatus(this.task.getThreads(), false);
            } catch (Exception | Error e) {
                // e.printStackTrace();
                throw new UnsupportedOperationException(e.getMessage());
            }
        });

        threadPool.submit(() -> {
            try {
                processFileStream();
            } catch (Exception | Error e) {
                // e.printStackTrace();
                throw new UnsupportedOperationException(e.getMessage());
            }

        }, this.task.getThreads());

        // 关闭线程池，等待任务完成
        threadPool.close();

        // 清除数据区
        this.uncompressedPipLine.clear();
    }

    /**
     * 初始化 VCF 文件信息
     */
    void checkVcfSubject(IPartReader partReader) throws IOException {
        // 保存标题模版
        byte[] reference = null;
        VolumeByteStream localLineCache = new VolumeByteStream(2 << 21);

        // 初始化为第一行文本
        partReader.readLine(localLineCache);

        // 遍历文件，将注释信息过滤掉
        while ((localLineCache.cacheOf(0) == ByteCode.NUMBER_SIGN) && (localLineCache.cacheOf(1) == ByteCode.NUMBER_SIGN)) {
            // 检验是否为参考序列地址，如果检测到，则将其保存下来，并在后续一并刷入 gtb 文件
            if (localLineCache.startWith(ByteCode.REFERENCE_STRING) && (reference == null)) {
                // 请注意，当存在两行的 ref 时，也只保留第一行
                // 判断末尾是否有奇怪的 \r
                reference = localLineCache.cacheOf(ByteCode.REFERENCE_STRING.length, localLineCache.size());
            }
            localLineCache.reset(0);

            // 载入下一行
            partReader.readLine(localLineCache);
        }

        /* 校验标题行 */
        Assert.that((localLineCache.cacheOf(0) == ByteCode.NUMBER_SIGN) && localLineCache.size() > 46, GBCExceptionOptions.FileFormatException, "doesn't match to standard VCF file");

        // 样本名信息
        byte[] subjects = localLineCache.endWith(ByteCode.CARRIAGE_RETURN) ? localLineCache.takeOut(46, localLineCache.size() - 47) : localLineCache.takeOut(46);

        // 如果第一个模版变量没有数据，则填充
        GroupSubjectFormatter subjectManager = new GroupSubjectFormatter(subjects);
        this.validSubjectNum = subjectManager.subjectNum;

        // 关闭本地行缓冲区
        localLineCache.close();

        // 验证块大小参数
        int blockSizeType = BlockSizeParameter.getSuggestBlockSizeType(task.getBlockSizeType(), validSubjectNum);
        this.blockSize = BlockSizeParameter.getBlockSize(blockSizeType);
        this.baseInfoManager.setBlockSizeType(blockSizeType);

        // 写入初始头信息
        this.outputFile.write(ValueUtils.value2ByteArray(0, 5));

        // 写入 refer 网址
        if (reference != null) {
            this.outputFile.write(reference);
        }

        // 写入换行符
        this.outputFile.write(ByteCode.NEWLINE);

        // 写入样本名
        VolumeByteStream subjectsSeq = ICompressor.compress(task.getCompressor(), task.getCompressionLevel(), subjectManager.subjects, 0, subjectManager.subjects.length);
        this.outputFile.writeIntegerValue(subjectsSeq.size());
        this.outputFile.write(subjectsSeq);
    }

    /**
     * 读取基因组文件
     */
    void processFileStream() {
        try {
            Block<Boolean, FileStream> fileStreamBlock = this.uncompressedPipLine.get();
            // 确认为需要处理的任务块，只有需要这么一些线程的时候，才会创建容器
            if (fileStreamBlock.getStatus()) {
                // 创建本地 lineCache 缓冲区
                VolumeByteStream localLineCache = new VolumeByteStream(2 << 20);

                // 创建本地编码缓冲区
                VolumeByteStream encodedCache = new VolumeByteStream(this.validSubjectNum * this.blockSize);

                // 创建压缩上下文
                ShareCache caches = new ShareCache(encodedCache, new VolumeByteStream((blockSize * Math.max(20, validSubjectNum)) >> 1));
                GTBCompressionContext ctx = new GTBCompressionContext(this.task, this.validSubjectNum, caches);

                // 染色体信息
                byte[] chromosomeInfo;

                // 未压缩的块
                UncompressedBlock uncompressedBlock = new UncompressedBlock(this.validSubjectNum, this.task, this.blockSize, encodedCache);

                do {
                    // 提取要处理的文件块
                    FileStream fileReader = fileStreamBlock.getData();

                    out:
                    while (true) {
                        // 缓冲区没有数据，并且到达了文件末尾
                        boolean continueSearch;
                        do {
                            if ((localLineCache.size() == 0) && (fileReader.readLine(localLineCache) == -1)) {
                                break out;
                            }

                            // 识别对应的染色体类型及编号
                            Chromosome chromosome;
                            int ind = localLineCache.indexOf(ByteCode.TAB) + 1;
                            if (localLineCache.startWith(ByteCode.CHR_STRING)) {
                                // 从支持的染色体列表中获取数据
                                chromosome = ChromosomeInfo.get(localLineCache, 3, ind - 4);
                            } else {
                                chromosome = ChromosomeInfo.get(localLineCache, 0, ind - 1);
                            }
                            chromosomeInfo = localLineCache.cacheOf(0, ind);

                            // 跳过该位点
                            if (chromosome == null) {
                                localLineCache.reset();
                                continueSearch = true;
                            } else {
                                uncompressedBlock.chromosomeIndex = chromosome.chromosomeIndex;
                                continueSearch = !formatVariant(localLineCache, uncompressedBlock.variants[uncompressedBlock.seek], encodedCache.getCache(), genotypeQC, uncompressedBlock.chromosomeIndex);
                            }

                        } while (continueSearch);

                        // 填充 blockData 的第一个位点，让整个数据块非空
                        uncompressedBlock.seek++;

                        // 当 block 未满时，进行填充 2-xxx 位点
                        while (uncompressedBlock.remaining() > 0) {
                            // 获取该行变异位点数据，读取到空行时，该文件结束，跳转至下一个文件
                            if (fileReader.readLine(localLineCache) == -1) {
                                // 已经到了文件最后，则处理完成后接受下一个文件任务
                                processGTBBlock(ctx, uncompressedBlock);
                                break out;
                            } else {
                                // 相邻两行染色体不一致时，发送当前数据，并跳转至下一个块数据
                                if (!localLineCache.startWith(chromosomeInfo)) {
                                    processGTBBlock(ctx, uncompressedBlock);
                                    continue out;
                                } else {
                                    // 进行质控
                                    if (formatVariant(localLineCache, uncompressedBlock.variants[uncompressedBlock.seek], encodedCache.getCache(), genotypeQC, uncompressedBlock.chromosomeIndex)) {
                                        uncompressedBlock.seek++;
                                    }
                                }
                            }
                        }

                        // 完整地写入了一个块
                        if (!uncompressedBlock.empty()) {
                            processGTBBlock(ctx, uncompressedBlock);
                        }
                    }

                    // 该文件已经被读取完毕
                    fileReader.close();

                    // 继续读取下一个文件任务
                    fileStreamBlock = this.uncompressedPipLine.get();
                } while (fileStreamBlock.getStatus());

                ctx.close();
                localLineCache.close();
                caches.freeMemory();
            }

        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    /**
     * 编码基因型数据
     * @param formatter 外部格式匹配器
     */
    boolean formatVariant(final VolumeByteStream lineCache, final VariantAbstract variant, final byte[] encodedCache, final GenotypeQC formatter, final int chromosomeIndex) {
        try {
            // 提取质控字段
            int posStart = lineCache.indexOf(ByteCode.TAB, 1) + 1;
            int posEnd = lineCache.indexOf(ByteCode.TAB, posStart + 1);
            int altStart = lineCache.indexOf(ByteCode.TAB, posEnd + 2) + 1;
            int refStart = lineCache.indexOf(ByteCode.TAB, altStart + 1) + 1;

            // refEnd + 1, qualEnd 获得质量值
            int refEnd = lineCache.indexOf(ByteCode.TAB, refStart + 1);
            int qualEnd = lineCache.indexOf(ByteCode.TAB, refEnd + 2);

            // infoStart, formatStart - 1 获得 INFO 值
            int infoStart = lineCache.indexOf(ByteCode.TAB, qualEnd + 2) + 1;
            int formatStart = lineCache.indexOf(ByteCode.TAB, infoStart + 1) + 1;
            int genotypeStart = lineCache.indexOf(ByteCode.TAB, formatStart + 2);

            // 记录等位基因个数
            int allelesNum = 2;
            for (int i = refStart; i < refEnd; i++) {
                if (lineCache.cacheOf(i) == ByteCode.COMMA) {
                    allelesNum += 1;
                }
            }

            // 如果质控值为 true，说明该位点应该被删除，此时返回 false
            if (this.variantQC.filter(lineCache, allelesNum, refEnd + 1, qualEnd, infoStart, formatStart - 1)) {
                return false;
            }

            // 记录编码器索引
            variant.encoderIndex = allelesNum == 2 ? 0 : 1;

            // 加载基因型格式化匹配器
            int[] indexes = formatter.load(lineCache, formatStart, genotypeStart);

            /* 根据是否仅有 GT，决定是否需要进行过滤 */
            if (lineCache.endWith(ByteCode.CARRIAGE_RETURN)) {
                // 重设换行符类型
                lineCache.reset(lineCache.size() - 1);
            }

            if (indexes == null) {
                int length;
                int seek = genotypeStart;

                for (int i = 0; i < this.validSubjectNum - 1; i++) {
                    length = 1;
                    seek += 2;

                    while (lineCache.cacheOf(seek) != ByteCode.TAB) {
                        seek++;
                        length++;
                    }

                    encodedCache[variant.encodedStart + i] = this.begEncoder.encode(lineCache.getCache(), seek, length);
                }

                length = lineCache.size() - seek - 1;
                seek = lineCache.size();
                encodedCache[variant.encodedStart + this.validSubjectNum - 1] = this.begEncoder.encode(lineCache.getCache(), seek, length);
            } else {
                int length, mark, genotypeLength;
                int seek = genotypeStart;

                for (int i = 0; i < this.validSubjectNum - 1; i++) {
                    length = 1;
                    seek += 2;

                    // 当前基因型是否为 .
                    if (lineCache.cacheOf(seek - length) == ByteCode.PERIOD) {
                        encodedCache[variant.encodedStart + i] = this.begEncoder.encodeMiss();
                        seek = lineCache.indexOf(ByteCode.TAB, seek);
                    } else {
                        // 检测 :
                        while (lineCache.cacheOf(seek) != ByteCode.COLON) {
                            seek++;
                            length++;
                        }

                        mark = seek;
                        genotypeLength = length;

                        // 检测 tab 分隔符
                        while (lineCache.cacheOf(seek) != ByteCode.TAB) {
                            seek++;
                            length++;
                        }

                        encodedCache[variant.encodedStart + i] = this.begEncoder.encode(formatter.filter(lineCache, seek, length, indexes), lineCache.getCache(), mark, genotypeLength);
                    }
                }

                // 最后一个基因型数据
                genotypeLength = 1;
                mark = seek + 2;

                // 当前基因型是否为 .
                if (lineCache.cacheOf(mark - genotypeLength) == ByteCode.PERIOD) {
                    encodedCache[variant.encodedStart + this.validSubjectNum - 1] = this.begEncoder.encodeMiss();
                } else {
                    length = lineCache.size() - seek - 1;
                    seek = lineCache.size();

                    // 检测 :
                    while (lineCache.cacheOf(mark) != ByteCode.COLON) {
                        mark++;
                        genotypeLength++;
                    }

                    encodedCache[variant.encodedStart + this.validSubjectNum - 1] = this.begEncoder.encode(formatter.filter(lineCache, seek, length, indexes), lineCache.getCache(), mark, genotypeLength);
                }
            }

            /* 群体等位基因水平过滤器，该过滤器不一定 work */
            if (this.alleleQC.size() != 0) {
                int alleleCounts = 0;
                int validAllelesNum = 0;
                byte code;
                for (int i = 0; i < variant.encodedLength; i++) {
                    code = encodedCache[variant.encodedStart + i];

                    if (code != 0) {
                        validAllelesNum += 1;

                        // 0 等位基因个数
                        alleleCounts += 2 - this.begEncoder.scoreOf(code);
                    }
                }

                // 单倍型
                if (ChromosomeInfo.getPloidy(chromosomeIndex) == 1) {
                    alleleCounts = alleleCounts >> 1;
                } else {
                    // 二倍型
                    validAllelesNum = validAllelesNum << 1;
                }

                if (this.alleleQC.filter(alleleCounts, validAllelesNum)) {
                    return false;
                }
            }

            // 转换位置数据
            variant.position = 0;
            for (int i = posStart; i < posEnd; i++) {
                variant.position = variant.position * 10 + (lineCache.cacheOf(i) - 48);
            }

            // 设置等位基因
            variant.setAllele(lineCache, altStart, refEnd - altStart);

            return true;
        } finally {
            lineCache.reset();
        }
    }


    /**
     * 处理 基因组 block
     */
    void processGTBBlock(GTBCompressionContext ctx, UncompressedBlock uncompressedBlock) throws IOException {
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
    }
}



