package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.gbc.coder.BEGTransfer;
import edu.sysu.pmglab.suranyi.gbc.coder.CoderConfig;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.calculation.ld.ILDModel;
import edu.sysu.pmglab.suranyi.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.formatter.*;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author suranyi
 */

public class Variant {
    /**
     * GTBReader 读取到的位点对象
     */
    public String chromosome;
    public int position;
    public int ploidy;
    public byte[] REF;
    public byte[] ALT;
    public byte[] BEGs;
    public boolean phased;
    public Object property;

    public Variant() {
        BEGs = new byte[0];
    }

    public Variant(Variant variant) {
        this.chromosome = variant.chromosome;
        this.position = variant.position;
        this.ploidy = variant.ploidy;
        this.REF = ArrayUtils.copyOfRange(variant.REF, 0, variant.REF.length);
        this.ALT = ArrayUtils.copyOfRange(variant.ALT, 0, variant.ALT.length);
        this.BEGs = ArrayUtils.copyOfRange(variant.BEGs, 0, variant.BEGs.length);
        this.phased = variant.phased;
        this.property = variant.property;
    }

    /**
     * 将多个二等位基因/多等位基因位点合并为单个位点
     *
     * @param variants 等位基因列表
     */
    public static Variant join(SmartList<Variant> variants) {
        Assert.NotEmpty(variants);

        if (variants.size() == 1) {
            return new Variant(variants.get(0));
        }

        Variant targetVariant = new Variant();
        Variant firstVariant = variants.get(0);

        targetVariant.chromosome = firstVariant.chromosome;
        targetVariant.position = firstVariant.position;
        targetVariant.BEGs = new byte[firstVariant.BEGs.length];
        targetVariant.REF = firstVariant.REF;
        targetVariant.ploidy = firstVariant.ploidy;
        targetVariant.phased = firstVariant.phased;

        // 转换编码表
        int[][] transCode = new int[variants.size()][];
        SmartList<byte[]> newALT = new SmartList<>();
        newALT.add(targetVariant.REF);

        for (int i = 0; i < variants.size(); i++) {
            Variant variant = variants.get(i);
            if (!variant.chromosome.equals(targetVariant.chromosome) || variant.position != targetVariant.position) {
                throw new GTBComponentException("variants with different coordinates cannot be combined into a single multi-allelic variant");
            }

            if (variant.BEGs.length != targetVariant.BEGs.length) {
                throw new GTBComponentException("variants with different sample sizes cannot be combined into a single multi-allelic variant");
            }

            if (variant.ploidy != targetVariant.ploidy) {
                throw new GTBComponentException("variants with different ploidy cannot be combined into a single multi-allelic variant");
            }

            targetVariant.phased = targetVariant.phased || variant.phased;
            int[] ACs = variant.getACs();
            transCode[i] = new int[ACs.length];

            out:
            for (int j = 0; j < ACs.length; j++) {
                if (ACs[j] > 0) {
                    byte[] allelej = variant.getAllele(j);
                    for (int k = 0; k < newALT.size(); k++) {
                        if (Arrays.equals(newALT.get(k), allelej)) {
                            transCode[i][j] = k;
                            continue out;
                        }
                    }

                    // 没有找到，说明是新的 alt
                    newALT.add(allelej);
                    transCode[i][j] = (byte) (newALT.size() - 1);
                }
            }
        }

        if (newALT.size() > CoderConfig.MAX_ALLELE_NUM) {
            throw new GTBComponentException("variant contains too many alternative alleles (> " + (CoderConfig.MAX_ALLELE_NUM) + ")");
        }

        // 经过检验, 开始合并位点
        int leftGenotype;
        int rightGenotype;

        BEGEncoder encoder = BEGEncoder.getEncoder(targetVariant.phased);

        if (targetVariant.phased) {
            // phased 时
            for (int i = 0; i < targetVariant.BEGs.length; i++) {
                leftGenotype = -1;
                rightGenotype = -1;

                for (int j = 0; j < variants.size(); j++) {
                    Variant variant = variants.get(j);

                    if (variant.BEGs[i] != 0) {
                        if (leftGenotype == -1 || leftGenotype == 0) {
                            // 还没设置该基因型
                            leftGenotype = transCode[j][BEGDecoder.decodeHaplotype(0, variant.BEGs[i])];
                        }

                        if (rightGenotype == -1 || rightGenotype == 0) {
                            // 还没设置该基因型
                            rightGenotype = transCode[j][BEGDecoder.decodeHaplotype(1, variant.BEGs[i])];
                        }
                    }
                }

                if (leftGenotype == -1 && rightGenotype == -1) {
                    targetVariant.BEGs[i] = encoder.encodeMiss();
                } else {
                    targetVariant.BEGs[i] = encoder.encode(leftGenotype, rightGenotype);
                }
            }
        } else {
            // unphased 时
            for (int i = 0; i < targetVariant.BEGs.length; i++) {
                leftGenotype = -1;
                rightGenotype = -1;

                for (int j = 0; j < variants.size(); j++) {
                    Variant variant = variants.get(j);

                    if (variant.BEGs[i] != 0) {
                        if (leftGenotype == -1 || leftGenotype == 0) {
                            // 还没设置该基因型
                            leftGenotype = transCode[j][BEGDecoder.decodeHaplotype(0, variant.BEGs[i])];

                            if (leftGenotype == 0) {
                                leftGenotype = transCode[j][BEGDecoder.decodeHaplotype(1, variant.BEGs[i])];

                                if (rightGenotype == -1) {
                                    rightGenotype = 0;
                                }
                            } else {
                                rightGenotype = transCode[j][BEGDecoder.decodeHaplotype(1, variant.BEGs[i])];
                            }
                        } else if (rightGenotype == 0) {
                            // 还没设置该基因型
                            rightGenotype = transCode[j][BEGDecoder.decodeHaplotype(0, variant.BEGs[i])];

                            if (rightGenotype == 0) {
                                rightGenotype = transCode[j][BEGDecoder.decodeHaplotype(1, variant.BEGs[i])];
                            }
                        }
                    }
                }

                if (leftGenotype == -1 && rightGenotype == -1) {
                    targetVariant.BEGs[i] = encoder.encodeMiss();
                } else {
                    targetVariant.BEGs[i] = encoder.encode(leftGenotype, rightGenotype);
                }
            }
        }

        VolumeByteStream newALTCache = new VolumeByteStream();
        newALT.popFirst();
        for (byte[] allele : newALT) {
            newALTCache.writeSafety(allele);
            newALTCache.writeSafety(ByteCode.COMMA);
        }

        if (newALTCache.size() == 0) {
            newALTCache.write(ByteCode.PERIOD);
            newALTCache.write(ByteCode.COMMA);
        }

        targetVariant.ALT = newALTCache.rangeOf(0, newALTCache.size() - 1);
        return targetVariant;
    }

