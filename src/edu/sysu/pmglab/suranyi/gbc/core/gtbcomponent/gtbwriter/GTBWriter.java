package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbwriter;

import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.Pair;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.build.BlockSizeParameter;
import edu.sysu.pmglab.suranyi.gbc.core.common.block.VariantAbstract;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.suranyi.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.FileBaseInfoManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBTree;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.suranyi.threadPool.Block;
import edu.sysu.pmglab.suranyi.threadPool.DynamicPipeline;
import edu.sysu.pmglab.suranyi.threadPool.ThreadPool;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @Data :2021/09/02
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :GTB 文件写入
 */

public class GTBWriter implements AutoCloseable {
    FileStream gtb;
    GTBWriterBuilder task;

    /**
     * IO 数据管道
     */
    DynamicPipeline<Boolean, UncompressedBlock> uncompressedPipLine;
    LinkedBlockingDeque<UncompressedBlock> compressedPipLine;

    /**
     * 位点控制器
     */
    final AlleleQC alleleQC;

    /**
     * 输出数据文件
     */
    final FileStream outputFile;

    /**
     * 编码器, 将文本基因型编码为字节编码
     */
    final BEGEncoder begEncoder;

    /**
     * 根数据管理器，压缩后的数据在此处进行校验
     */
    final FileBaseInfoManager baseInfoManager;
    final SmartList<GTBNode> GTBNodeCache = new SmartList<>(1024, true);
    int maxEstimateSize = 0;

    ThreadPool threadPool;

    /**
     * 文件基本信息
     */
    int validSubjectNum;
    int blockSize;

    /**
     * 标记当前状态
     */
    UncompressedBlock currentBlock;

    GTBWriter(GTBWriterBuilder buildTask) throws IOException {
        // 设置任务
        this.task = buildTask;

        // 加载 BEG 编码表和 MBEG 编码表
        this.begEncoder = BEGEncoder.getEncoder(task.isPhased());

        // 设置基本信息管理器
        this.baseInfoManager = FileBaseInfoManager.of(task);

        // 位点控制器
        this.alleleQC = task.getAlleleQC();

        // 设置输出数据文件
        this.outputFile = new FileStream(task.getOutputFileName(), FileOptions.CHANNEL_WRITER);

        // 开始进行工作
        startWork();
    }

    void startWork() throws IOException {
        // 样本名信息
        byte[] subjects = task.subjectManager.getSubjects();

        // 如果第一个模版变量没有数据，则填充
        this.validSubjectNum = task.subjectManager.getSubjectNum();

        // 验证块大小参数
        int blockSizeType = BlockSizeParameter.getSuggestBlockSizeType(task.getBlockSizeType(), validSubjectNum);
        this.blockSize = BlockSizeParameter.getBlockSize(blockSizeType);
        this.baseInfoManager.setBlockSizeType(blockSizeType);

        // 写入初始头信息
        this.outputFile.write(ValueUtils.value2ByteArray(0, 5));

        // 写入 refer 网址
        if (task.referenceManager != null) {
            this.outputFile.write(task.referenceManager.getReference());
        }

        // 写入换行符
        this.outputFile.write(ByteCode.NEWLINE);

        // 写入样本名
        VolumeByteStream subjectsSeq = ICompressor.compress(task.getCompressor(), task.getCompressionLevel(), subjects, 0, subjects.length);
        this.outputFile.writeIntegerValue(subjectsSeq.size());
        this.outputFile.write(subjectsSeq);

        // 创建线程池
        threadPool = new ThreadPool(this.task.getThreads());

        // 创建数据管道
        this.uncompressedPipLine = new DynamicPipeline<>(task.getThreads());
        this.compressedPipLine = new LinkedBlockingDeque<>(task.getThreads());

        for (int i = 0; i < task.getThreads(); i++) {
            UncompressedBlock initBlock = new UncompressedBlock(this.validSubjectNum, this.task, this.blockSize);
            this.compressedPipLine.add(initBlock);
        }

        threadPool.submit(() -> {
            try {
                processFileStream();
            } catch (Exception | Error e) {
                // e.printStackTrace();
                throw new UnsupportedOperationException(e.getMessage());
            }

        }, this.task.getThreads());
    }

