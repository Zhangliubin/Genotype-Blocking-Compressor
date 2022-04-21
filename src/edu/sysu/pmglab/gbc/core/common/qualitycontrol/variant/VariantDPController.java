package edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.check.exception.RuntimeExceptionOptions;
import edu.sysu.pmglab.container.VolumeByteStream;

/**
 * @Data        :2021/06/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :位点DP控制器
 */

public class VariantDPController implements IVariantQC {
    /**
     * DP 字符, 默认不进行过滤
     */
    public static final byte[] KEYWORD = {0x44, 0x50};
    public static final int DEFAULT = 0;
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE - 2;
    int method;

    public VariantDPController() {
        this.method = DEFAULT;
    }

    public VariantDPController(int method) {
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
        return "DP >= " + this.method;
    }
}
