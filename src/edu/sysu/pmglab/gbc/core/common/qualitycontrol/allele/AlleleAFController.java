package edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.check.exception.RuntimeExceptionOptions;

/**
 * @Data        :2021/03/06
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :等位基因频率控制器
 */

public class AlleleAFController implements IAlleleQC {
    double minAf;
    double maxAf;

    public static final double DEFAULT = 0;
    public static final double MIN = 0;
    public static final double MAX = 1;

    public AlleleAFController(double minAf, double maxAf) {
        Assert.valueRange(minAf, MIN, MAX);
        Assert.valueRange(maxAf, MIN, MAX);

        this.minAf = minAf;
        this.maxAf = maxAf;
    }

    @Override
    public boolean filter(int alleleCount, int validAllelesNum) {
        double minAc = validAllelesNum * this.minAf;
        double maxAc = validAllelesNum * this.maxAf;

        return (alleleCount < minAc) || (alleleCount > maxAc);
    }

    @Override
    public boolean empty() {
        return (this.minAf == MIN) && (this.maxAf == MAX);
    }

    @Override
    public void reset(IAlleleQC controller) {
        Assert.that(getClass().isInstance(controller), RuntimeExceptionOptions.ClassCastException);
        this.minAf = Math.max(this.minAf, ((AlleleAFController) controller).minAf);
        this.maxAf = Math.min(this.maxAf, ((AlleleAFController) controller).maxAf);
    }

    @Override
    public String toString() {
        return this.minAf + " <= AF <= " + this.maxAf;
    }
}
