package edu.sysu.pmglab.gbc.core.build;

/**
 * @Data        :2021/08/30
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :位点坐标
 */

class VariantCoordinate {
    public final short managerIndex;
    public final int nodeIndex;
    public final int variantIndex;

    public VariantCoordinate(short managerIndex, int nodeIndex, int variantIndex) {
        this.managerIndex = managerIndex;
        this.nodeIndex = nodeIndex;
        this.variantIndex = variantIndex;
    }
}