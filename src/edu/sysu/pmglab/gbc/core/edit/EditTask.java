package edu.sysu.pmglab.gbc.core.edit;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.check.ioexception.IOExceptionOptions;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.FileUtils;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.ITask;
import edu.sysu.pmglab.gbc.core.exception.GBCExceptionOptions;
import edu.sysu.pmglab.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.gbc.core.gtbcomponent.*;
import edu.sysu.pmglab.unifyIO.FileStream;
import edu.sysu.pmglab.unifyIO.options.FileOptions;

import java.io.IOException;
import java.util.*;

/**
 * @Author      :suranyi
 * @Description :编辑节点任务构造器
 */

public class EditTask implements ITask {
    private final GTBManager mainManager;
    private final GTBTree mainManagerTree;

    private String outputFileName;
    private final StringArray mergeFiles = new StringArray(10, true);

    private int rootIndex;
    private final Set<Operator> operators = new HashSet<>(6);
    private String newContigFile;

    /**
     * 构造器，原位改变模式
     * @param inputFileName 输入文件名
     */
    public EditTask(String inputFileName) {
        this(inputFileName, null);
    }

    /**
     * 构造器
     * @param inputFileName 输入文件名
     * @param outputFileName 输出文件名
     */
    public EditTask(String inputFileName, String outputFileName) {
        this.mainManager = GTBRootCache.get(inputFileName);
        this.mainManagerTree = this.mainManager.getGtbTree();
        this.outputFileName = outputFileName;
    }

    /**
     * 设置输出文件名
     */
    @Override
    public EditTask setOutputFileName(String outputFileName) {
        synchronized (this) {
            this.outputFileName = outputFileName;
        }
        return this;
    }

    /**
     * 编辑模式不支持多线程
     * @param threads 并行线程数
     */
    @Override
    public ITask setParallel(int threads) {
        return this;
    }

    @Override
    public int getThreads() {
        return 1;
    }

    /**
     * 获取输出文件名
     */
    @Override
    public String getOutputFileName() {
        return this.outputFileName;
    }

    /**
     * 删除染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosomes 删除的染色体编号
     */
    public EditTask delete(int... chromosomes) {
        Assert.NotNull(chromosomes);

        synchronized (this) {
            // 移除指定的染色体数据
            mainManagerTree.remove(chromosomes);

            // 添加操作
            operators.add(Operator.DELETE);
        }

        return this;
    }

    /**
     * 删除染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosomes 删除的染色体编号
     */
    public EditTask delete(String... chromosomes) {
        return delete(ChromosomeTags.getIndexes(chromosomes));
    }

    /**
     * 删除染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosomes 删除的染色体编号
     */
    public <ChromosomeType> EditTask delete(Collection<ChromosomeType> chromosomes) {
        return delete(ChromosomeTags.identifyChromosome(chromosomes));
    }

    /**
     * 删除染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosomeIndex 删除的染色体编号
     * @param nodeIndexes 该染色体编号的节点信息，若该值为 null，则删除全部信息
     */
    public EditTask delete(int chromosomeIndex, int[] nodeIndexes) {
        HashMap<Integer, int[]> chromosomeNodes = new HashMap<>(1);
        chromosomeNodes.put(chromosomeIndex, nodeIndexes);
        return delete(chromosomeNodes);
    }

    /**
     * 删除染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosome 删除的染色体编号 (字符串格式)
     * @param nodeIndexes 该染色体编号的节点信息，若该值为 null，则删除全部信息
     */
    public EditTask delete(String chromosome, int[] nodeIndexes) {
        return delete(ChromosomeTags.getIndex(chromosome), nodeIndexes);
    }

    /**
     * 删除染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosomeNodeList 删除的染色体编号及对应编号下移除的节点整数数组
     */
    public <ChromosomeType> EditTask delete(Map<ChromosomeType, int[]> chromosomeNodeList) {
        Assert.NotNull(chromosomeNodeList);

        synchronized (this) {
            // 移除指定的染色体数据
            mainManagerTree.remove(chromosomeNodeList);

            // 添加操作
            operators.add(Operator.DELETE);
        }

        return this;
    }

