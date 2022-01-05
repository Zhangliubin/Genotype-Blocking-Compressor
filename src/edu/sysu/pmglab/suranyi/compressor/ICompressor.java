package edu.sysu.pmglab.suranyi.compressor;

import edu.sysu.pmglab.suranyi.compressor.empty.EmptyCompressor;
import edu.sysu.pmglab.suranyi.compressor.lzma.LzmaCompressor;
import edu.sysu.pmglab.suranyi.compressor.zstd.ZstdCompressor;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.check.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author suranyi
 * @description 压缩器抽象接口
 */

public abstract class ICompressor implements Closeable, AutoCloseable {
    /**
     * 请在此处注册方法 (追加元素)
     */
    static final Class<ICompressor>[] COMPRESSOR_ENABLE = new Class[]{ZstdCompressor.class, LzmaCompressor.class, EmptyCompressor.class};

    /**
     * 默认压缩器编号
     */
    public static final int DEFAULT = 0;

    /**
     * 输出缓冲区
     */
    protected final VolumeByteStream cache;

    /**
     * 获取压缩器实例
     * @param compressorIndex 压缩器索引
     * @param compressionLevel 压缩级别
     * @return 压缩器实例
     */
    public static ICompressor getInstance(int compressorIndex, int compressionLevel) {
        return getInstance(compressorIndex, compressionLevel, 0);
    }

    /**
     * 获取压缩器实例
     * @param compressorIndex 压缩器索引
     * @param compressionLevel 压缩级别
     * @param cacheSize 缓存数据大小
     * @return 压缩器实例
     */
    public static ICompressor getInstance(int compressorIndex, int compressionLevel, int cacheSize) {
        Assert.valueRange(cacheSize, 0, Integer.MAX_VALUE - 2);

        return getInstance(compressorIndex, compressionLevel, new VolumeByteStream(cacheSize));
    }

    /**
     * 获取压缩器实例
     * @param compressorIndex 压缩器索引
     * @param compressionLevel 压缩级别
     * @param cache 缓存区
     * @return 压缩器实例
     */
    public static ICompressor getInstance(int compressorIndex, int compressionLevel, VolumeByteStream cache) {
        try {
            return COMPRESSOR_ENABLE[compressorIndex].getConstructor(int.class, VolumeByteStream.class).newInstance(compressionLevel, cache);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }

        throw new IllegalArgumentException();
    }

    /**
     * 获取最大压缩级别
     * @param compressorIndex 压缩器索引
     * @return 最大压缩级别
     */
    public static int getMaxCompressionLevel(int compressorIndex) {
        try {
            return (int) COMPRESSOR_ENABLE[compressorIndex].getField("MAX_LEVEL").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }

        throw new IllegalArgumentException();
    }

    /**
     * 获取最小压缩级别
     * @param compressorIndex 压缩器索引
     * @return 最小压缩级别
     */
    public static int getMinCompressionLevel(int compressorIndex) {
        try {
            return (int) COMPRESSOR_ENABLE[compressorIndex].getField("MIN_LEVEL").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }

        throw new IllegalArgumentException();
    }

    /**
     * 获取默认压缩级别
     * @param compressorIndex 压缩器索引
     * @return 默认压缩级别
     */
    public static int getDefaultCompressionLevel(int compressorIndex) {
        try {
            return (int) COMPRESSOR_ENABLE[compressorIndex].getField("DEFAULT_LEVEL").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }

        throw new IllegalArgumentException();
    }

    /**
     * 获取压缩器索引
     * @param compressorName 压缩器名字
     * @return 压缩器索引
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
     * 获取压缩器名字
     * @param compressorIndex 压缩器索引
     * @return 压缩器名字
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
     * 获取压缩器名字
     * @return 压缩器名字
     */
    public static String[] getCompressorNames() {
        String[] names = new String[COMPRESSOR_ENABLE.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = getCompressorName(i);
        }
        return names;
    }

    /**
     * 获取压缩边界
     * @param compressorIndex 压缩器索引
     * @param uncompressedLength 输入数据长度
     * @return 最大压缩数据大小
     */
    public static int getCompressBound(int compressorIndex, int uncompressedLength) {
        Assert.that(uncompressedLength >= 0);

        try {
            return (int) COMPRESSOR_ENABLE[compressorIndex].getMethod("compressBound", int.class).invoke(null, uncompressedLength);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }

        throw new IllegalArgumentException();
    }

    /**
     * 一次性压缩数据
     * @param compressorIndex 压缩器索引
     * @param src 待压缩数据
     * @param srcOffset 源数据偏移量
     * @param srcLength 输入数据长度
     * @throws IOException IO 异常
     */
    public static VolumeByteStream compress(int compressorIndex, int compressionLevel, byte[] src, int srcOffset, int srcLength) throws IOException {
        try {
            return (VolumeByteStream) COMPRESSOR_ENABLE[compressorIndex].getMethod("compress", int.class, byte[].class, int.class, int.class).invoke(null, compressionLevel, src, srcOffset, srcLength);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }

        throw new IllegalArgumentException();
    }

