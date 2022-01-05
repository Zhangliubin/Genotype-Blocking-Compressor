package edu.sysu.pmglab.suranyi.gbc.coder.encoder;

/**
 * @Data        :2021/03/22
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :组合编码器
 */

enum UnPhasedGroupEncoder implements MBEGEncoder {
    /**
     * 单例模式组合编码器
     */
    INSTANCE;

    byte[][][][] encoder;

    UnPhasedGroupEncoder() {
        this.encoder = new byte[4][4][4][4];
        // 无向
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++) {
                    for (int l = 0; l < 4; l++) {
                        this.encoder[i][j][k][l] = (byte) (i * 64 + j * 16 + k * 4 + l);
                    }
                }
            }
        }
    }

    @Override
    public byte encode(byte code1) {
        return this.encoder[code1][code1][code1][code1];
    }

    @Override
    public byte encode(byte code1, byte code2) {
        return this.encoder[code1][code2][code2][code2];
    }

    @Override
    public byte encode(byte code1, byte code2, byte code3) {
        return this.encoder[code1][code2][code3][code3];
    }

    @Override
    public byte encode(byte code1, byte code2, byte code3, byte code4) {
        return this.encoder[code1][code2][code3][code4];
    }

    @Override
    public boolean isPhased() {
        return false;
    }

    static MBEGEncoder getInstance() {
        return INSTANCE;
    }
}
