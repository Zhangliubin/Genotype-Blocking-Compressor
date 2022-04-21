package edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.check.exception.RuntimeExceptionOptions;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.gbc.coder.CoderConfig;

/**
 * @Data        :2021/06/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :位点等位基因数控制器
 */

public class VariantAllelesNumController implements IVariantQC {
    public static final int DEFAULT = 15;
    public static final int MIN = 2;
    public static final int MAX = CoderConfig.MAX_ALLELE_NUM;
    int method;

    public VariantAllelesNumController() {
        this.method = DEFAULT;
    }

    public VariantAllelesNumController(int method) {
        Assert.valueRange(method, MIN, MAX);
        this.method = method;
    }

    @Override
    public boolean qualityControl(VolumeByteStream variant, int allelesNum, int qualStart, int qualEnd, int infoStart, int infoEnd) {
        return allelesNum > this.method;
    }

    @Override
    public boolean empty() {
        return false;
    }

    @Override
    public void reset(IVariantQC variantQC) {
        Assert.that(getClass().isInstance(variantQC), RuntimeExceptionOptions.ClassCastException);
        this.method = Math.min(this.method, variantQC.getMethod());
    }

    @Override
    public int getMethod() {
        return this.method;
    }

    @Override
    public String toString() {
        return "allelesNum <= " + this.method;
    }
}
