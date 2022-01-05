package edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.genotype;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.exception.RuntimeExceptionOptions;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;

/**
 * @Data        :2021/06/15
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :基因型水平Gq控制器
 */

public class GenotypeGQController implements IGenotypeQC {
    /**
     * GQ 字符
     */
    public static final byte[] KEYWORD = {0x47, 0x51};

    public static final int DEFAULT = 20;
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE - 2;

    int method;

    public GenotypeGQController() {
        this.method = DEFAULT;
    }

    public GenotypeGQController(int method) {
        Assert.valueRange(method, MIN, MAX);
        this.method = method;
    }

    @Override
    public boolean empty() {
        return method == MIN;
    }

    @Override
    public boolean qualityControl(VolumeByteStream variant, int seek, int length, int index) {
        int score = calculateScore(variant, seek, length, index);
        return score < this.method;
    }

    @Override
    public byte[] getKeyWord() {
        return KEYWORD;
    }

    @Override
    public void reset(IGenotypeQC genotypeQC) {
        Assert.that(getClass().isInstance(genotypeQC), RuntimeExceptionOptions.ClassCastException);
        this.method = Math.max(this.method, genotypeQC.getMethod());
    }

    @Override
    public int getMethod() {
        return this.method;
    }

    @Override
    public String toString() {
        return "GQ >= " + this.method;
    }
}
