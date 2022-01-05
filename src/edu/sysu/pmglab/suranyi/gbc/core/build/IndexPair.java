package edu.sysu.pmglab.suranyi.gbc.core.build;

/**
 * @Data :2021/04/12
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :索引对
 */

class IndexPair {
    public int seqIndex;
    public int index;
    public int groupIndex;
    public int codeIndex;

    public IndexPair(int seqIndex, int index, int groupIndex, int codeIndex) {
        this.seqIndex = seqIndex;
        this.index = index;
        this.groupIndex = groupIndex;
        this.codeIndex = codeIndex;
    }

    @Override
    public String toString() {
        return "IndexPair{" +
                "seqIndex=" + seqIndex +
                ", index=" + index +
                ", groupIndex=" + groupIndex +
                ", codeIndex=" + codeIndex +
                '}';
    }
}