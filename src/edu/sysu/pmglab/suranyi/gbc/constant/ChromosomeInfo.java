package edu.sysu.pmglab.suranyi.gbc.constant;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;
import edu.sysu.pmglab.suranyi.unifyIO.InputStreamReaderStream;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @Data        :2021/02/17
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :GTB 组件常量
 */

public enum ChromosomeInfo {
    /**
     * 单例模式
     */
    INSTANCE;

    public static final String DEFAULT_FILE = "human/hg38.p13";
    private String currentFile = "";
    private Chromosome[] chromosomes;
    private final Chromosome[] default_chromosomes;

    public final static int MIN_CHROMOSOME_INDEX = 0;
    public final static int MAX_CHROMOSOME_INDEX = 255;

    /**
     * 构造器，本压缩器支持的染色体列表
     */
    ChromosomeInfo() {
        try {
            this.chromosomes = load(DEFAULT_FILE, false);
        } catch (IOException ignored) {
        }

        this.default_chromosomes = new Chromosome[256];
        for (int i = 0; i < default_chromosomes.length; i++) {
            this.default_chromosomes[i] = new Chromosome(i, String.valueOf(i + 1), 2, -1);
        }
    }

    /**
     * 获取当前的文件名
     */
    public static String getCurrentFile() {
        return INSTANCE.currentFile;
    }

    /**
     * 从 vcf 文件中构建 contig 文件
     */
    public static void build(String inputFileName, String outputFileName) throws IOException {
        Assert.that(!inputFileName.equals(outputFileName), "inputFileName = outputFileName");

        FileStream in = new FileStream(inputFileName, inputFileName.endsWith(".gz") ? FileOptions.BGZIP_READER : FileOptions.CHANNEL_READER);
        FileStream out = new FileStream(outputFileName, FileOptions.CHANNEL_WRITER);

        VolumeByteStream outputCache = new VolumeByteStream();

        VolumeByteStream lineCache = new VolumeByteStream();
        boolean searchRef = true;
        byte[] contigStartFlag = "##contig=".getBytes();
        while (in.readLine(lineCache) != -1) {
            if (lineCache.startWith(ByteCode.NUMBER_SIGN)) {
                if (searchRef && lineCache.startWith(ByteCode.REFERENCE_STRING)) {
                    out.write(lineCache);
                    searchRef = false;
                } else {
                    if (lineCache.startWith(contigStartFlag)) {
                        // 捕获 contig 信息
                        byte[] chromosome = null;
                        byte[] length = null;

                        for (int i = contigStartFlag.length; i < lineCache.size(); i++) {
                            if (lineCache.startWith(i, ByteCode.ID_STRING)) {
                                if (lineCache.startWith(i + 3, ByteCode.CHR_STRING)) {
                                    i = i + 3;
                                }
                                for (int j = i + 3; j < lineCache.size(); j++) {
                                    if (lineCache.cacheOf(j) == ByteCode.COMMA || lineCache.cacheOf(j) == 0x3e) {
                                        chromosome = lineCache.cacheOf(i + 3, j);
                                        break;
                                    }
                                }
                            }

                            if (lineCache.startWith(i, "length=".getBytes())) {
                                for (int j = i + 7; j < lineCache.size(); j++) {
                                    if (lineCache.cacheOf(j) == ByteCode.COMMA || lineCache.cacheOf(j) == 0x3e) {
                                        length = lineCache.cacheOf(i + 7, j);
                                        break;
                                    }
                                }
                            }
                        }

                        if (chromosome != null) {
                            outputCache.writeSafety(ByteCode.NEWLINE);
                            outputCache.writeSafety(chromosome);

                            outputCache.writeSafety(ByteCode.COMMA);
                            outputCache.writeSafety(ByteCode.TWO);

                            outputCache.writeSafety(ByteCode.COMMA);
                            if (length != null) {
                                outputCache.writeSafety(length);
                            } else {
                                outputCache.writeSafety(ByteCode.ZERO);
                            }
                        }
                    }
                }
            } else {
                break;
            }
            lineCache.reset();
        }
        out.write("\n#chromosome,ploidy,length");
        out.write(outputCache);
        in.close();
        out.close();
    }

    public static Chromosome[] load(String resourceName) throws IOException {
        return INSTANCE.load(resourceName, true);
    }

