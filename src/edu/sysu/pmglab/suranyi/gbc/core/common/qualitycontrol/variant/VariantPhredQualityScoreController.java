package edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.exception.RuntimeExceptionOptions;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;

/**
 * @Data        :2021/06/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :位点QUAL控制器
 */

public class VariantPhredQualityScoreController implements IVariantQC {
    public static final int DEFAULT = 30;
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE - 2;
    int method;

    public VariantPhredQualityScoreController() {
        this.method = DEFAULT;
    }

    public VariantPhredQualityScoreController(int method) {
        Assert.valueRange(method, MIN, MAX);
        this.method = method;
    }

    @Override
    public boolean qualityControl(VolumeByteStream variant, int allelesNum, int qualStart, int qualEnd, int infoStart, int infoEnd) {
        // 如果是 .，则默认不进行过滤，返回 false
        if (variant.cacheOf(qualStart) != ByteCode.PERIOD) {
            int score = 0;
            for (int index = qualStart; index < qualEnd && variant.cacheOf(index) != ByteCode.PERIOD; index++) {
                score = score * 10 + (variant.cacheOf(index) - 48);
            }

            return score < this.method;
        }
        return false;
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
        return "Phred quality score >= " + this.method;
    }
}
