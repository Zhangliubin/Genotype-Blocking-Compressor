package edu.sysu.pmglab.gbc.coder;

import edu.sysu.pmglab.gbc.coder.decoder.MBEGDecoder;

/**
 * @Data        :2020/10/08
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :BEG 编码转换器
 */

public enum BEGTransfer {
    /* 单例模式转换器 */
    INSTANCE;

    /**
     * 转码器、组合编码器、组合解码器
     */
    private final byte[] unphasedTransfer = initTransfer();
    private final byte[][] reverser = new byte[][]{new byte[]{0, 3, 2, 1}, new byte[]{0, 3, 4, 1, 2}};
    private final MBEGDecoder[] groupDecoders = MBEGDecoder.getDecoders();

    /**
     * 转码
     * @param code 编码值
     */
    public static byte toUnphased(byte code) {
        return INSTANCE.unphasedTransfer[code & 0xFF];
    }

    /**
     * 转码
     * @param code 编码值
     */
    public static byte toUnphased(int code) {
        return INSTANCE.unphasedTransfer[code];
    }

    /**
     * 翻转 allele 顺序
     * @param code 编码值
     */
    public static byte reverse(boolean phased, byte code) {
        return INSTANCE.reverser[phased ? 1 : 0][code];
    }

    /**
     * 翻转 allele 顺序
     * @param code 编码值
     */
    public static byte reverse(boolean phased, int code) {
        return INSTANCE.reverser[phased ? 1 : 0][code];
    }

    /**
     * 翻转 allele 顺序
     * @param code 编码值
     */
    public static byte reverse(int phasedIndex, byte code) {
        return INSTANCE.reverser[phasedIndex][code];
    }

    /**
     * 翻转 allele 顺序
     * @param code 编码值
     */
    public static byte reverse(int phasedIndex, int code) {
        return INSTANCE.reverser[phasedIndex][code];
    }

    /**
     * 将 code 解码
     * @param groupDecoderIndex 组合解码器索引, 0 表示 unphased, 1 表示 phased
     */
    public static byte groupDecode(int groupDecoderIndex, int code, int codeIndex) {
        return INSTANCE.groupDecoders[groupDecoderIndex].decode(code, codeIndex);
    }

    /**
     * 将 code 解码
     * @param groupDecoderIndex 组合解码器索引, 0 表示 unphased, 1 表示 phased
     */
    public static byte groupDecode(int groupDecoderIndex, byte code, int codeIndex) {
        return INSTANCE.groupDecoders[groupDecoderIndex].decode(code & 0xFF, codeIndex);
    }

    /**
     * 初始化转码表
     */
    private byte[] initTransfer(){
        byte[] transfer = new byte[CoderConfig.MAX_ALLELE_NUM * CoderConfig.MAX_ALLELE_NUM + 1];

        for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
            for (int j = i; j < CoderConfig.MAX_ALLELE_NUM; j++) {
                transfer[CoderConfig.mapGenotypeTo(i, j)] = (byte) CoderConfig.mapGenotypeTo(i, j);
            }
        }

        for (int i = 0; i < CoderConfig.MAX_ALLELE_NUM; i++) {
            for (int j = 0; j < i; j++) {
                transfer[CoderConfig.mapGenotypeTo(i, j)] = (byte) CoderConfig.mapGenotypeTo(j, i);
            }
        }
        return transfer;
    }
}
