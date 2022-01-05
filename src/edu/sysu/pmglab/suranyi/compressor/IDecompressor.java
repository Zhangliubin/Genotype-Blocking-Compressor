package edu.sysu.pmglab.suranyi.compressor;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.compressor.empty.EmptyDecompressor;
import edu.sysu.pmglab.suranyi.compressor.lzma.LzmaDecompressor;
import edu.sysu.pmglab.suranyi.compressor.zstd.ZstdDecompressor;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author suranyi
 * @description 解压器抽象接口
 */

public abstract class IDecompressor implements AutoCloseable {
    /**
     * 请在此处注册方法 (追加元素)，并确保其顺序与 ICompressor 中一致
     */
    public static final Class<IDecompressor>[] COMPRESSOR_ENABLE = new Class[]{ZstdDecompressor.class, LzmaDecompressor.class, EmptyDecompressor.class};

    /**
     * 获取解压器实例
     * @param compressorIndex 解压器索引，0 代表 zstd，1 代表 lzma
     * @return 解压器实例
     */
    public static IDecompressor getInstance(int compressorIndex) {
        return getInstance(compressorIndex, 0);
    }

    /**
     * 获取解压器实例
     * @param compressorIndex 解压器索引，0 代表 zstd，1 代表 lzma
     * @param cacheSize 缓存数据大小
     * @return 解压器实例
     */
    public static IDecompressor getInstance(int compressorIndex, int cacheSize) {
        Assert.valueRange(cacheSize, 0, Integer.MAX_VALUE - 2);
        return getInstance(compressorIndex, new VolumeByteStream(cacheSize));
    }

    /**
     * 获取解压器实例
     * @param compressorIndex 解压器索引，0 代表 zstd，1 代表 lzma
     * @param cache 缓存区
     * @return 解压器实例
     */
    public static IDecompressor getInstance(int compressorIndex, VolumeByteStream cache) {
        try {
            return COMPRESSOR_ENABLE[compressorIndex].getConstructor(VolumeByteStream.class).newInstance(cache);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }

        throw new IllegalArgumentException();
    }

    /**
     * 获取解压器索引
     * @param compressorName 解压器名字
     * @return 解压器索引
     */
    public static int getCompressorIndex(String compressorName) {
        try {
            for (int i = 0; i < COMPRESSOR_ENABLE.length; i++) {
                if (((String) COMPRESSOR_ENABLE[i].getField("COMPRESSOR_NAME").get(null)).equalsIgnoreCase(compressorName)) {
                    return i;
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        throw new IllegalArgumentException("undefined compressor = " + compressorName);
    }

    /**
     * 获取解压器名字
     * @param compressorIndex 解压器索引
     * @return 解压器名字
     */
    public static String getCompressorName(int compressorIndex) {
        try {
            return (String) COMPRESSOR_ENABLE[compressorIndex].getField("COMPRESSOR_NAME").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }

        throw new IllegalArgumentException();
    }

    /**
     * 获取压缩边界
     * @param compressorIndex 压缩器索引
     * @param src 输入数据
     * @param offset 偏移量
     * @param length 有效数据长度
     * @return 原数据大小
     */
    public static int getDecompressBound(int compressorIndex, byte[] src, int offset, int length) {
        try {
            return (int) COMPRESSOR_ENABLE[compressorIndex].getMethod("decompressBound", byte[].class, int.class, int.class).invoke(null, src, offset, length);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }

        throw new IllegalArgumentException();
    }

    /**
     * 一次性解压缩数据
     * @param compressorIndex 解压器索引
     * @param src 待解压缩数据
     * @param srcOffset 源数据偏移量
     * @param srcLength 输入数据长度
     * @throws IOException IO 异常
     */
    public static VolumeByteStream decompress(int compressorIndex, byte[] src, int srcOffset, int srcLength) throws IOException {
        try {
            return (VolumeByteStream) COMPRESSOR_ENABLE[compressorIndex].getMethod("decompress", byte[].class, int.class, int.class).invoke(null, src, srcOffset, srcLength);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }

        throw new IllegalArgumentException();
    }

    /**
     * 一次性解压缩数据
     * @param compressorIndex 解压器索引
     * @param src 待解压缩数据
     * @throws IOException IO 异常
     */
    public static VolumeByteStream decompress(int compressorIndex, VolumeByteStream src) throws IOException {
        return decompress(compressorIndex, src.getCache(), 0, src.size());
    }

    /**
     * 输出缓冲区
     */
    protected final VolumeByteStream cache;

    /**
     * 构造器方法
     */
    public IDecompressor() {
        this.cache = new VolumeByteStream(0);
    }

    /**
     * 构造器方法
     * @param cacheSize 缓冲区大小
     */
    public IDecompressor(int cacheSize) {
        this.cache = new VolumeByteStream(cacheSize);
    }

    /**
     * 构造器方法
     * @param cache 传入外部缓冲区
     */
    public IDecompressor(VolumeByteStream cache) {
        this.cache = cache;
    }

    /**
     * 预估解压大小
     * @param src 源数据
     * @param offset 偏移量
     * @param length 输入数据长度
     * @return 预估压缩大小
     */
    public abstract int getDecompressBound(byte[] src, int offset, int length);

    /**
     * 解压缩方法，解压 src 数据，并写入到 dst 中
     * @param src 源数据
     * @param srcOffset 源数据偏移量
     * @param srcLength 源数据传入长度
     * @param dst 目标容器
     * @param dstOffset 目标容器偏移量
     * @param dstLength 目标容器可用长度
     * @return 写入数据长度，通常比 dst 长度小
     * @throws IOException IO 异常
     */
    public abstract int decompress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException;

    /**
     * 关闭压缩器方法
     */
    @Override
    public abstract void close();

    /**
     * 直接获取缓冲区
     * @return 缓冲区
     */
    public final VolumeByteStream getCache() {
        return this.cache;
    }

    /**
     * 解压缩方法
     * @param src 源数据
     * @return 写入数据长度
     */
    public final int decompress(VolumeByteStream src) throws IOException {
        return decompress(src, this.cache);
    }

    /**
     * 解压缩方法
     * @param src 源数据
     * @param dst 目标容器
     * @return 写入数据长度
     */
    public final int decompress(VolumeByteStream src, VolumeByteStream dst) throws IOException {
        return decompress(src.getCache(), 0, src.size(), dst);
    }

    /**
     * 解压缩方法，压缩 src 数据，并将压缩结果写入缓冲区或源数据容器
     * @param src 字节数组
     * @param offset 偏移量
     * @param length 长度
     * @param dst 目标容器
     * @return 写入数据长度
     */
    public final int decompress(byte[] src, int offset, int length, VolumeByteStream dst) throws IOException {
        int requestSize = getDecompressBound(src, offset, length);
        if (requestSize > dst.remaining()) {
            dst.expansionTo(dst.size() + requestSize);
        }

        int length0 = decompress(src, offset, length, dst.getCache(), dst.size(), requestSize);
        dst.reset(dst.size() + length0);
        return length0;
    }

    /**
     * 重设缓冲区
     */
    public final void reset() {
        this.cache.reset();
    }
}