    /**
     * 将多个二等位基因/多等位基因位点合并为单个位点
     *
     * @param variants 等位基因列表
     */
    public static Variant join(Variant... variants) {
        return join(new SmartList<>(variants));
    }

    public Variant(int chromosomeIndex, int position, String REF, String ALT, byte[] BEGs) {
        this(chromosomeIndex, position, REF.getBytes(), ALT.getBytes(), BEGs);
    }

    public Variant(int chromosomeIndex, int position, byte[] REF, byte[] ALT, byte[] BEGs) {
        this(chromosomeIndex, position, REF, ALT, BEGs, true);
    }

    public Variant(int chromosomeIndex, int position, String REF, String ALT, byte[] BEGs, boolean phased) {
        this(chromosomeIndex, position, REF.getBytes(), ALT.getBytes(), BEGs, phased);
    }

    public Variant(int chromosomeIndex, int position, byte[] Allele, byte[] BEGs) {
        this(chromosomeIndex, position, Allele, BEGs, true);
    }

    public Variant(String chromosome, int position, byte[] Allele, byte[] BEGs) {
        this(chromosome, position, Allele, BEGs, true);
    }

    public Variant(int chromosomeIndex, int position, byte[] Allele, byte[] BEGs, boolean phased) {
        this(chromosomeIndex, position, Allele, Allele, BEGs, phased);
        int sepIndex = ArrayUtils.indexOf(Allele, ByteCode.TAB);
        this.REF = ArrayUtils.copyOfRange(Allele, sepIndex);
        this.ALT = ArrayUtils.copyOfRange(Allele, sepIndex + 1, Allele.length);
    }

