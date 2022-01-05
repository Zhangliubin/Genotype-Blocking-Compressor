package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbwriter;

import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.suranyi.unifyIO.options.FileOptions;
import edu.sysu.pmglab.suranyi.unifyIO.FileStream;

import java.io.IOException;

/**
 * @Data        :2021/09/02
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :GTB 文件写入
 */

public class GTBWriter {
    private final FileStream gtb;
    private VolumeByteStream uncompressed;

    public GTBWriter(String fileName) throws IOException {
        this.gtb = new FileStream(fileName, FileOptions.CHANNEL_WRITER);
    }



    /**
     * 写入一个位点
     */
    public void write(Variant variant) {

    }
}