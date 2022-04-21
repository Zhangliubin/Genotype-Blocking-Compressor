package edu.sysu.pmglab.gbc.core.build;

import edu.sysu.pmglab.gbc.constant.ChromosomeTag;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.common.block.VariantAbstract;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.genotype.GenotypeQC;
import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.compressor.ICompressor;
import edu.sysu.pmglab.container.Pair;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.ShareCache;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.core.exception.FileFormatException;
import edu.sysu.pmglab.gbc.core.exception.GBCExceptionOptions;
import edu.sysu.pmglab.threadPool.Block;
import edu.sysu.pmglab.threadPool.DynamicPipeline;
import edu.sysu.pmglab.threadPool.ThreadPool;
import edu.sysu.pmglab.unifyIO.FileStream;
import edu.sysu.pmglab.unifyIO.partreader.IPartReader;

import java.io.IOException;
import java.util.Arrays;

/**
 * @Data        :2021/02/14
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :Build 核心任务，由 BuildTask 调用
 */

class BuildKernelMultiFile extends BuildKernel {
    /**
     * IO 数据管道
     */
    DynamicPipeline<Boolean, Pair<FileStream, int[]>> uncompressedPipLine;

    /**
     * 对外的提交方法，将任务提交至本类，进行压缩任务
     */
    static void submit(final BuildTask task) throws IOException {
        new BuildKernelMultiFile(task);
    }

    /**
     * 标准构造器，传入 EditTask，根据该提交任务执行工作
     * @param task 待执行任务
     */
    BuildKernelMultiFile(final BuildTask task) throws IOException {
        super(task);
    }

