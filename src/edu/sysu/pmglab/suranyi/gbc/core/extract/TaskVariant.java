package edu.sysu.pmglab.suranyi.gbc.core.extract;

import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;

import java.util.HashSet;

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
    public VolumeByteStream positionInfo = new VolumeByteStream(10);

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
     * 检查位点的范围是否符合条件，若符合则设置为 index，不符合则设置为 -1 (即抛弃该任务)
     */
    TaskVariant checkBounds(int minPos, int maxPos, int index) {
        if ((this.position < minPos) || (this.position > maxPos)) {
            return reload();
        }

        return setIndex(index);
    }

    /**
     * 检查位点的范围是否符合条件，若符合则设置为 index，不符合则设置为 -1 (即抛弃该任务)
     */
    TaskVariant checkBounds(HashSet<Integer> objTask, int index) {
        if (!objTask.contains(this.position)) {
            return reload();
        }

        return setIndex(index);
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
     * 设置该位点的位置数据
     */
    TaskVariant setPositionInfo() {
        // 重设位置缓冲区
        byte[] cache = this.positionInfo.getCache();
        int resNum = position;
        if (resNum == 0) {
            cache[0] = ByteCode.ZERO;
            this.positionInfo.reset(1);
        } else {
            int length = (int) (Math.floor(Math.log10(resNum)) + 1);
            this.positionInfo.reset(length);
            int index = length - 1;
            while (resNum > 0) {
                cache[index--] = (byte) (resNum % 10 + 48);
                resNum = resNum / 10;
            }
        }

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

    @Override
    public String toString() {
        return "VariantTask{" +
                "index=" + index +
                ", position=" + position +
                ", decoderIndex=" + decoderIndex +
                ", alleleRange: " + alleleStart + "~" + (alleleStart + alleleLength) +
                '}';
    }
}