    public Variant(String chromosome, int position, byte[] Allele, byte[] BEGs, boolean phased) {
        this(chromosome, position, Allele, Allele, BEGs, phased);
        int sepIndex = ArrayUtils.indexOf(Allele, ByteCode.TAB);
        this.REF = ArrayUtils.copyOfRange(Allele, sepIndex);
        this.ALT = ArrayUtils.copyOfRange(Allele, sepIndex + 1, Allele.length);
    }

    public Variant(int chromosomeIndex, int position, byte[] REF, byte[] ALT, byte[] BEGs, boolean phased) {
        this(ChromosomeInfo.getString(chromosomeIndex), position, REF, ALT, BEGs, phased);
    }

    public Variant(String chromosome, int position, byte[] REF, byte[] ALT, byte[] BEGs, boolean phased) {
        this.chromosome = chromosome;
        this.position = position;
        this.ploidy = ChromosomeInfo.get(chromosome).ploidy;
        this.REF = REF;
        this.ALT = ALT;
        this.BEGs = BEGs;
        this.phased = phased;
    }

    /**
     * 获取可替代等位基因的个数
     *
     * @return 可替代等位基因个数
     */
    public int getAlternativeAlleleNum() {
        return ArrayUtils.valueCounts(ALT, ByteCode.COMMA) + 2;
    }

    /**
     * 获取 AC 值, 该值已通过倍型校正
     *
     * @return 获取 AC 值
     */
    public int getAC() {
        return apply(ACValueFormatter.INSTANCE);
    }

    /**
     * 获取 AC 值, 该值已通过倍型校正
     *
     * @return 获取 AC 值
     */
    public int[] getACs() {
        return apply(ACsValueFormatter.INSTANCE);
    }

    /**
     * 获取 AN 值, 该值已通过倍型校正
     *
     * @return 获取 AN 值
     */
    public int getAN() {
        return apply(ANValueFormatter.INSTANCE);
    }

    /**
     * 获取 基因型计数值
     */
    public int[] getGenotypeCounts() {
        return apply(GenotypeCountsFormatter.INSTANCE);
    }

    /**
     * 获取缺失样本的个数
     *
     * @return 缺失样本个数
     */
    public int getMissSubjectNum() {
        return apply(MissSubjectNumValueFormatter.INSTANCE);
    }

    /**
     * 获取 AF
     *
     * @return AF 值，等位基因频率
     */
    public double getAF() {
        return apply(AFValueFormatter.INSTANCE);
    }

    /**
     * 获取所有等位基因形式的频率值
     *
     * @return AFs 值，等位基因频率
     */
    public double[] getAFs() {
        return apply(AFsValueFormatter.INSTANCE);
    }

    /**
     * 获取 MAF
     *
     * @return MAF 值，次等位基因频率
     */
    public double getMAF() {
        return apply(MAFValueFormatter.INSTANCE);
    }

    /**
     * 是否有缺失基因型
     */
    public boolean hasMissGenotype() {
        return apply(HasMissGTFormatter.INSTANCE);
    }

    /**
     * 获取位点的 BEG 编码
     */
    public int getBEGCode(int index) {
        return BEGs[index] & 0xFF;
    }

    /**
     * 转为位编码
     */
    public int[] toBitCode(byte[] bitCodeTable) {
        return apply(ToBitCodeFormatter.INSTANCE, bitCodeTable);
    }

    /**
     * 获取位点的 BEG 编码
     *
     * @param index          编码索引
     * @param haplotypeIndex 单倍型索引 (1 / 2)
     */
    public int getGenotypeCode(int index, int haplotypeIndex) {
        return BEGDecoder.decodeHaplotype(haplotypeIndex, BEGs[index]);
    }

    /**
     * 按照新的等位基因信息转换编码
     */
    public void resetAlleles(String newREF, String newALT) {
        resetAlleles(newREF.getBytes(), newALT.getBytes());
    }

    /**
     * 按照新的等位基因信息转换编码
     */
    public void resetAlleles(String newREF) {
        resetAlleles(newREF.getBytes());
    }

