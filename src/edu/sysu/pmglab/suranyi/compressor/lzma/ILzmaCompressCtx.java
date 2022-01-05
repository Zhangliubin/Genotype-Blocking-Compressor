package edu.sysu.pmglab.suranyi.compressor.lzma;

import java.io.IOException;

/**
 * @author suranyi
 * @description LZMA 压缩上下文抽象类
 */

abstract class ILzmaCompressCtx {
    /**
     * 压缩方法
     * @param src 原数据
     * @param srcOffset 源数据偏移量
     * @param srcLength 源数据有效长度
     * @param dst 目标数据容器
     * @param dstOffset 目标数据容器偏移量
     * @param dstLength 目标容器可用长度
     * @return 实际写入长度
     * @throws IOException IO 异常
     */
    abstract int compress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException;

    /**
     * 关闭压缩上下文
     */
    abstract void close();
}