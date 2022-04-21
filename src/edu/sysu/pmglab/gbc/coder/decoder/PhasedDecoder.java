package edu.sysu.pmglab.gbc.coder.decoder;

/**
 * @Data        :2021/03/22
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :有向组合解码器
 */

enum PhasedDecoder implements BEGDecoder {
    /**
     * 单例模式组合解码器
     */
    INSTANCE;
    final byte[][][] decodeDict = initDecodeDict(true);

    @Override
    public byte[] decode(int ploidy, int code) {
        return decodeDict[ploidy][code];
    }

    @Override
    public boolean isPhased() {
        return true;
    }

    static BEGDecoder getInstance() {
        return INSTANCE;
    }
}
