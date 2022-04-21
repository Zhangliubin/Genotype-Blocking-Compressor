package edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele;

/**
 * @Data        :2021/03/07
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :控制器接口
 */

public interface IAlleleQC {
    /**
     * 执行过滤，true 时 移除该位点
     * @param alleleCount 该位点的等位基因计数
     * @param validAllelesNum 有效等位基因总数
     * @return 是否移除该位点数据
     */
    boolean filter(int alleleCount, int validAllelesNum);

    /**
     * 是否为空控制器
     * @return 是否为空控制器
     */
    default boolean empty() {
        return false;
    }

    /**
     * 重设控制器
     * @param filter 新控制器
     */
    default void reset(IAlleleQC filter) {};
}
