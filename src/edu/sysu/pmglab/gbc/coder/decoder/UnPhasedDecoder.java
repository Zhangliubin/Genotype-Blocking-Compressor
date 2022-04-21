package edu.sysu.pmglab.gbc.coder.decoder;

/**
 * @Data        :2021/03/22
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :无向解码器
 */

enum UnPhasedDecoder implements BEGDecoder {
    /**
     * 单例模式解码器
     */
    INSTANCE;

    final byte[][][] decodeDict = initDecodeDict(false);

    @Override
    public byte[] decode(int ploidy, int code) {
        return decodeDict[ploidy][code];
    }

    @Override
    public boolean isPhased() {
        return false;
    }

    static BEGDecoder getInstance() {
        return INSTANCE;
    }
}
