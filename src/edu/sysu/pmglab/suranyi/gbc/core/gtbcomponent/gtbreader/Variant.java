package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.gbc.coder.BEGTransfer;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.coder.encoder.BEGEncoder;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
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
     * @return 可替代等位基因个数
     */
    public int getAlternativeAlleleNum() {
        return ArrayUtils.valueCounts(ALT, ByteCode.COMMA) + 2;
    }

    /**
     * 获取 AC 值, 该值已通过倍型校正
     * @return 获取 AC 值
     */
    public int getAC() {
        return apply(ACValueFormatter.INSTANCE);
    }

    /**
     * 获取 AN 值, 该值已通过倍型校正
     * @return 获取 AN 值
     */
    public int getAN() {
        return apply(ANValueFormatter.INSTANCE);
    }

    /**
     * 获取缺失样本的个数
     * @return 缺失样本个数
     */
    public int getMissSubjectNum() {
        return apply(MissSubjectNumValueFormatter.INSTANCE);
    }

    /**
     * 获取 AF
     * @return AF 值，等位基因频率
     */
    public double getAF() {
        return apply(AFValueFormatter.INSTANCE);
    }

    /**
     * 获取所有等位基因形式的频率值
     * @return AFs 值，等位基因频率
     */
    public double[] getAFs() {
        return apply(AFsValueFormatter.INSTANCE);
    }

    /**
     * 获取 MAF
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
     * @param index 编码索引
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
                // 此时处理的情况复杂的多，会变成多等位基因位点
                byte[] transCode = new byte[allelesNum];
                VolumeByteStream finalALT = new VolumeByteStream(newALT.length + REF.length + ALT.length + 2);
                finalALT.write(newALT);

                byte newAllelesNum = (byte) (ArrayUtils.valueCounts(newALT, ByteCode.COMMA) + 2);

                byte[][] oldMatchers = new byte[allelesNum][];
                byte[][] newMatchers = new byte[newAllelesNum][];

                oldMatchers[0] = REF;
                int lastPointer = 0;
                int searchPointer = 1;
                int alleleIndex = 1;

                while (searchPointer < ALT.length) {
                    if (ALT[searchPointer] == ByteCode.COMMA) {
                        oldMatchers[alleleIndex++] = ArrayUtils.copyOfRange(ALT, lastPointer, searchPointer);
                        lastPointer = searchPointer + 1;
                    }

                    searchPointer++;
                }
                oldMatchers[alleleIndex] = ArrayUtils.copyOfRange(ALT, lastPointer, searchPointer);

                newMatchers[0] = newREF;
                lastPointer = 0;
                searchPointer = 1;
                alleleIndex = 1;

                while (searchPointer < newALT.length) {
                    if (newALT[searchPointer] == ByteCode.COMMA) {
                        newMatchers[alleleIndex++] = ArrayUtils.copyOfRange(newALT, lastPointer, searchPointer);
                        lastPointer = searchPointer + 1;
                    }

                    searchPointer++;
                }
                newMatchers[alleleIndex] = ArrayUtils.copyOfRange(newALT, lastPointer, searchPointer);

                out:
                for (byte id1 = 0; id1 < oldMatchers.length; id1++) {
                    for (byte id2 = 0; id2 < newMatchers.length; id2++) {
                        if (ArrayUtils.equal(oldMatchers[id1], newMatchers[id2])) {
                            transCode[id1] = id2;
                            continue out;
                        }
                    }

                    // 没有发现匹配的，说明是新碱基
                    transCode[id1] = newAllelesNum++;
                    finalALT.write(ByteCode.COMMA);
                    finalALT.write(oldMatchers[id1]);
                }
                REF = newREF;
                ALT = finalALT.remaining() == 0 ? finalALT.getCache() : finalALT.values();
                newMatchers = null;
                oldMatchers = null;
                finalALT = null;

                // 转换编码
                BEGEncoder encoder = BEGEncoder.getEncoder(this.phased);
                for (int i = 0; i < this.BEGs.length; i++) {
                    BEGs[i] = BEGs[i] == 0 ? 0 : encoder.encode(transCode[BEGDecoder.decodeHaplotype(0, BEGs[i])], transCode[BEGDecoder.decodeHaplotype(1, BEGs[i])]);
                }
            }
        }
    }

    /**
     * 横向合并位点 (认为它们来自不同的测序样本)
     * @param otherVariant 另一个变异位点
     */
    public Variant merge(Variant otherVariant) {
        return merge(otherVariant, true);
    }

    /**
     * 横向合并位点 (认为它们来自不同的测序样本)
     * @param otherVariant 另一个变异位点
     * @param verifyCoordinate 是否验证坐标
     */
    public Variant merge(final Variant otherVariant, boolean verifyCoordinate) {
        if (verifyCoordinate && !(otherVariant.chromosome.equals(this.chromosome) && (otherVariant.position == this.position))) {
            throw new UnsupportedOperationException("Requests to merge variant with different coordinates are not allowed.");
        }

        Variant mergeVariant = new Variant();
        mergeVariant.chromosome = this.chromosome;
        mergeVariant.position = this.position;
        mergeVariant.ploidy = this.ploidy;
        mergeVariant.phased = this.phased;

        // 传入位点作为主位点
        if ((ArrayUtils.equal(REF, otherVariant.REF) && (ArrayUtils.equal(ALT, otherVariant.ALT)))) {
            mergeVariant.REF = this.REF;
            mergeVariant.ALT = this.ALT;
            mergeVariant.BEGs = ArrayUtils.merge(this.BEGs, otherVariant.BEGs);
        } else {
            // 获取等位基因个数
            int allelesNum = getAlternativeAlleleNum();

            // 此时处理的情况复杂的多，会变成多等位基因位点
            VolumeByteStream finalALT = new VolumeByteStream(ALT.length + otherVariant.REF.length + otherVariant.ALT.length + 2);
            finalALT.write(ALT);

            int otherAllelesNum = otherVariant.getAlternativeAlleleNum();
            byte[] transCode = new byte[otherAllelesNum];

            byte[][] oldMatchers = new byte[otherAllelesNum][];
            byte[][] newMatchers = new byte[allelesNum][];

            oldMatchers[0] = otherVariant.REF;
            int lastPointer = 0;
            int searchPointer = 1;
            int alleleIndex = 1;

            while (searchPointer < otherVariant.ALT.length) {
                if (otherVariant.ALT[searchPointer] == ByteCode.COMMA) {
                    oldMatchers[alleleIndex++] = ArrayUtils.copyOfRange(otherVariant.ALT, lastPointer, searchPointer);
                    lastPointer = searchPointer + 1;
                }

                searchPointer++;
            }
            oldMatchers[alleleIndex] = ArrayUtils.copyOfRange(otherVariant.ALT, lastPointer, searchPointer);

            newMatchers[0] = REF;
            lastPointer = 0;
            searchPointer = 1;
            alleleIndex = 1;

            while (searchPointer < ALT.length) {
                if (ALT[searchPointer] == ByteCode.COMMA) {
                    newMatchers[alleleIndex++] = ArrayUtils.copyOfRange(ALT, lastPointer, searchPointer);
                    lastPointer = searchPointer + 1;
                }

                searchPointer++;
            }
            newMatchers[alleleIndex] = ArrayUtils.copyOfRange(ALT, lastPointer, searchPointer);

            out:
            for (byte id1 = 0; id1 < oldMatchers.length; id1++) {
                for (byte id2 = 0; id2 < newMatchers.length; id2++) {
                    if (ArrayUtils.equal(oldMatchers[id1], newMatchers[id2])) {
                        transCode[id1] = id2;
                        continue out;
                    }
                }

                // 没有发现匹配的，说明是新碱基
                transCode[id1] = (byte) allelesNum++;
                finalALT.write(ByteCode.COMMA);
                finalALT.write(oldMatchers[id1]);
            }
            mergeVariant.REF = this.REF;
            mergeVariant.ALT = finalALT.remaining() == 0 ? finalALT.getCache() : finalALT.values();

            // 转换编码
            BEGEncoder encoder = BEGEncoder.getEncoder(this.phased);
            mergeVariant.BEGs = new byte[this.BEGs.length + otherVariant.BEGs.length];
            System.arraycopy(this.BEGs, 0, mergeVariant.BEGs, 0, this.BEGs.length);
            for (int i = 0; i < otherVariant.BEGs.length; i++) {
                mergeVariant.BEGs[this.BEGs.length + i] = otherVariant.BEGs[i] == 0 ? 0 : encoder.encode(transCode[BEGDecoder.decodeHaplotype(0, otherVariant.BEGs[i])], transCode[BEGDecoder.decodeHaplotype(1, otherVariant.BEGs[i])]);
            }
        }
        return mergeVariant;
    }

    /**
     * 转为 VCF 格式
     */
    public byte[] toVCF() {
        return toVCF(false);
    }

    /**
     * 转为 VCF 格式
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
     * @param cache 输出缓冲区
     */
    public int toVCFSite(VolumeByteStream cache) {
        return apply(VCFSiteVariantFormatter.INSTANCE, cache);
    }

    /**
     * 转为 VCF 格式
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
     * 转为任意格式
     * @param variantFormatter 序列格式转换器
     */
    public <Out> Out apply(VariantFormatter<Void, Out> variantFormatter) {
        return variantFormatter.apply(this);
    }

    /**
     * 转为任意格式
     * @param variantFormatter 序列格式转换器
     */
    public <In, Out> Out apply(VariantFormatter<In, Out> variantFormatter, In params) {
        return variantFormatter.apply(this, params);
    }

    /**
     * 转为任意格式
     * @param variantFormatter 序列格式转换器
     * @param cache 输出缓冲区
     */
    public <Out> int apply(VariantFormatter<Void, Out> variantFormatter, VolumeByteStream cache) {
        return variantFormatter.apply(this, cache);
    }

    @Override
    public Variant clone() throws CloneNotSupportedException {
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
        return "Variant{" +
                "chromosome='" + chromosome + '\'' +
                ", position=" + position +
                ", REF='" + new String(REF) + '\'' +
                ", ALT='" + new String(ALT) + '\'' +
                '}';
    }
}