    /**
     * 保留染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosomes 保留的染色体编号
     */
    public EditTask retain(int... chromosomes) {
        Assert.NotNull(chromosomes);

        synchronized (this) {
            // 移除指定的染色体数据
            mainManagerTree.retain(chromosomes);

            // 添加操作
            operators.add(Operator.RETAIN);
        }

        return this;
    }

    /**
     * 保留染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosomes 保留的染色体编号
     */
    public EditTask retain(String... chromosomes) {
        return retain(ChromosomeTags.getIndexes(chromosomes));
    }

    /**
     * 保留染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosomes 保留的染色体编号
     */
    public <ChromosomeType> EditTask retain(Collection<ChromosomeType> chromosomes) {
        return retain(ChromosomeTags.identifyChromosome(chromosomes));
    }

    /**
     * 保留染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosomeIndex 保留的染色体编号
     * @param nodeIndexes 该染色体编号的节点信息，若该值为 null，则保留全部信息
     */
    public EditTask retain(int chromosomeIndex, int[] nodeIndexes) {
        HashMap<Integer, int[]> chromosomeNodes = new HashMap<>(1);
        chromosomeNodes.put(chromosomeIndex, nodeIndexes);
        return retain(chromosomeNodes);
    }

    /**
     * 保留染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosome 保留的染色体编号字符串
     * @param nodeIndexes 该染色体编号的节点信息，若该值为 null，则保留全部信息
     */
    public EditTask retain(String chromosome, int[] nodeIndexes) {
        return retain(ChromosomeTags.getIndex(chromosome), nodeIndexes);
    }

    /**
     * 保留染色体数据，delete、retain、merge 需要进行 submit 才会正式刷入操作
     * @param chromosomeNodeList 保留的染色体编号及对应编号下保留的节点整数数组
     */
    public <ChromosomeType> EditTask retain(Map<ChromosomeType, int[]> chromosomeNodeList) {
        Assert.NotNull(chromosomeNodeList);

        synchronized (this) {
            // 移除指定的染色体数据
            mainManagerTree.retain(chromosomeNodeList);

            // 添加操作
            operators.add(Operator.RETAIN);
        }

        return this;
    }

    /**
     * 节点去重，一般不会发生重复节点的情况
     */
    public EditTask unique() {
        synchronized (this) {
            mainManagerTree.flush(true);

            // 添加操作
            operators.add(Operator.UNIQUE);
        }

        return this;
    }

    /**
     * 分组重构
     */
    public EditTask split() {
        return split(true);
    }

    /**
     * 分组重构
     */
    public EditTask split(boolean isSplit) {
        synchronized (this) {
            // 添加操作
            if (isSplit) {
                operators.add(Operator.SPLIT);
            } else {
                operators.remove(Operator.SPLIT);
            }
        }

        return this;
    }

    /**
     * 合并多个 GTB 文件，这些文件必须具有相同的 phased, compressor, subjects 信息
     * @param otherInputFileNames 合并的源文件
     */
    public EditTask concat(String... otherInputFileNames) {
        return concat(new Array<>(otherInputFileNames));
    }

    /**
     * 合并多个 GTB 文件，这些文件必须具有相同的 phased, compressor, subjects 信息
     * @param otherManagers 合并的源管理器
     */
    public EditTask concat(GTBManager... otherManagers) {
        StringArray otherInputFileNames = new StringArray(otherManagers.length);
        for (GTBManager manager : otherManagers) {
            if (!manager.getFileName().equals(mainManager.getFileName())) {
                otherInputFileNames.add(manager.getFileName());
            }
        }

        return concat(otherInputFileNames);
    }

