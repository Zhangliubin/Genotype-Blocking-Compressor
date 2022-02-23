package edu.sysu.pmglab.suranyi.gbc.core.common.allelechecker;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.Variant;

import java.io.IOException;
import java.util.HashSet;

/**
 * @author suranyi
 * @description 等位基因频率差值检验
 */

public class AlleleFreqTestChecker implements AlleleChecker {
    public final double alpha;
    public double maf = DEFAULT_MAF;
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

    public AlleleFreqTestChecker(double alpha, double maf) {
        Assert.valueRange(alpha, 1e-6, 0.5);
        Assert.valueRange(maf, 0, 1);
        this.alpha = alpha;
        this.maf = maf;
    }

    @Override
    public boolean isEqual(Variant variant1, Variant variant2, double AC11, double AC12, double AC21, double AC22, boolean reverse) {
        // 原假设: 观测频数没有区别 (即两位点的碱基序列相同)
        double fq1 = AC11 / (AC11 + AC12);
        double fq2 = AC21 / (AC21 + AC22);

        // 如果是常见变异，则不能使用这个方法判断
        if (Math.min(fq1, 1 - fq1) <= this.maf && Math.min(fq2, 1 - fq2) <= this.maf) {
            return Math.abs(fq1 - fq2) < this.alpha;
        }

        return false;
    }

    @Override
    public void setReader(GTBManager manager1, GTBManager manager2) throws IOException {

    }

    @Override
    public void setPosition(HashSet<Integer> position) {

    }

    @Override
    public String toString() {
        if (this.maf == 1) {
            return "|af1 - af2| < " + this.alpha;
        } else {
            return "MAF <= " + this.maf + " and |af1 - af2| < " + this.alpha;
        }
    }

    @Override
    public void close() throws Exception {

    }
}