    public Chromosome[] load(final String resourceName, boolean flush) throws IOException {
        if (resourceName == null || (this.currentFile.equals(resourceName))) {
            return null;
        }

        // 打开文件资源
        FileStream fileStream;
        InputStream innerResource = ChromosomeInfo.class.getResourceAsStream("/contig/" + resourceName);
        if (innerResource != null) {
            fileStream = new FileStream(new InputStreamReaderStream(innerResource));
        } else {
            fileStream = new FileStream(resourceName);
        }

        VolumeByteStream lineCache = new VolumeByteStream(128);
        fileStream.readLine(lineCache);

        // 解析注释行
        String reference = null;
        while ((lineCache.cacheOf(0) == ByteCode.NUMBER_SIGN) && (lineCache.cacheOf(1) == ByteCode.NUMBER_SIGN)) {
            if (lineCache.startWith(ByteCode.REFERENCE_STRING)) {
                reference = new String(lineCache.cacheOf(ByteCode.REFERENCE_STRING.length, lineCache.size()));
            }

            lineCache.reset();
            fileStream.readLine(lineCache);
        }

        // 解析正文字段
        int count = 0;
        ArrayList<Chromosome> chromosomes = new ArrayList<>(32);
        String[] fields = new String(lineCache.cacheOf(1, lineCache.size())).split(",");
        int chromosomeIndex = ArrayUtils.indexOf(fields, "chromosome");
        int ploidyIndex = ArrayUtils.indexOf(fields, "ploidy");
        int lengthIndex = ArrayUtils.indexOf(fields, "length");

        Assert.that(chromosomeIndex != -1, "doesn't match to standard Chromosome Config file.");

        lineCache.reset();
        while (fileStream.readLine(lineCache) != -1) {
            String[] groups = new String(lineCache.values()).split(",");
            chromosomes.add(new Chromosome(count, groups[chromosomeIndex], ploidyIndex == -1 ? 2 : ValueUtils.matchInteger(groups[ploidyIndex]), ploidyIndex == -1 ? 2 : lengthIndex == -1 ? -1 : Integer.parseInt(groups[lengthIndex]), reference));
            count++;
            lineCache.reset();
        }


        fileStream.close();
        if (flush) {
            this.chromosomes = chromosomes.toArray(new Chromosome[0]);
            this.currentFile = resourceName;
        }
        return chromosomes.toArray(new Chromosome[0]);
    }

    /**
     * 根据染色体的字符串值获取对应的染色体信息
     * @param chromosomeString 染色体字符串
     */
    public static Chromosome get(String chromosomeString) {
        for (Chromosome chromosome : INSTANCE.chromosomes) {
            if (chromosome.chromosomeString.equals(chromosomeString)) {
                return chromosome;
            }
        }

        throw new UnsupportedOperationException("unable to identify chromosome=" + chromosomeString + " (supported: " + Arrays.toString(ChromosomeInfo.supportedChromosomeList()) + ")");
    }

    /**
     * 根据染色体的索引值获取对应的染色体信息
     * @param chromosomeIndex 染色体索引值
     */
    public static Chromosome get(int chromosomeIndex) {
        for (Chromosome chromosomeObject : INSTANCE.chromosomes) {
            if (chromosomeObject.chromosomeIndex == chromosomeIndex) {
                return chromosomeObject;
            }
        }

        // 没有找到，说明 contig 文件不匹配
        return INSTANCE.default_chromosomes[chromosomeIndex];
    }

    /**
     * 传入一个未知类型的染色体标记数据
     * @param chromosomeUnknownType 未知类型的染色体数据
     * @param <ChromosomeType> 染色体范型
     * @return 识别出来的染色体信息
     */
    public static <ChromosomeType> Chromosome get(ChromosomeType chromosomeUnknownType) {
        if (chromosomeUnknownType instanceof String) {
            return get((String) chromosomeUnknownType);
        } else if (chromosomeUnknownType instanceof Integer) {
            int chromosomeIndex = (Integer) chromosomeUnknownType;
            return get(chromosomeIndex);
        }

        throw new GTBComponentException("unsupported chromosome type: " + chromosomeUnknownType.getClass());
    }

    /**
     * 根据染色体的字符串字节数组获取对应的染色体信息，一般用于校验变异位点信息
     * @param variant 变异位点
     * @param offset 偏移量
     * @param length 长度
     */
    public static Chromosome get(VolumeByteStream variant, int offset, int length) {
        for (Chromosome chromosome : INSTANCE.chromosomes) {
            if (variant.equal(offset, length, chromosome.chromosomeByteArray, 0, chromosome.chromosomeByteArray.length)) {
                return chromosome;
            }
        }

        return null;
    }

    /**
     * 根据本类中定义的染色体信息类顺序，获取对应的索引值
     * @param chromosomeIndex 染色体索引值
     */
    public static byte[] getBytes(int chromosomeIndex) {
        if (chromosomeIndex < INSTANCE.chromosomes.length) {
            return INSTANCE.chromosomes[chromosomeIndex].chromosomeByteArray;
        } else {
            return INSTANCE.default_chromosomes[chromosomeIndex].chromosomeByteArray;
        }
    }

