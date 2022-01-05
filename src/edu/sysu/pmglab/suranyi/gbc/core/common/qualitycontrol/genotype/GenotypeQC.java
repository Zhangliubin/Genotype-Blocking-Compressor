package edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.genotype;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.IVariantQC;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @Data        :2021/06/15
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :基因型水平控制器
 */

public class GenotypeQC implements Iterable<IGenotypeQC> {
    final SmartList<IGenotypeQC> controllers = new SmartList<>(2);

    public GenotypeQC() {
    }

    /**
     * 添加控制器
     * @param controller 控制器
     */
    public void add(IGenotypeQC controller) {
        // 仅添加非空控制器
        Assert.NotNull(controller);

        if (!controller.empty()) {
            for (IGenotypeQC qc : this.controllers) {
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
    public void remove(IGenotypeQC controller) {
        Assert.NotNull(controller);

        this.controllers.remove(controller);
    }

    /**
     * 获取所有的控制器
     */
    public IGenotypeQC[] getGenotypeQCs() {
        return this.controllers.toArray();
    }

    /**
     * 获取指定的控制器
     */
    public IGenotypeQC getGenotypeQC(int index) {
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
     * 根据传入的变异位点序列 FORMAT 信息加载格式匹配器
     * @param variant 变异位点序列
     * @param formatStartSeek FORMAT 开始的指针
     * @param formatEndSeek FORMAT 结束的指针
     */
    public int[] load(VolumeByteStream variant, int formatStartSeek, int formatEndSeek) {
        if (formatEndSeek - formatStartSeek == 2) {
            return null;
        }
        formatStartSeek += 3;

        int[] indexes = new int[this.controllers.size()];

        // 寻找自定义的字段
        next:
        for (int i = 0; i < this.controllers.size(); i++) {
            int index = 1;
            byte[] keyword = this.controllers.get(i).getKeyWord();

            int pointer = formatStartSeek;
            while (pointer < formatEndSeek - 1) {
                if (variant.startWith(pointer, keyword)) {
                    indexes[i] = index;
                    continue next;
                }

                if (variant.cacheOf(pointer) == ByteCode.COLON) {
                    index++;
                }

                pointer++;
            }

            indexes[i] = -1;
        }

        return indexes;
    }

    /**
     * 质控对外方法，false 代表保留，true 代表需要剔除
     * @param variant 变异位点序列
     * @param seek 开始的指针/位置
     * @param length 长度
     */
    public boolean filter(VolumeByteStream variant, int seek, int length, int[] indexes) {
        for (int i = 0; i < controllers.size(); i++) {
            if (indexes[i] != -1 && this.controllers.get(i).qualityControl(variant, seek, length, indexes[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        Iterator<IGenotypeQC> it = iterator();

        StringBuilder sb = new StringBuilder();
        while (true) {
            IGenotypeQC e = it.next();
            sb.append(e);
            if (!it.hasNext()) {
                return sb.toString();
            }
            sb.append(',').append(' ');
        }
    }

    @Override
    public Iterator<IGenotypeQC> iterator() {
        return this.controllers.iterator();
    }
}

