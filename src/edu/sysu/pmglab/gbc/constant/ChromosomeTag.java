package edu.sysu.pmglab.gbc.constant;

import edu.sysu.pmglab.check.Assert;

/**
 * @Data        :2021/09/04
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :
 */

public class ChromosomeTag {
    public final int chromosomeIndex;
    public final String chromosomeString;
    public final byte[] chromosomeByteArray;
    public final int ploidy;
    public final int length;
    public final String reference;

    ChromosomeTag(int chromosomeIndex, String chromosomeString, int ploidy, int length) {
        Assert.that(chromosomeIndex >= 0 && chromosomeIndex <= 255, "too much chromosome input (> 256)");
        Assert.valueRange(ploidy, 1, 2);
        this.chromosomeIndex = chromosomeIndex;
        this.chromosomeString = chromosomeString;
        this.chromosomeByteArray = chromosomeString.getBytes();
        this.ploidy = ploidy;
        this.length = length;
        this.reference = null;
    }

    public ChromosomeTag(int chromosomeIndex, String chromosomeString, int ploidy, int length, String reference) {
        Assert.that(chromosomeIndex >= 0 && chromosomeIndex <= 255, "too much chromosome input (> 256)");
        Assert.valueRange(ploidy, 1, 2);
        this.chromosomeIndex = chromosomeIndex;
        this.chromosomeString = chromosomeString;
        this.chromosomeByteArray = chromosomeString.getBytes();
        this.ploidy = ploidy;
        this.length = length > 0 ? length : -1;
        this.reference = reference;
    }

    @Override
    public String toString() {
        return "Chromosome{" +
                "chromosomeIndex=" + chromosomeIndex +
                ", chromosomeString='" + chromosomeString + '\'' +
                ", ploidy=" + ploidy +
                ", length=" + length +
                ", reference='" + reference + '\'' +
                '}';
    }
}