    /**
     * 按照新的等位基因信息转换编码
     */
    public void resetAlleles(byte[] newREF, byte[] newALT) {
        if ((ArrayUtils.equal(REF, newREF) && ArrayUtils.startWiths(newALT, ALT) && ((newALT.length == ALT.length) || (newALT[ALT.length] == ByteCode.COMMA)))) {
            ALT = newALT;
        } else {
            // 获取等位基因个数
            int allelesNum = getAlternativeAlleleNum();
            if ((allelesNum == 2) && ArrayUtils.equal(ALT, newREF) && (ArrayUtils.startWiths(newALT, REF) && ((newALT.length == REF.length) || (newALT[REF.length] == ByteCode.COMMA)))) {
                // 二等位基因，并且只需要翻转编码表
                for (int i = 0; i < this.BEGs.length; i++) {
                    BEGs[i] = BEGTransfer.reverse(this.phased, BEGs[i]);
                }

                REF = newREF;
                ALT = newALT;
            } else {
                // 此时处理的情况复杂的多，可能会变成多等位基因位点
                SmartList<byte[]> newALTCache = new SmartList<>();
                int[] ACs = this.getACs();
                int[] transCode = new int[ACs.length];
                newALTCache.add(newREF);

                // 虚拟位点
                Variant fakeVariant = new Variant();
                fakeVariant.REF = newREF;
                fakeVariant.ALT = newALT;

                out:
                for (int i = 0; i < fakeVariant.getAlternativeAlleleNum(); i++) {
                    byte[] allelej = fakeVariant.getAllele(i);
                    for (int j = 0; j < newALTCache.size(); j++) {
                        if (Arrays.equals(newALTCache.get(j), allelej)) {
                            continue out;
                        }
                    }

                    // 没有找到，说明是新的 alt
                    newALTCache.add(allelej);
                }

                // 扫描 allele
                out:
                for (int i = 0; i < ACs.length; i++) {
                    if (ACs[i] > 0) {
                        byte[] allelej = getAllele(i);
                        for (int j = 0; j < newALTCache.size(); j++) {
                            if (Arrays.equals(newALTCache.get(j), allelej)) {
                                transCode[i] = j;
                                continue out;
                            }
                        }

                        // 没有找到，说明是新的 alt
                        newALTCache.add(allelej);
                        transCode[i] = (byte) (newALTCache.size() - 1);
                    }
                }

                if (newALTCache.size() > CoderConfig.MAX_ALLELE_NUM) {
                    throw new GTBComponentException("variant contains too many alternative alleles (> " + (CoderConfig.MAX_ALLELE_NUM) + ")");
                }

                // 经过检验, 开始合并位点
                VolumeByteStream cache = new VolumeByteStream();
                newALTCache.popFirst();
                for (byte[] allele : newALTCache) {
                    cache.writeSafety(allele);
                    cache.writeSafety(ByteCode.COMMA);
                }

                if (newALTCache.size() == 0) {
                    cache.write(ByteCode.PERIOD);
                    cache.write(ByteCode.COMMA);
                }

                this.REF = newREF;
                this.ALT = cache.rangeOf(0, cache.size() - 1);

                // 转换编码
                boolean fastMode = true;
                for (int i = 0; i < ACs.length; i++) {
                    if (ACs[i] != i) {
                        // 如果映射关系没有改变, 则为快速模式
                        fastMode = false;
                        break;
                    }
                }

                if (!fastMode) {
                    BEGEncoder encoder = BEGEncoder.getEncoder(this.phased);
                    for (int i = 0; i < this.BEGs.length; i++) {
                        BEGs[i] = BEGs[i] == 0 ? 0 : encoder.encode(transCode[BEGDecoder.decodeHaplotype(0, BEGs[i])], transCode[BEGDecoder.decodeHaplotype(1, BEGs[i])]);
                    }
                }
            }
        }
    }

    /**
     * 按照新的等位基因信息转换编码
     */
    public void resetAlleles(byte[] newREF) {
        if (!newREF.equals(this.REF)) {
            resetAlleles(newREF, this.REF);
        }
    }

    /**
     * 横向合并位点 (认为它们来自不同的测序样本)
     *
     * @param otherVariant 另一个变异位点
     */
    public Variant merge(Variant otherVariant) {
        return merge(otherVariant, true);
    }

    /**
     * 横向合并位点 (认为它们来自不同的测序样本)
     *
     * @param otherVariant 另一个变异位点
     */
    public Variant merge(Variant otherVariant, Variant target) {
        return merge(otherVariant, target, true);
    }

    /**
     * 横向合并位点 (认为它们来自不同的测序样本)
     *
     * @param otherVariant     另一个变异位点
     * @param verifyCoordinate 是否验证坐标
     */
    public Variant merge(final Variant otherVariant, boolean verifyCoordinate) {
        return merge(otherVariant, new Variant(), verifyCoordinate);
    }