    /**
     * 合并多个 GTB 文件，这些文件必须具有相同的 phased, compressor, subjects 信息
     * @param otherUnknownTypeManagers 合并的源文件
     */
    public <ManagerType> EditTask concat(BaseArray<ManagerType> otherUnknownTypeManagers) {
        Assert.NotNull(otherUnknownTypeManagers);

        if (otherUnknownTypeManagers.size() > 0) {
            Array<String> managerFileNames = new Array<>(otherUnknownTypeManagers.size());
            for (ManagerType managerUnknownType : otherUnknownTypeManagers) {
                if (managerUnknownType instanceof String) {
                    managerFileNames.add((String) managerUnknownType);
                } else if (managerUnknownType instanceof GTBManager) {
                    managerFileNames.add(((GTBManager) managerUnknownType).getFileName());
                } else {
                    throw new GTBComponentException("unsupported node type: " + managerUnknownType.getClass());
                }
            }

            // 移除重估文件、已存在的文件
            managerFileNames.dropDuplicated();
            managerFileNames.remove(mainManager.getFileName());

            // 加载待合并的文件
            GTBManager[] fromsGtb = GTBRootCache.get(managerFileNames);

            // 校验 header 信息
            for (GTBManager gtbManager : fromsGtb) {
                Assert.that(mainManager.isPhased() == gtbManager.isPhased(), GBCExceptionOptions.GTBComponentException, "files with different `phased` cannot be concatenated");
                Assert.that(mainManager.getCompressorIndex() == gtbManager.getCompressorIndex(), GBCExceptionOptions.GTBComponentException, "files with different `compressor` cannot be concatenated");
                Assert.that((mainManager.getSubjectNum() == gtbManager.getSubjectNum()) && (ArrayUtils.equal(mainManager.getSubjects(), gtbManager.getSubjects())), GBCExceptionOptions.GTBComponentException, "files with different `subjects name` cannot be concatenated");
            }

            synchronized (this) {
                for (GTBManager otherManager : fromsGtb) {
                    if (otherManager != mainManager) {
                        // 绑定节点
                        otherManager.bind(++rootIndex);

                        // 将该节点树合并至主根
                        mainManagerTree.add(otherManager.getGtbTree());
                    }
                }

                /* 节点树合并完成，开始去重，得到最终完整的主根 */
                mainManagerTree.flush(true);
                mergeFiles.addAll(managerFileNames);

                // 添加操作
                operators.add(Operator.CONCAT);
            }
        }
        return this;
    }

    /**
     * 重设样本名
     */
    public EditTask resetSubjects(String... subjects) {
        Assert.NotNull(subjects);

        if (!((subjects.length == 0) && (this.mainManager.getSubjectNum() == 0))) {
            synchronized (this) {
                VolumeByteStream vbs = new VolumeByteStream(2 << 20);

                Assert.that((subjects.length > 0) && (subjects.length == mainManager.getSubjectNum()), GBCExceptionOptions.GTBComponentException, "The number of subjects must be equal to " + mainManager.getSubjectNum() + " to overwrite the subject of the original file.");

                vbs.write(subjects[0]);
                for (int i = 1; i < subjects.length; i++) {
                    vbs.writeSafety(ByteCode.TAB);
                    vbs.write(subjects[i]);
                }
                mainManager.getSubjectManager().load(vbs);

                this.operators.add(Operator.RESET_SUBJECT);
            }
        }

        return this;
    }

