package edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.container.SmartList;

import java.util.Iterator;

/**
 * @Data        :2021/06/08
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :群体等位基因水平控制器
 */

public class AlleleQC implements Iterable<IAlleleQC> {
    final SmartList<IAlleleQC> controllers = new SmartList<>(3);

    public AlleleQC() {

    }

    /**
     * 添加控制器
     * @param controller 控制器
     */
    public void add(IAlleleQC controller) {
        // 仅添加非空控制器
        Assert.NotNull(controller);

        if (!controller.empty()) {
            for (IAlleleQC qc : this.controllers) {
                if (qc.getClass().equals(controller.getClass())) {
                    qc.reset(controller);
                    return;
                }
            }
            this.controllers.add(controller);
        }
    }

    /**
     * 移除控制器
     * @param controller 控制器
     */
    public void remove(IAlleleQC controller) {
        Assert.NotNull(controller);

        this.controllers.remove(controller);
    }

    /**
     * 获取所有的控制器
     */
    public IAlleleQC[] getGenotypeQCs() {
        return this.controllers.toArray();
    }

    /**
     * 获取指定的控制器
     */
    public IAlleleQC getGenotypeQC(int index) {
        return this.controllers.get(index);
    }

    /**
     * 控制器数量
     */
    public int size() {
        return this.controllers.size();
    }

    /**
     * 清除所有的控制器
     */
    public void clear() {
        this.controllers.clear();
    }

    @Override
    public String toString() {
        Iterator<IAlleleQC> it = iterator();

        StringBuilder sb = new StringBuilder();
        while (true) {
            IAlleleQC e = it.next();
            sb.append(e);
            if (!it.hasNext()) {
                return sb.toString();
            }
            sb.append(',').append(' ');
        }
    }

    /**
     * 对外执行群体等位基因水平过滤
     * @param alleleCounts 等位基因计数
     * @param validAllelesNum 有效等位基因数
     * @return 是否需要移除该位点 (true 代表需要移除)
     */
    public boolean filter(int alleleCounts, int validAllelesNum) {
        for (IAlleleQC filter : this.controllers) {
            if (filter.filter(alleleCounts, validAllelesNum)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Iterator<IAlleleQC> iterator() {
        return this.controllers.iterator();
    }
}