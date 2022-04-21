package edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant;

import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;

import java.math.BigDecimal;

/**
 * @Data        :2021/08/29
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :位点质控器接口
 */

public interface IVariantQC {
    /**
     * 质控方法，false 代表保留，true 代表需要剔除
     * @param variant 变异位点序列
     * @param allelesNum 等位基因数
     * @param qualStart 质量值开始位置
     * @param qualEnd 质量值结束位置
     * @param infoStart info值开始位置
     * @param infoEnd info值结束位置
     * @return 是否移除该位点数据
     */
    boolean qualityControl(VolumeByteStream variant, int allelesNum, int qualStart, int qualEnd, int infoStart, int infoEnd);

    /**
     * 是否为空过滤器
     * @return 是否为空过滤器
     */
    default boolean empty() {
        return false;
    }

    /**
     * 计算信息字段值
     * @param variant 变异位点序列
     * @param infoStart info值开始位置
     * @param infoEnd info值结束位置
     * @param KEYWORD 识别的关键字
     * @return 计算值，-1 表示不存在此字段
     */
    default int calculateInfoField(VolumeByteStream variant, int infoStart, int infoEnd, final byte[] KEYWORD) {
        if (variant.cacheOf(infoStart) != ByteCode.PERIOD) {
            int fieldEnd;

            // 检索第一个位置是否为关键字段
            if (ArrayUtils.startWiths(variant.getCache(), infoStart, KEYWORD) && variant.cacheOf(infoStart + KEYWORD.length) == ByteCode.EQUAL) {
                infoStart += KEYWORD.length + 1;
                fieldEnd = variant.indexOf(ByteCode.SEMICOLON, infoStart + 1, infoEnd);
                if (fieldEnd == -1) {
                    // 没有找到 ;，表明 INFO 只有一个值
                    fieldEnd = infoEnd;
                }

                return formatToIntegerValue(variant, infoStart, fieldEnd, KEYWORD);
            } else {
                while (infoStart < infoEnd) {
                    infoStart = variant.indexOf(KEYWORD, infoStart, infoEnd);
                    if (infoStart == -1) {
                        return -1;
                    } else {
                        if (variant.cacheOf(infoStart - 1) == ByteCode.SEMICOLON && variant.cacheOf(infoStart + KEYWORD.length) == ByteCode.EQUAL) {
                            fieldEnd = variant.indexOf(ByteCode.SEMICOLON, infoStart + 3, infoEnd);
                            return formatToIntegerValue(variant, infoStart + 3, fieldEnd == -1 ? infoEnd : fieldEnd, KEYWORD);
                        }

                        infoStart += KEYWORD.length + 1;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * 转换 int 值
     * @param variant 变异位点序列
     * @param fieldStart 字段起点
     * @param fieldEnd 字段终点
     * @param KEYWORD 识别的关键字
     * @return 计算值，-1 表示不存在此字段
     */
    default int formatToIntegerValue(VolumeByteStream variant, int fieldStart, int fieldEnd, final byte[] KEYWORD) {
        if (variant.cacheOf(fieldStart) == ByteCode.MINUS) {
            // 说明是负数
            return -2;
        }

        int score = 0;
        for (int index = fieldStart; index < fieldEnd; index++) {
            if (variant.cacheOf(index) == 101 || variant.cacheOf(index) == 69) {
                // 确定为科学计数法
                return new BigDecimal(new String(variant.rangeOf(fieldStart, fieldEnd))).intValue();
            }

            if (variant.cacheOf(index) == ByteCode.PERIOD) {
                // 遇到 .，继续查看后续是否有可疑的信息

                if ((index == fieldStart) && (index + 1 == fieldEnd)) {
                    // 只有一个点数据
                    return -1;
                }

                index += 1;
                while (index < fieldEnd) {
                    if (variant.cacheOf(index) == 101 || variant.cacheOf(index) == 69) {
                        // 确定为科学计数法
                        return new BigDecimal(new String(variant.rangeOf(fieldStart, fieldEnd))).intValue();
                    }

                    index += 1;
                }
                break;
            }

            score = score * 10 + (variant.cacheOf(index) - 48);
        }

        return score;
    }

    /**
     * 重设控制器
     * @param variantQC 新控制器
     */
    void reset(IVariantQC variantQC);

    /**
     * 获取阈值
     */
    int getMethod();
}
