package edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.exception.RuntimeExceptionOptions;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;

/**
 * @Data        :2021/06/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :位点MQ控制器
 */

public class VariantMQController implements IVariantQC {
    /**
     * DP 字符
     */
    public static final byte[] KEYWORD = {0x4d, 0x51};

    public static final int DEFAULT = 20;
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE - 2;
    int method;

    public VariantMQController() {
        this.method = DEFAULT;
    }

    public VariantMQController(int method) {
        Assert.valueRange(method, MIN, MAX);
        this.method = method;
    }

    @Override
    public boolean qualityControl(VolumeByteStream variant, int allelesNum, int qualStart, int qualEnd, int infoStart, int infoEnd) {
        int score = calculateInfoField(variant, infoStart, infoEnd, KEYWORD);
        if (score == -1) {
            return false;
        } else {
            return score < this.method;
        }
    }

    @Override
    public boolean empty() {
        return this.method == MIN;
    }

    @Override
    public void reset(IVariantQC variantQC) {
        Assert.that(getClass().isInstance(variantQC), RuntimeExceptionOptions.ClassCastException);
        this.method = Math.max(this.method, variantQC.getMethod());
    }

    @Override
    public int getMethod() {
        return this.method;
    }

    @Override
    public String toString() {
        return "MQ >= " + this.method;
    }
}