    /**
     * 横向合并位点 (认为它们来自不同的测序样本)
     *
     * @param otherVariant     另一个变异位点
     * @param target           填充数据到指定的位点
     * @param verifyCoordinate 是否验证坐标
     */
    public Variant merge(final Variant otherVariant, Variant target, boolean verifyCoordinate) {
        if (verifyCoordinate && !(otherVariant.chromosome.equals(this.chromosome) && (otherVariant.position == this.position))) {
            throw new UnsupportedOperationException("merge variant with different coordinates are not allowed");
        }

        target.chromosome = this.chromosome;
        target.position = this.position;
        target.ploidy = this.ploidy;
        target.phased = this.phased;

        // 传入位点作为主位点
        if ((ArrayUtils.equal(REF, otherVariant.REF) && (ArrayUtils.equal(ALT, otherVariant.ALT)))) {
            target.REF = this.REF;
            target.ALT = this.ALT;
            if (target.BEGs.length == this.BEGs.length + otherVariant.BEGs.length) {
                System.arraycopy(this.BEGs, 0, target.BEGs, 0, this.BEGs.length);
                System.arraycopy(otherVariant.BEGs, 0, target.BEGs, this.BEGs.length, otherVariant.BEGs.length);
            } else {
                target.BEGs = ArrayUtils.merge(this.BEGs, otherVariant.BEGs);
            }
        } else {
            // 此时处理的情况复杂的多，会变成多等位基因位点
            SmartList<byte[]> newALT = new SmartList<>();
            int[] ACs1 = this.getACs();
            int[] ACs2 = otherVariant.getACs();
            int[][] transCode = new int[][]{new int[ACs1.length], new int[ACs2.length]};
            newALT.add(this.REF);

            // 扫描 allele
            out:
            for (int i = 1; i < ACs1.length; i++) {
                if (ACs1[i] > 0) {
                    byte[] allelej = getAllele(i);
                    for (int j = 0; j < newALT.size(); j++) {
                        if (Arrays.equals(newALT.get(j), allelej)) {
                            transCode[0][i] = j;
                            continue out;
                        }
                    }

                    // 没有找到，说明是新的 alt
                    newALT.add(allelej);
                    transCode[0][i] = (byte) (newALT.size() - 1);
                }
            }

            out:
            for (int i = 0; i < ACs2.length; i++) {
                if (ACs2[i] > 0) {
                    byte[] allelej = otherVariant.getAllele(i);
                    for (int j = 0; j < newALT.size(); j++) {
                        if (Arrays.equals(newALT.get(j), allelej)) {
                            transCode[1][i] = j;
                            continue out;
                        }
                    }

                    // 没有找到，说明是新的 alt
                    newALT.add(allelej);
                    transCode[1][i] = (byte) (newALT.size() - 1);
                }
            }

            if (newALT.size() > CoderConfig.MAX_ALLELE_NUM) {
                throw new GTBComponentException("variant contains too many alternative alleles (> " + (CoderConfig.MAX_ALLELE_NUM) + ")");
            }

            // 经过检验, 开始合并位点
            VolumeByteStream newALTCache = new VolumeByteStream();
            newALT.popFirst();
            for (byte[] allele : newALT) {
                newALTCache.writeSafety(allele);
                newALTCache.writeSafety(ByteCode.COMMA);
            }

            if (newALTCache.size() == 0) {
                newALTCache.write(ByteCode.PERIOD);
                newALTCache.write(ByteCode.COMMA);
            }

            target.REF = this.REF;
            target.ALT = newALTCache.rangeOf(0, newALTCache.size() - 1);

            // 转换编码
            BEGEncoder encoder = BEGEncoder.getEncoder(this.phased);
            if (target.BEGs.length != this.BEGs.length + otherVariant.BEGs.length) {
                target.BEGs = new byte[this.BEGs.length + otherVariant.BEGs.length];
            }

            // 尽可能不处理 variant 1 (直接拷贝)
            int beginIndex = ArrayUtils.indexOf(ACs1, 0, 1);
            boolean fastMode = true;

            if (beginIndex != -1) {
                // 所有的碱基都有计数值
                for (int i = beginIndex + 1; i < ACs1.length; i++) {
                    // 在 beginIndex 之后的全都是 0
                    if (ACs1[i] != 0) {
                        fastMode = false;
                        break;
                    }
                }
            }

            if (fastMode) {
                System.arraycopy(this.BEGs, 0, target.BEGs, 0, this.BEGs.length);
            } else {
                for (int i = 0; i < this.BEGs.length; i++) {
                    target.BEGs[i] = this.BEGs[i] == 0 ? 0 : encoder.encode(transCode[0][BEGDecoder.decodeHaplotype(0, this.BEGs[i])], transCode[0][BEGDecoder.decodeHaplotype(1, this.BEGs[i])]);
                }
            }

            for (int i = 0; i < otherVariant.BEGs.length; i++) {
                target.BEGs[this.BEGs.length + i] = otherVariant.BEGs[i] == 0 ? 0 : encoder.encode(transCode[1][BEGDecoder.decodeHaplotype(0, otherVariant.BEGs[i])], transCode[1][BEGDecoder.decodeHaplotype(1, otherVariant.BEGs[i])]);
            }
        }
        return target;
    }

