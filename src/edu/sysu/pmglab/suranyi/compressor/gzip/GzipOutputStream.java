package edu.sysu.pmglab.suranyi.compressor.gzip;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author suranyi
 * @description 可设置压缩级别的 Gzip 压缩器
 */

public class GzipOutputStream extends java.util.zip.GZIPOutputStream {
    public GzipOutputStream(OutputStream out) throws IOException {
        this(out, GzipCompressor.DEFAULT_LEVEL);
    }

    public GzipOutputStream(OutputStream out, int level) throws IOException {
        super(out);
        this.def.setLevel(level);
    }
}