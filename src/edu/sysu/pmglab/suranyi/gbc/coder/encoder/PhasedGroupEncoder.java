package edu.sysu.pmglab.suranyi.gbc.coder.encoder;

/**
 * @Data        :2021/03/22
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :组合编码器
 */

enum PhasedGroupEncoder implements MBEGEncoder {
    /**
     * 单例模式组合编码器
     */
    INSTANCE;

    byte[][][] encoder;

    PhasedGroupEncoder() {
        this.encoder = new byte[5][5][5];

        // 有向
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 5; k++) {
                    this.encoder[i][j][k] = (byte) (i * 25 + j * 5 + k);
                }
            }
        }
    }

    @Override
    public byte encode(byte code1) {
        return this.encoder[code1][code1][code1];
    }

    @Override
    public byte encode(byte code1, byte code2) {
        return this.encoder[code1][code2][code2];
    }

    @Override
    public byte encode(byte code1, byte code2, byte code3) {
        return this.encoder[code1][code2][code3];
    }

    @Override
    public boolean isPhased() {
        return true;
    }

    static MBEGEncoder getInstance() {
        return INSTANCE;
    }
}