    @Override
    void startWork() throws IOException {
        // 创建数据管道
        this.uncompressedPipLine = new DynamicPipeline<>(task.getThreads() << 2);

        // 创建分块读取器
        IPartReader[] partReaders = new IPartReader[this.task.getInputFileNames().size()];
        for (int i = 0; i < partReaders.length; i++) {
            partReaders[i] = IPartReader.getInstance(this.task.getInputFileName(i));
        }

        // 检验 VCF 文件的样本名序列是否合法
        int[][] relativeIndexes = checkVcfSubjects(partReaders);

        // 创建线程池
        ThreadPool threadPool = new ThreadPool(this.task.getThreads() + 1);

        // 创建 Input 线程
        threadPool.submit(() -> {
            try {
                for (int i = 0; i < partReaders.length; i++) {
                    for (FileStream fileStream : partReaders[i].part(this.task.getThreads())) {
                        this.uncompressedPipLine.put(true, new Pair<>(fileStream, relativeIndexes[i]));
                    }
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
    int[][] checkVcfSubjects(IPartReader[] partReaders) throws IOException {
        // 保存标题模版
        byte[] reference = null;
        GroupSubjectFormatter[] subjectsManagers = new GroupSubjectFormatter[this.task.getInputFileNames().size()];
        int[][] relativeIndexes = new int[subjectsManagers.length][];
        int[] subjectsNum = new int[subjectsManagers.length];
        VolumeByteStream localLineCache = new VolumeByteStream(2 << 21);

        // 记录每个 vcf 文件注释信息长度
        for (int i = 0; i < subjectsManagers.length; i++) {
            // 获取文件句柄
            IPartReader vcfReader = partReaders[i];

            // 初始化为第一行文本
            vcfReader.readLine(localLineCache);

            // 遍历文件，将注释信息过滤掉
            while ((localLineCache.cacheOf(0) == ByteCode.NUMBER_SIGN) && (localLineCache.cacheOf(1) == ByteCode.NUMBER_SIGN)) {
                // 检验是否为参考序列地址，如果检测到，则将其保存下来，并在后续一并刷入 gtb 文件
                if ((reference == null) && localLineCache.startWith(ByteCode.REFERENCE_STRING)) {
                    reference = localLineCache.cacheOf(ByteCode.REFERENCE_STRING.length, localLineCache.size());
                }
                localLineCache.reset(0);

                // 载入下一行
                vcfReader.readLine(localLineCache);
            }

            /* 校验标题行 */
            Assert.that((localLineCache.cacheOf(0) == ByteCode.NUMBER_SIGN) && localLineCache.size() >= 45, GBCExceptionOptions.FileFormatException, "doesn't match to standard VCF file (#CHROM POS ID REF ALT QUAL FILTER INFO FORMAT <S1 ...>)");

            // 样本名信息
            byte[] subjects;
            if (localLineCache.size() == 45) {
                subjects = new byte[0];
            } else {
                subjects = localLineCache.takeOut(46);
            }

            if (subjectsManagers[0] == null) {
                // 如果第一个模版变量没有数据，则填充
                subjectsManagers[i] = new GroupSubjectFormatter(subjects, true);
            } else {
                // 检验当前文件的样本序列与之前是否一致
                for (int j = 0; j < i; j++) {
                    if (subjectsManagers[j].equal(subjects)) {
                        subjectsManagers[i] = subjectsManagers[j];
                        break;
                    }
                }

                // 如果都不一致，则设置为新的样本管理器
                if (subjectsManagers[i] == null) {
                    subjectsManagers[i] = new GroupSubjectFormatter(subjects, true);
                }
            }

            subjectsNum[i] = subjectsManagers[i].subjectNum;
        }

        // 关闭本地行缓冲区
        localLineCache.close();

        // 将最长的样本序列作为主样本
        int mainSubjectManagerIndex = ValueUtils.argmax(subjectsNum);
        this.validSubjectNum = subjectsManagers[mainSubjectManagerIndex].subjectNum;

        // 主样本设置为 range(1~n)
        relativeIndexes[mainSubjectManagerIndex] = ArrayUtils.range(this.validSubjectNum - 1);
        subjectsManagers[mainSubjectManagerIndex].relativeIndexes = relativeIndexes[mainSubjectManagerIndex];

        // 检验其他所有的样本名管理器
        for (int i = 0; i < subjectsManagers.length; i++) {
            try {
                if (!subjectsManagers[i].equals(subjectsManagers[mainSubjectManagerIndex])) {
                    relativeIndexes[i] = subjectsManagers[mainSubjectManagerIndex].get(new String(subjectsManagers[i].subjects).split("\t"));
                } else {
                    relativeIndexes[i] = relativeIndexes[mainSubjectManagerIndex];
                }
            } catch (NullPointerException e) {
                throw new FileFormatException("The subjects in " + this.task.getInputFileName(i) + " are different from the file's received before, maybe they came from different groups of subjects. Please remove it from inputFileNames.");
            }
        }

        // 验证块大小参数
        int blockSizeType = BlockSizeParameter.getSuggestBlockSizeType(task.getBlockSizeType(), validSubjectNum);
        this.blockSize = BlockSizeParameter.getBlockSize(blockSizeType);
        this.baseInfoManager.setBlockSizeType(blockSizeType);

        // 写入块大小占位符
        this.outputFile.write(ValueUtils.value2ByteArray(0, 5));

        // 写入 refer 网址
        if (reference != null) {
            this.outputFile.write(reference);
        }

        // 写入换行符
        this.outputFile.write(ByteCode.NEWLINE);

        // 写入样本名
        VolumeByteStream subjectSeq = ICompressor.compress(task.getCompressor(), task.getCompressionLevel(), subjectsManagers[mainSubjectManagerIndex].subjects, 0, subjectsManagers[mainSubjectManagerIndex].subjects.length);
        this.outputFile.writeIntegerValue(subjectSeq.size());
        this.outputFile.write(subjectSeq);

        // 返回相对索引表
        return relativeIndexes;
    }

    /**
     * 读取基因组文件
     */
    @Override
    void processFileStream() {
        try {
            Block<Boolean, Pair<FileStream, int[]>> fileStreamBlock = this.uncompressedPipLine.get();
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
                    FileStream fileReader = fileStreamBlock.getData().key;
                    int[] relativeIndex = fileStreamBlock.getData().value;

                    out:
                    while (true) {
                        // 缓冲区没有数据，并且到达了文件末尾
                        boolean continueSearch;
                        do {
                            if ((localLineCache.size() == 0) && (fileReader.readLine(localLineCache) == -1)) {
                                break out;
                            }

                            // 识别对应的染色体类型及编号
                            ChromosomeTag chromosome;
                            int ind = localLineCache.indexOf(ByteCode.TAB);
                            if (localLineCache.startWith(ByteCode.CHR_STRING)) {
                                // 从支持的染色体列表中获取数据
                                chromosome = ChromosomeTags.get(localLineCache, 3, ind - 3);
                            } else {
                                chromosome = ChromosomeTags.get(localLineCache, 0, ind);
                            }
                            chromosomeInfo = localLineCache.cacheOf(0, ind);

                            // 跳过该位点
                            if (chromosome == null) {
                                localLineCache.reset();
                                continueSearch = true;
                            } else {
                                uncompressedBlock.chromosomeIndex = chromosome.chromosomeIndex;
                                continueSearch = !formatVariant(localLineCache, uncompressedBlock.variants[uncompressedBlock.seek], encodedCache.getCache(), genotypeQC, relativeIndex, uncompressedBlock.chromosomeIndex, relativeIndex.length < this.validSubjectNum);
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
                                    if (formatVariant(localLineCache, uncompressedBlock.variants[uncompressedBlock.seek], encodedCache.getCache(), genotypeQC, relativeIndex, uncompressedBlock.chromosomeIndex, relativeIndex.length < this.validSubjectNum)) {
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

                caches.freeMemory();
                ctx.close();
                localLineCache.close();
            }

        } catch (IOException | InterruptedException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * 编码基因型数据
     * @param formatter 外部格式匹配器
     * @param relativeIndexes 编码的索引顺序
     * @param init 初始化编码值为 .
     */
    boolean formatVariant(final VolumeByteStream lineCache, final VariantAbstract variant, final byte[] encodedCache, final GenotypeQC formatter, final int[] relativeIndexes, final int chromosomeIndex, final boolean init) {
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

            // 是否填充缺失编码
            if (init) {
                Arrays.fill(encodedCache, variant.encodedStart, variant.encodedStart + variant.encodedLength, this.begEncoder.encodeMiss());
            }

            if (genotypeStart != -1) {
                // 加载基因型格式化匹配器
                int[] indexes = formatter.load(lineCache, formatStart, genotypeStart);

                /* 根据是否仅有 GT，决定是否需要进行过滤 */
                if (indexes == null) {
                    int length;
                    int seek = genotypeStart;
                    int subjectIndex;

                    for (int i = 0; i < relativeIndexes.length - 1; i++) {
                        length = 1;
                        seek += 2;
                        subjectIndex = relativeIndexes[i];

                        while (lineCache.cacheOf(seek) != ByteCode.TAB) {
                            seek++;
                            length++;
                        }

                        encodedCache[variant.encodedStart + subjectIndex] = this.begEncoder.encode(lineCache.getCache(), seek, length);
                    }

                    length = lineCache.size() - seek - 1;
                    seek = lineCache.size();
                    encodedCache[variant.encodedStart + relativeIndexes[relativeIndexes.length - 1]] = this.begEncoder.encode(lineCache.getCache(), seek, length);
                } else {
                    int length, mark, genotypeLength;
                    int seek = genotypeStart;
                    int subjectIndex;
                    for (int i = 0; i < relativeIndexes.length - 1; i++) {
                        length = 1;
                        seek += 2;
                        subjectIndex = relativeIndexes[i];

                        // 当前基因型是否为 .
                        if (lineCache.cacheOf(seek - length) == ByteCode.PERIOD) {
                            encodedCache[variant.encodedStart + subjectIndex] = this.begEncoder.encodeMiss();
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

                            encodedCache[variant.encodedStart + subjectIndex] = this.begEncoder.encode(formatter.filter(lineCache, seek, length, indexes), lineCache.getCache(), mark, genotypeLength);
                        }
                    }

                    // 最后一个基因型数据
                    genotypeLength = 1;
                    mark = seek + 2;

                    // 获取样本索引
                    subjectIndex = relativeIndexes[relativeIndexes.length - 1];

                    // 当前基因型是否为 .
                    if (lineCache.cacheOf(mark - genotypeLength) == ByteCode.PERIOD) {
                        encodedCache[variant.encodedStart + subjectIndex] = this.begEncoder.encodeMiss();
                    } else {
                        length = lineCache.size() - seek - 1;
                        seek = lineCache.size();

                        // 检测 :
                        while (lineCache.cacheOf(mark) != ByteCode.COLON) {
                            mark++;
                            genotypeLength++;
                        }

                        encodedCache[variant.encodedStart + subjectIndex] = this.begEncoder.encode(formatter.filter(lineCache, seek, length, indexes), lineCache.getCache(), mark, genotypeLength);
                    }
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
                if (ChromosomeTags.getPloidy(chromosomeIndex) == 1) {
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
}