    /**
     * 读取基因组文件
     */
    void processFileStream() {
        try {
            Block<Boolean, UncompressedBlock> taskBlock = this.uncompressedPipLine.get();

            // 确认为需要处理的任务块，只有需要这么一些线程的时候，才会创建容器
            if (taskBlock.getStatus()) {
                // 创建压缩上下文
                do {
                    // 完整地写入了一个块
                    if (!taskBlock.getData().empty()) {
                        processGTBBlock(taskBlock.getData().ctx, taskBlock.getData());
                    }

                    // 继续读取下一个文件任务
                    this.compressedPipLine.put(taskBlock.getData());
                    taskBlock = this.uncompressedPipLine.get();
                } while (taskBlock.getStatus());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // e.printStackTrace();
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

    /**
     * 写入一个位点
     */
    public boolean write(Variant info) throws InterruptedException {
        if (info.BEGs.length != validSubjectNum) {
            throw new GTBComponentException("inconsistent sample size");
        }

        if (currentBlock == null) {
            currentBlock = this.compressedPipLine.take();
        }

        if (this.alleleQC.size() != 0) {
            int alleleCounts = 0;
            int validAllelesNum = 0;
            byte code;
            for (int i = 0; i < info.BEGs.length; i++) {
                code = info.BEGs[i];

                if (code != 0) {
                    validAllelesNum += 1;

                    // 0 等位基因个数
                    alleleCounts += 2 - this.begEncoder.scoreOf(code);
                }
            }

            // 单倍型
            if (ChromosomeInfo.getPloidy(ChromosomeInfo.getIndex(info.chromosome)) == 1) {
                alleleCounts = alleleCounts >> 1;
            } else {
                // 二倍型
                validAllelesNum = validAllelesNum << 1;
            }

            if (this.alleleQC.filter(alleleCounts, validAllelesNum)) {
                return false;
            }
        }

        // 将这个位点写入缓冲
        int currentVariantChromosome = ChromosomeInfo.getIndex(info.chromosome);
        if (currentBlock.seek == 0) {
            currentBlock.chromosomeIndex = currentVariantChromosome;
        }

        if (currentVariantChromosome != currentBlock.chromosomeIndex) {
            this.uncompressedPipLine.put(true, currentBlock);
            currentBlock = this.compressedPipLine.take();
            currentBlock.chromosomeIndex = currentVariantChromosome;
        }

        VariantAbstract variant = currentBlock.getCurrentVariant();
        variant.position = info.position;
        VolumeByteStream alleles = new VolumeByteStream(info.ALT.length + 1 + info.REF.length);
        alleles.write(info.REF);
        alleles.write(ByteCode.TAB);
        alleles.write(info.ALT);
        variant.setAllele(alleles);
        variant.encoderIndex = info.getAlternativeAlleleNum() == 2 ? 0 : 1;
        System.arraycopy(info.BEGs, 0, currentBlock.encodedCache.getCache(), variant.encodedStart, validSubjectNum);
        currentBlock.seek++;

        if (currentBlock.remaining() == 0) {
            this.uncompressedPipLine.put(true, currentBlock);
            currentBlock = null;
        }

        return true;
    }

    @Override
    public void close() throws Exception {
        if (currentBlock != null && !currentBlock.empty()) {
            this.uncompressedPipLine.put(true, currentBlock);
        }

        // 发送关闭信号
        this.uncompressedPipLine.putStatus(this.task.getThreads(), false);

        // 关闭线程池，等待任务完成
        threadPool.close();

        // 回收内存
        for (int i = 0; i < this.compressedPipLine.size(); i++) {
            this.compressedPipLine.take().freeMemory();
        }

        // 清除数据区
        this.compressedPipLine.clear();
        this.uncompressedPipLine.clear();

        // 生成最后的 gtb 文件
        generateGTBFile();
    }
}