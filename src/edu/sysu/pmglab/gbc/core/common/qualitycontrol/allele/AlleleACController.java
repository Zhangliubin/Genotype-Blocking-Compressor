package edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.check.exception.RuntimeExceptionOptions;

/**
 * @Data        :2021/03/06
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :等位基因计数控制器
 */

public class AlleleACController implements IAlleleQC {
    int minAc;
    int maxAc;

    public static final int DEFAULT = 0;
    public static final int MIN = 0;
    public static final int MAX = Integer.MAX_VALUE;

    public AlleleACController(int minAc, int maxAc) {
        Assert.valueRange(minAc, MIN, MAX);
        Assert.valueRange(maxAc, MIN, MAX);

        this.minAc = minAc;
        this.maxAc = maxAc;
    }

    @Override
    public boolean filter(int alleleCount, int validAllelesNum) {
        return (alleleCount < this.minAc) || (alleleCount > this.maxAc);
    }

    @Override
    public boolean empty() {
        return (this.minAc == MIN) && (this.maxAc == MAX);
    }

    @Override
    public void reset(IAlleleQC controller) {
        Assert.that(getClass().isInstance(controller), RuntimeExceptionOptions.ClassCastException);
        this.minAc = Math.max(this.minAc, ((AlleleACController) controller).minAc);
        this.maxAc = Math.min(this.maxAc, ((AlleleACController) controller).maxAc);
    }

    @Override
    public String toString() {
        return (this.maxAc == MAX) ? "AC >= " + this.minAc : this.minAc + " <= AC <= " + this.maxAc;
    }
}
