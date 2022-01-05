package edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;

import java.util.Iterator;

/**
 * @Data        :2021/06/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :位点控制器抽象类
 */

public class VariantQC implements Iterable<IVariantQC> {
    final SmartList<IVariantQC> controllers = new SmartList<>(4);

    public VariantQC() {
    }

    /**
     * 添加控制器
     * @param controller 控制器
     */
    public void add(IVariantQC controller) {
        // 仅添加非空控制器
        Assert.NotNull(controller);

        if (!controller.empty()) {
            for (IVariantQC qc : this.controllers) {
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
    public void remove(IVariantQC controller) {
        Assert.NotNull(controller);

        this.controllers.remove(controller);
    }

    /**
     * 获取所有的控制器
     */
    public IVariantQC[] getVariantQCs() {
        return this.controllers.toArray();
    }

    /**
     * 获取指定的控制器
     */
    public IVariantQC getVariantQC(int index) {
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

    /**
     * 质控方法，false 代表保留，true 代表需要剔除
     * @param variant 变异位点序列
     * @param allelesNum 等位基因数
     * @param qualStart 质量值开始位置
     * @param qualEnd 质量值结束位置
     * @param infoStart info值开始位置
     * @param infoEnd info值结束位置
     */
    public boolean filter(VolumeByteStream variant, int allelesNum, int qualStart, int qualEnd, int infoStart, int infoEnd) {
        for (IVariantQC controller : this.controllers) {
            if (controller.qualityControl(variant, allelesNum, qualStart, qualEnd, infoStart, infoEnd)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        Iterator<IVariantQC> it = iterator();

        StringBuilder sb = new StringBuilder();
        while (true) {
            IVariantQC e = it.next();
            sb.append(e);
            if (!it.hasNext()) {
                return sb.toString();
            }
            sb.append(',').append(' ');
        }
    }

    @Override
    public Iterator<IVariantQC> iterator() {
        return this.controllers.iterator();
    }
}