    /**
     * 转为 VCF 格式
     */
    public byte[] toVCF() {
        return toVCF(false);
    }

    /**
     * 转为 VCF 格式
     *
     * @param cache 输出缓冲区
     */
    public int toVCF(VolumeByteStream cache) {
        return toVCF(false, cache);
    }

    /**
     * 转为 VCF 位点数据 (非基因型数据)
     */
    public byte[] toVCFSite() {
        return apply(VCFSiteVariantFormatter.INSTANCE);
    }

    /**
     * 转为 VCF 位点数据 (非基因型数据)
     *
     * @param cache 输出缓冲区
     */
    public int toVCFSite(VolumeByteStream cache) {
        return apply(VCFSiteVariantFormatter.INSTANCE, cache);
    }

    /**
     * 转为 VCF 格式
     *
     * @param info 是否写入 INFO 信息 (AC, AN, AF)
     */
    public byte[] toVCF(boolean info) {
        if (info) {
            return apply(VCFVariantFormatter.INSTANCE);
        } else {
            return apply(EasyVCFVariantFormatter.INSTANCE);
        }
    }

    /**
     * 转为 VCF 格式
     *
     * @param info 是否写入 INFO 信息 (AC, AN, AF)
     */
    public int toVCF(boolean info, VolumeByteStream cache) {
        if (info) {
            return apply(VCFVariantFormatter.INSTANCE, cache);
        } else {
            return apply(EasyVCFVariantFormatter.INSTANCE, cache);
        }
    }

    /**
     * 转为 unphased 基因型
     */
    public void toUnphased() {
        toUnphased(true);
    }

    /**
     * 转为 unphased 基因型
     *
     * @param inplace 原位改变基因型的向型
     */
    public byte[] toUnphased(boolean inplace) {
        if (this.phased) {
            if (inplace) {
                for (int i = 0; i < this.BEGs.length; i++) {
                    this.BEGs[i] = BEGTransfer.toUnphased(this.BEGs[i]);
                }
                this.phased = false;
                return this.BEGs;
            } else {
                return apply(UnphasedVariantFormatter.INSTANCE);
            }
        } else {
            return this.BEGs;
        }
    }