    /**
     * 根据本类中定义的染色体信息类顺序，获取对应的字符串值
     * @param chromosomeIndex 染色体索引值
     */
    public static String getString(int chromosomeIndex) {
        if (chromosomeIndex < INSTANCE.chromosomes.length) {
            return INSTANCE.chromosomes[chromosomeIndex].chromosomeString;
        } else {
            return INSTANCE.default_chromosomes[chromosomeIndex].chromosomeString;
        }
    }

    /**
     * 根据本类中定义的染色体信息类顺序，获取对应的contig长度
     * @param chromosomeIndex 染色体索引值
     */
    public static int getContigLength(int chromosomeIndex) {
        if (chromosomeIndex < INSTANCE.chromosomes.length) {
            return INSTANCE.chromosomes[chromosomeIndex].length;
        } else {
            return INSTANCE.default_chromosomes[chromosomeIndex].length;
        }
    }

    /**
     * 根据本类中定义的染色体信息类顺序，获取对应的倍体
     * @param chromosomeIndex 染色体索引值
     */
    public static int getPloidy(int chromosomeIndex) {
        if (chromosomeIndex < INSTANCE.chromosomes.length) {
            return INSTANCE.chromosomes[chromosomeIndex].ploidy;
        } else {
            return INSTANCE.default_chromosomes[chromosomeIndex].ploidy;
        }
    }

    /**
     * 获取染色体索引对应的 header 信息
     * @param chromosomeIndex 染色体索引信息
     */
    public static String getHeader(int chromosomeIndex) {
        String header = "\n##contig=<ID=" + ChromosomeInfo.getString(chromosomeIndex);

        if (ChromosomeInfo.getContigLength(chromosomeIndex) > 0) {
            header += ",length=" + ChromosomeInfo.getContigLength(chromosomeIndex);
        }

        if (ChromosomeInfo.getRef(chromosomeIndex) != null) {
            header += ",URL=" + ChromosomeInfo.getRef(chromosomeIndex);
        }

        header += ">";

        return header;
    }

    /**
     * 获取该参考序列的地址
     * @param chromosomeIndex 染色体索引
     * @return 对应的参考系
     */
    public static String getRef(int chromosomeIndex) {
        if (chromosomeIndex < INSTANCE.chromosomes.length) {
            return INSTANCE.chromosomes[chromosomeIndex].reference;
        } else {
            return INSTANCE.default_chromosomes[chromosomeIndex].reference;
        }
    }

    /**
     * 获取本类支持全部的染色体字符串
     */
    public static String[] supportedChromosomeList() {
        String[] list = new String[INSTANCE.chromosomes.length];
        for (int i = 0; i < INSTANCE.chromosomes.length; i++) {
            list[i] = INSTANCE.chromosomes[i].chromosomeString;
        }

        return list;
    }

    /**
     * 将字符串形式的染色体数据批量生成索引值
     * @return 识别出来的染色体索引值
     */
    public static int[] getIndexes(String... chromosomes) {
        int[] chromosomeIndexes = new int[chromosomes.length];

        for (int i = 0; i < chromosomes.length; i++) {
            chromosomeIndexes[i] = ChromosomeInfo.getIndex(chromosomes[i]);
        }

        return chromosomeIndexes;
    }

    /**
     * 将字符串形式的染色体数据批量生成索引值
     * @param chromosomeString 获取染色体编号
     * @return 识别出来的染色体索引值
     */
    public static int getIndex(String chromosomeString) {
        return get(chromosomeString).chromosomeIndex;
    }

    /**
     * 识别染色体编号，并转换为 HashMap 格式
     * @param origin 原始数据
     * @param <ChromosomeType> 染色体类型
     * @param <V> 值类型
     * @return 将 chromosome 格式化为 索引形式
     */
    public static <ChromosomeType, V> HashMap<Integer, V> identifyChromosome(Map<ChromosomeType, V> origin) {
        HashMap<Integer, V> target = new HashMap<>(origin.size());

        for (ChromosomeType chromosomeUnknownType : origin.keySet()) {
            target.put(get(chromosomeUnknownType).chromosomeIndex, origin.get(chromosomeUnknownType));
        }

        return target;
    }

    /**
     * 识别染色体编号，并转换为 int[ ] 格式
     * @param origin 原始数据
     * @param <ChromosomeType> 染色体类型
     * @return 将 chromosome 格式化为 索引形式
     */
    public static <ChromosomeType> int[] identifyChromosome(Collection<ChromosomeType> origin) {
        SmartList<Integer> chromosomeIndexes = new SmartList<>(origin.size());

        for (ChromosomeType chromosomeUnknownType : origin) {
            chromosomeIndexes.add(get(chromosomeUnknownType).chromosomeIndex);
        }

        int[] out = new int[chromosomeIndexes.size()];
        for (int i = 0; i < chromosomeIndexes.size(); i++) {
            out[i] = chromosomeIndexes.get(i);
        }

        return out;
    }
}
