package edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker;

import edu.sysu.pmglab.suranyi.check.Assert;

/**
 * @author suranyi
 * @description 简易卡方检验
 */

public class AlleleFreqTestChecker implements AlleleChecker {
    public final double alpha;
    public final static double DEFAULT_ALPHA = 0.05;

    public AlleleFreqTestChecker() {
        this(DEFAULT_ALPHA);
    }

    public AlleleFreqTestChecker(float alpha) {
        this((double) alpha);
    }

    public AlleleFreqTestChecker(double alpha) {
        Assert.valueRange(alpha, 1e-6, 0.5);
        this.alpha = alpha;
    }

    @Override
    public boolean isEqual(double AC11, double AC12, double AC21, double AC22) {
        // 原假设: 观测频数没有区别 (即两位点的碱基序列相同)
        double fq1 = AC11 / (AC11 + AC12);
        double fq2 = AC21 / (AC21 + AC22);

        // 卡方值小于给定最大值，接受原假设，两位点碱基序列差异不大
        return Math.abs(fq1 - fq2) < this.alpha;
    }

    @Override
    public String toString() {
        return "|af1 - af2| < " + this.alpha;
    }
}