    /**
     * 将一个多等位基因位点转为多个二等位基因位点
     */
    public SmartList<Variant> split() {
        int[] ACs = getACs();
        int[] transCodeReverse = new int[ACs.length];

        int index = 0;
        SmartList<Variant> out = new SmartList<>();
        transCodeReverse[index++] = 0;

        for (int i = 1; i < ACs.length; i++) {
            if (ACs[i] > 0) {
                transCodeReverse[index++] = i;
            }
        }

        if (index == 1 && transCodeReverse[0] == 0) {
            // 只有一个有效的等位基因
            Variant cacheVariant = new Variant();
            cacheVariant.chromosome = this.chromosome;
            cacheVariant.position = this.position;
            cacheVariant.ploidy = this.ploidy;
            cacheVariant.phased = this.phased;
            cacheVariant.REF = ArrayUtils.copyOfRange(this.REF, 0, this.REF.length);
            cacheVariant.BEGs = new byte[this.BEGs.length];
            cacheVariant.ALT = new byte[]{ByteCode.PERIOD};
            out.add(cacheVariant);
        } else {
            // 有多个有效等位基因时
            if (ACs.length == 2) {
                // 2 等位基因位点, 直接添加自身
                out.add(new Variant(this));
            } else {
                BEGEncoder encoder = BEGEncoder.getEncoder(this.phased);
                for (int i = 1; i < index; i++) {
                    Variant cacheVariant = new Variant();
                    cacheVariant.chromosome = this.chromosome;
                    cacheVariant.position = this.position;
                    cacheVariant.ploidy = this.ploidy;
                    cacheVariant.phased = this.phased;
                    cacheVariant.REF = ArrayUtils.copyOfRange(this.REF, 0, this.REF.length);
                    cacheVariant.BEGs = new byte[this.BEGs.length];
                    cacheVariant.ALT = getAllele(transCodeReverse[i]);

                    for (int j = 0; j < this.BEGs.length; j++) {
                        if (this.BEGs[j] == 0) {
                            cacheVariant.BEGs[j] = 0;
                        } else {
                            int leftGenotype = BEGDecoder.decodeHaplotype(0, this.BEGs[j]);
                            int rightGenotype = BEGDecoder.decodeHaplotype(1, this.BEGs[j]);

                            if (leftGenotype == transCodeReverse[i]) {
                                leftGenotype = 1;
                            } else {
                                leftGenotype = 0;
                            }

                            if (rightGenotype == transCodeReverse[i]) {
                                rightGenotype = 1;
                            } else {
                                rightGenotype = 0;
                            }

                            cacheVariant.BEGs[j] = encoder.encode(leftGenotype, rightGenotype);
                        }
                    }

                    out.add(cacheVariant);
                }
            }
        }

        return out;
    }

    /**
     * 简化 alleles，把 AC = 0 的位点删除
     */
    public boolean simplifyAlleles() {
        return simplifyAlleles(false);
    }