    /**
     * 提交编辑任务
     */
    public void submit() throws IOException {
        // 检查是否冲突
        String tempOutputFile = this.outputFileName;
        if (this.outputFileName == null) {
            this.outputFileName = autoGenerateOutputFileName();
        }

        if (operators.contains(Operator.SPLIT)) {
            if (!FileUtils.exists(this.outputFileName)) {
                // 不存在该文件夹时创建
                FileUtils.mkdirs(this.outputFileName);
            } else {
                throw new IOException("can't create output folder from " + this.outputFileName + " -- please rename");
            }
        } else {
            tempOutputFile = this.outputFileName + ".~$temp";
            Assert.that(!this.mainManager.getFileName().equals(tempOutputFile) && !this.mergeFiles.contains(tempOutputFile), IOExceptionOptions.FileAlreadyExistsException, "can't create a temporary file from " + this.outputFileName + " -- please rename");
        }

        synchronized (this) {
            // 需要编辑原文件
            if (operators.size() > 0) {
                // 创建新文件
                HashMap<Integer, FileStream> outs = new HashMap<>(24);
                HashMap<Integer, VolumeByteStream> headers = new HashMap<>(24);

                // 打开主文件
                FileStream in = mainManager.getFileStream();

                if (operators.contains(Operator.SPLIT)) {
                    for (int chromosomeIndex : mainManager.getChromosomeList()) {
                        GTBManager tempManager = new GTBManager(mainManager, chromosomeIndex);

                        outs.put(chromosomeIndex, new FileStream(this.outputFileName + "/chr" + ChromosomeTags.getString(chromosomeIndex) + ".gtb", FileOptions.CHANNEL_WRITER));
                        headers.put(chromosomeIndex, tempManager.getGtbTree().build());

                        // 写入头部信息
                        tempManager.checkOrderedGTB();
                        tempManager.checkSuggestToBGZF();
                        outs.get(chromosomeIndex).write(tempManager.buildHeader());
                    }
                } else {
                    FileStream out = new FileStream(tempOutputFile, FileOptions.CHANNEL_WRITER);
                    VolumeByteStream headerTemp = mainManagerTree.build();
                    for (int chromosomeIndex : mainManager.getChromosomeList()) {
                        outs.put(chromosomeIndex, out);
                        headers.put(chromosomeIndex, headerTemp);
                    }

                    // 写入头部信息
                    mainManager.checkOrderedGTB();
                    mainManager.checkSuggestToBGZF();
                    out.write(mainManager.buildHeader());
                }

                if (operators.contains(Operator.CONCAT)) {
                    // 多来源文件时
                    FileStream[] fromIns = new FileStream[mergeFiles.size()];
                    for (int i = 0; i < fromIns.length; i++) {
                        fromIns[i] = new FileStream(mergeFiles.get(i), FileOptions.CHANNEL_READER);
                    }

                    for (GTBNodes nodes : mainManagerTree) {
                        for (GTBNode node : nodes) {
                            if (node.getRootIndex() == 0) {
                                in.writeTo(node.blockSeek, node.blockSize, outs.get(node.chromosomeIndex).getChannel());
                            } else {
                                fromIns[node.getRootIndex() - 1].writeTo(node.blockSeek, node.blockSize, outs.get(node.chromosomeIndex).getChannel());
                            }
                        }
                    }

                    for (FileStream localFileStream : fromIns) {
                        localFileStream.close();
                    }
                } else {
                    for (GTBNodes nodes : mainManagerTree) {
                        for (GTBNode node : nodes) {
                            in.writeTo(node.blockSeek, node.blockSize, outs.get(node.chromosomeIndex).getChannel());
                        }
                    }
                }

                if (operators.contains(Operator.SPLIT)) {
                    for (int chromosomeIndex : mainManager.getChromosomeList()) {
                        outs.get(chromosomeIndex).write(headers.get(chromosomeIndex));
                    }
                } else {
                    if (mainManager.getChromosomeList().length > 0) {
                        int chromosomeIndex = mainManager.getChromosomeList()[0];
                        outs.get(chromosomeIndex).write(headers.get(chromosomeIndex));
                    }
                }

                // 关闭文件流
                for (FileStream out : outs.values()) {
                    if (!out.isClosed()) {
                        out.close();
                    }
                }

                in.close();

                if (!operators.contains(Operator.SPLIT)) {
                    // 以临时文件模式创建
                    outs.get(mainManager.getChromosomeList()[0]).rename(this.outputFileName);
                }
            }
        }

        GTBRootCache.clear(this.mainManager);
        GTBRootCache.clear(this.mergeFiles);
        this.mergeFiles.clear();
        this.rootIndex = 0;
    }


    @Override
    public String autoGenerateOutputFileName() {
        if (operators.contains(Operator.SPLIT)) {
            // 此时生成的文件是文件夹名
            return FileUtils.fixExtension(this.mainManager.getFileName(), "", ".vcf.gz.gtb", ".vcf.gtb", ".gz.gtb", ".gtb");
        } else {
            return this.outputFileName + ".~$temp";
        }
    }

    /**
     * 清除 Edit 任务信息
     */
    public void clear() {
        this.operators.clear();
        this.mergeFiles.clear();
        this.rootIndex = 0;
    }

    @Override
    public String toString() {
        return "EditTask{" +
                "\n\tinputFile: " + this.mainManager.getFileName() +
                "\n\toutputFile: " + ((this.outputFileName != null) ? this.outputFileName : "<modified in place>") +
                "\n\toperators: " + this.operators +
                "\n}";
    }
}