    /**
     * 一次性压缩数据
     * @param compressorIndex 压缩器索引
     * @param src 待压缩数据
     * @param srcOffset 源数据偏移量
     * @param srcLength 输入数据长度
     * @return 最大压缩数据大小
     * @throws IOException IO 异常
     */
    public static VolumeByteStream compress(int compressorIndex, byte[] src, int srcOffset, int srcLength) throws IOException {
        try {
            return compress(compressorIndex, getDefaultCompressionLevel(compressorIndex), src, srcOffset, srcLength);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("undefined compressorIndex = " + compressorIndex);
        }
    }

    /**
     * 默认构造器方法
     */
    public ICompressor() {
        Assert.valueRange(getDefaultCompressionLevel(), getMinCompressionLevel(), getMaxCompressionLevel());
        this.cache = new VolumeByteStream(0);
    }

    /**
     * 构造器方法，指定压缩参数和缓冲区
     * @param compressionLevel 压缩器参数，一般来说值越小速度越快，越大则压缩比越高
     */
    public ICompressor(int compressionLevel) {
        Assert.valueRange(compressionLevel, getMinCompressionLevel(), getMaxCompressionLevel());
        this.cache = new VolumeByteStream(0);
    }

    /**
     * 构造器方法，指定压缩参数和缓冲区
     * @param compressionLevel 压缩器参数，一般来说值越小速度越快，越大则压缩比越高
     * @param cacheSize 缓冲区大小
     */
    public ICompressor(int compressionLevel, int cacheSize) {
        Assert.valueRange(compressionLevel, getMinCompressionLevel(), getMaxCompressionLevel());
        this.cache = new VolumeByteStream(cacheSize);
    }

    /**
     * 构造器方法，指定压缩参数和缓冲区
     * @param compressionLevel 压缩器参数，一般来说值越小速度越快，越大则压缩比越高
     * @param cache 缓冲区
     */
    public ICompressor(int compressionLevel, final VolumeByteStream cache) {
        Assert.valueRange(compressionLevel, getMinCompressionLevel(), getMaxCompressionLevel());
        this.cache = cache;
    }

    /**
     * 预估压缩后数据段大小
     * @param length 输入数据长度
     * @return 预估压缩后数据段大小
     */
    public abstract int getCompressBound(int length);

    /**
     * 获取最小压缩级别 (压缩参数)
     * @return 最小压缩级别
     */
    public abstract int getMinCompressionLevel();

    /**
     * 获取默认压缩级别 (压缩参数)
     * @return 默认压缩级别
     */
    public abstract int getDefaultCompressionLevel();

    /**
     * 获取最大压缩级别 (压缩参数)
     * @return 最大压缩级别
     */
    public abstract int getMaxCompressionLevel();

    /**
     * 压缩方法，压缩 src 数据，并写入到 dst 中
     * @param src 源数据
     * @param srcOffset 源数据偏移量
     * @param srcLength 源数据传入长度
     * @param dst 目标容器
     * @param dstOffset 目标容器偏移量
     * @param dstLength 目标容器可用长度
     * @return 写入数据长度，通常比 dst 小
     * @throws IOException IO 异常
     */
    public abstract int compress(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset, int dstLength) throws IOException;

    /**
     * 关闭压缩器方法
     */
    @Override
    public abstract void close();

    /**
     * 直接获取缓冲区
     * @return 缓冲区
     */
    public VolumeByteStream getCache() {
        return this.cache;
    }

    /**
     * 压缩方法，压缩 src 数据，并将压缩结果写入输出缓冲区
     * @param src 字节数组
     * @param offset 偏移量
     * @param length 长度
     * @return 输出缓冲区
     * @throws IOException IO 异常
     */
    public int compress(byte[] src, int offset, int length) throws IOException {
        return compress(src, offset, length, this.cache);
    }

    /**
     * 压缩方法
     * @param src 源数据
     * @return 写入数据长度
     * @throws IOException IO 异常
     */
    public int compress(VolumeByteStream src) throws IOException {
        return compress(src, this.cache);
    }

    /**
     * 压缩方法
     * @param src 源数据
     * @param dst 目标容器
     * @return 写入数据长度
     * @throws IOException IO 异常
     */
    public int compress(VolumeByteStream src, VolumeByteStream dst) throws IOException {
        return compress(src.getCache(), 0, src.size(), dst);
    }

    /**
     * 压缩方法，压缩 src 数据，并将压缩结果写入缓冲区或源数据容器
     * @param src 字节数组
     * @param offset 偏移量
     * @param length 长度
     * @param dst 目标容器
     * @return 写入数据长度
     * @throws IOException IO 异常
     */
    public int compress(byte[] src, int offset, int length, VolumeByteStream dst) throws IOException {
        int requestSize = getCompressBound(length);
        if (requestSize > dst.remaining()) {
            dst.expansionTo(dst.size() + requestSize);
        }

        int length0 = compress(src, offset, length, dst.getCache(), dst.size(), requestSize);
        dst.reset(dst.size() + length0);
        return length0;
    }

    /**
     * 重设缓冲区
     */
    public void reset() {
        this.cache.reset();
    }
}