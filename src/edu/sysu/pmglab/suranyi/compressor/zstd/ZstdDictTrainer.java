package edu.sysu.pmglab.suranyi.compressor.zstd;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author suranyi
 * @description 训练压缩字典
 */

public class ZstdDictTrainer {
    private final SmartList<byte[]> trainingSamples;
    private final int dictMaxSize;
    private byte[] dict;

    public ZstdDictTrainer(int dictMaxSize) {
        trainingSamples = new SmartList<>(true);
        this.dictMaxSize = dictMaxSize;
    }

    public synchronized void addSamples(byte[]... samples) {
        this.trainingSamples.addAll(Arrays.asList(samples));
    }

    public synchronized void addSamples(VolumeByteStream... samples) {
        for (VolumeByteStream vbs: samples){
            this.trainingSamples.add(vbs.values());
        }
    }

    /**
     * 训练字典
     */
    public synchronized byte[] train() throws ZstdException {
        // 如果已经有训练完成的字典，则直接返回
        if (this.dict != null){
            return this.dict;
        }

        // 没有训练完成的字典 (或被清除)
        byte[] cache = new byte[this.dictMaxSize];

        // 补充不足的数据
        for (int i = 0, size = this.trainingSamples.size(); i < 7 - size; i++) {
            this.trainingSamples.add(new byte[]{});
        }

        // 训练字典
        int length = (int) Zstd.trainFromBuffer(this.trainingSamples.toArray(new byte[][]{}), cache);

        // -1 表示不需要字典
        if (length == -1){
            return new byte[]{};
        }

        // 检验是否发生错误
        if (Zstd.isError(length)){
            throw new ZstdException(length);
        }

        return this.dict = length < cache.length ? ArrayUtils.copyOfRange(cache, 0, length) : cache;
    }

    /**
     * 保存压缩字典表到指定的文件
     * @param fileName 文件名
     */
    public void save(String fileName) throws IOException {
        try (FileStream dictFile = new FileStream(fileName, FileOptions.CHANNEL_WRITER)) {
            dictFile.write(train());
        }
    }

    /**
     * 清除保存的训练数据
     */
    public void clear(){
        this.dict = null;
        this.trainingSamples.clear();
    }

    /**
     * 清除保存的字典
     */
    public void clearDict(){
        this.dict = null;
    }

    /**
     * 清除保存的训练样本
     */
    public void clearSamples(){
        this.trainingSamples.clear();
    }
}
