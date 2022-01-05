package edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.genotype;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.exception.RuntimeExceptionOptions;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;

/**
 * @Data        :2021/06/15
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :基因型水平DP控制器
 */

public class GenotypeDPController implements IGenotypeQC {
    /**
     * DP 字符
     */
    public static final byte[] KEYWORD = {0x44, 0x50};

    public static final int DEFAULT = 4;
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE - 2;

    int method;

    public GenotypeDPController() {
        this.method = DEFAULT;
    }

    public GenotypeDPController(int method) {
        Assert.valueRange(method, MIN, MAX);
        this.method = method;
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
    public boolean empty() {
        return method == MIN;
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
        return "DP >= " + this.method;
    }
}
