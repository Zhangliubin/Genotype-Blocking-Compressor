package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.easytools.ValueUtils;

/**
 * @Data        :2021/03/10
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :解压的位点任务
 */

class TaskVariant {
    public int index;
    public int position;
    public int decoderIndex;

    public int alleleStart;
    public int alleleLength;

    /**
     * 设置该位点任务的 position
     */
    public TaskVariant setPosition(byte byte1, byte byte2, byte byte3, byte byte4) {
        this.position = ValueUtils.byteArray2IntegerValue(byte1, byte2, byte3, byte4);
        return this;
    }

    /**
     * 设置该位点的索引
     */
    public TaskVariant setIndex(int index) {
        this.index = index;
        return this;
    }

    /**
     * 设置该位点的索引
     */
    TaskVariant reload() {
        this.index = -1;
        return this;
    }

    /**
     * 设置该位点的索引
     */
    public TaskVariant setDecoderIndex(int index) {
        this.decoderIndex = index;
        return this;
    }

    /**
     * 设置该位点的等位基因数据
     */
    public TaskVariant setAlleleInfo(int start, int end) {
        this.alleleStart = start;
        this.alleleLength = end - start;
        return this;
    }

    /**
     * 两个位点任务的比对
     */
    public static int compareVariant(TaskVariant v1, TaskVariant v2) {
        int status = Integer.compare(v1.position, v2.position);
        if (status == 0) {
            status = Integer.compare(v1.decoderIndex, v2.decoderIndex);
            if (status == 0) {
                status = Integer.compare(v1.alleleLength, v2.alleleLength);
            }
        }

        return status;
    }
}
