package edu.sysu.pmglab.suranyi.gbc.coder.encoder;

/**
 * @Data        :2021/03/22
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :组合编码器
 */

enum UnPhasedEncoder implements BEGEncoder {
    /**
     * 单例模式组合编码器
     */
    INSTANCE;
    final byte[][] encodeDict = initEncodeDict(false);

    @Override
    public boolean isPhased() {
        return false;
    }

    @Override
    public byte encode(int i, int j) {
        return this.encodeDict[i][j];
    }

    static BEGEncoder getInstance(){
        return INSTANCE;
    }
}
