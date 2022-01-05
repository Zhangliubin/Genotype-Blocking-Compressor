package edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.exception.RuntimeExceptionOptions;

/**
 * @Data        :2021/03/06
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :最小有效等位基因数控制器
 */

public class AlleleANController implements IAlleleQC {
    int minAn;
    int maxAn;

    public static final int DEFAULT = 0;
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE;

    public AlleleANController(int minAn, int maxAn) {
        Assert.valueRange(minAn, MIN, MAX);
        Assert.valueRange(maxAn, MIN, MAX);

        this.minAn = minAn;
        this.maxAn = maxAn;
    }

    @Override
    public boolean filter(int alleleCount, int validAllelesNum) {
        return validAllelesNum < this.minAn || validAllelesNum > this.maxAn;
    }

    @Override
    public boolean empty() {
        return (this.minAn == MIN) && (this.maxAn == MAX);
    }

    @Override
    public void reset(IAlleleQC filter) {
        Assert.that(getClass().isInstance(filter), RuntimeExceptionOptions.ClassCastException);
        this.minAn = Math.max(this.minAn, ((AlleleANController) filter).minAn);
    }

    @Override
    public String toString() {
        return (this.maxAn == MAX) ? "AN >= " + this.minAn : this.minAn + " <= AN <= " + this.maxAn;
    }
}
