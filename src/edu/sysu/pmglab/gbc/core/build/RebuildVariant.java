package edu.sysu.pmglab.gbc.core.build;

/**
 * @Data        :2021/07/09
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :候选合并信息
 */


class RebuildVariant {
    final int position;
    public final byte[] allelesInfo;
    public final int allelesNum;
    public final int nodeIndex;
    public final int variantIndex;

    RebuildVariant(int position, int allelesNum, byte[] alleles, int nodeIndex, int variantIndex) {
        this.position = position;
        this.allelesNum = allelesNum;
        this.allelesInfo = alleles;
        this.nodeIndex = nodeIndex;
        this.variantIndex = variantIndex;
    }
}