    /**
     * 简化 alleles，把 AC = 0 的碱基删除
     *
     * @param fixREF 是否允许修改 REF
     */
    public boolean simplifyAlleles(boolean fixREF) {
        int[] ACs = getACs();
        int[] transCode = new int[ACs.length];
        int[] transCodeReverse = new int[ACs.length];
        int index = 0;

        if (fixREF) {
            for (int i = 0; i < ACs.length; i++) {
                if (ACs[i] > 0) {
                    transCodeReverse[index] = i;
                    transCode[i] = index++;
                }
            }
        } else {
            transCode[0] = index++;
            transCodeReverse[0] = 0;

            for (int i = 1; i < ACs.length; i++) {
                if (ACs[i] > 0) {
                    transCodeReverse[index] = i;
                    transCode[i] = index++;
                }
            }
        }

        if ((index == 0) || (index == 1 && transCodeReverse[0] == 0)) {
            // 所有的 allele 都是无效的, 此时保持 REF
            this.ALT = new byte[]{ByteCode.PERIOD};
            return true;
        }

        if (index < ACs.length) {
            // 有效等位基因个数比实际少, 此时才触发精简
            boolean fastMode = true;
            for (int i = 0; i < index; i++) {
                if (transCodeReverse[i] != i) {
                    fastMode = false;
                    break;
                }
            }

            if (fastMode) {
                int end = ArrayUtils.indexOf(this.ALT, ByteCode.COMMA, index - 1);
                this.ALT = ArrayUtils.copyOfRange(this.ALT, 0, end == -1 ? this.ALT.length : end);
            } else {
                // 必要进行基因型转换
                if (index == 1 && fixREF) {
                    this.REF = getAllele(transCodeReverse[0]);
                    this.ALT = new byte[]{ByteCode.PERIOD};
                } else {
                    VolumeByteStream newALT = new VolumeByteStream(this.ALT.length);
                    for (int i = 1; i < index; i++) {
                        newALT.write(getAllele(transCodeReverse[i]));
                        newALT.write(ByteCode.COMMA);
                    }

                    this.REF = getAllele(transCode[0]);
                    this.ALT = newALT.rangeOf(0, newALT.size() - 1);
                }

                // 基因型转换
                BEGEncoder encoder = BEGEncoder.getEncoder(this.phased);
                for (int i = 0; i < this.BEGs.length; i++) {
                    this.BEGs[i] = this.BEGs[i] == 0 ? 0 : encoder.encode(transCode[BEGDecoder.decodeHaplotype(0, this.BEGs[i])], transCode[BEGDecoder.decodeHaplotype(1, this.BEGs[i])]);
                }

            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取第 i 个等位基因
     */
    public byte[] getAllele(int index) {
        if (index == 0) {
            return this.REF;
        } else if (index == 1) {
            int pointer = ArrayUtils.indexOf(this.ALT, ByteCode.COMMA);
            if (pointer == -1) {
                return this.ALT;
            } else {
                return ArrayUtils.copyOfRange(this.ALT, 0, pointer);
            }
        } else {
            int pointerStart = ArrayUtils.indexOfN(this.ALT, ByteCode.COMMA, 0, index - 1);
            int pointerEnd = ArrayUtils.indexOfN(this.ALT, ByteCode.COMMA, pointerStart + 1, 1);

            if (pointerStart == -1) {
                return null;
            }

            if (pointerEnd == -1) {
                return ArrayUtils.copyOfRange(this.ALT, pointerStart + 1, this.ALT.length);
            }

            return ArrayUtils.copyOfRange(this.ALT, pointerStart + 1, pointerEnd);
        }
    }

    /**
     * 获取第 i 个等位基因的索引
     */
    public int getAlleleIndex(byte[] allele) {
        if (Arrays.equals(allele, this.REF)) {
            return 0;
        } else {
            // 是否包含逗号
            boolean containComma = ArrayUtils.contain(this.ALT, ByteCode.COMMA);

            if (containComma) {
                int start = 0;
                int end;
                int index = 1;
                while (true) {
                    end = ArrayUtils.indexOf(this.ALT, ByteCode.COMMA, start);
                    if (end != -1) {
                        if (ArrayUtils.equal(this.ALT, start, end, allele, 0, allele.length)) {
                            return index;
                        }

                        start = end + 1;
                        if (start < this.ALT.length) {
                            index++;
                        } else {
                            return -1;
                        }
                    } else {
                        // end == -1
                        if (ArrayUtils.equal(this.ALT, start, this.ALT.length, allele, 0, allele.length)) {
                            return index;
                        }
                        return -1;
                    }
                }
            } else {
                if (Arrays.equals(this.ALT, allele)) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    }

    /**
     * 计算二等位基因位点的 LD 系数
     *
     * @param model   LD 计算模型
     * @param variant 变异位点
     * @return LD 系数计算结果
     */
    public double calculateLDR2(ILDModel model, Variant variant) {
        return model.calculateLDR2(this, variant);
    }

    /**
     * 计算二等位基因位点的 LD 系数
     *
     * @param model   LD 计算模型
     * @param variant 变异位点
     * @return LD 系数计算结果
     */
    public double calculateLDR(ILDModel model, Variant variant) {
        return model.calculateLD(this, variant);
    }

    /**
     * 转为任意格式
     *
     * @param variantFormatter 序列格式转换器
     */
    public <Out> Out apply(VariantFormatter<Void, Out> variantFormatter) {
        return variantFormatter.apply(this);
    }

    /**
     * 转为任意格式
     *
     * @param variantFormatter 序列格式转换器
     */
    public <In, Out> Out apply(VariantFormatter<In, Out> variantFormatter, In params) {
        return variantFormatter.apply(this, params);
    }

    /**
     * 转为任意格式
     *
     * @param variantFormatter 序列格式转换器
     * @param cache            输出缓冲区
     */
    public <Out> int apply(VariantFormatter<Void, Out> variantFormatter, VolumeByteStream cache) {
        return variantFormatter.apply(this, cache);
    }

    @Override
    public Variant clone() {
        return new Variant(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Variant)) {
            return false;
        }

        Variant variant = (Variant) o;
        return position == variant.position && ploidy == variant.ploidy && chromosome.equals(variant.chromosome) && Arrays.equals(REF, variant.REF) && Arrays.equals(ALT, variant.ALT) && Arrays.equals(BEGs, variant.BEGs);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(chromosome, position, ploidy);
        result = 31 * result + Arrays.hashCode(REF);
        result = 31 * result + Arrays.hashCode(ALT);
        result = 31 * result + Arrays.hashCode(BEGs);
        return result;
    }

    @Override
    public String toString() {
        if (chromosome == null || REF == null || ALT == null) {
            return "Variant{empty}";
        } else {
            return "Variant{" +
                    "chromosome='" + chromosome + '\'' +
                    ", position=" + position +
                    ", REF='" + new String(REF) + '\'' +
                    ", ALT='" + new String(ALT) + '\'' +
                    '}';
        }
    }
}