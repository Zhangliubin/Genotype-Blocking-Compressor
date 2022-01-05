package edu.sysu.pmglab.suranyi.gbc.core.extract;

import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.BEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.coder.decoder.MBEGDecoder;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.suranyi.unifyIO.partwriter.BGZOutputParam;
import edu.sysu.pmglab.suranyi.unifyIO.partwriter.IBlockWriter;

import java.nio.ByteBuffer;

/**
 * @Data        :2021/03/12
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :重构器接口
 */

abstract class IVCFFormatter {
    final IndexPair[] pairs;
    final AlleleQC formatter;
    final int totalSubjectNum;
    final byte[][] AN_TEMP;
    final int eachLineSize;
    BEGDecoder decoder;
    MBEGDecoder groupDecoder;
    IBlockWriter<ByteBuffer> outputStream;

    IVCFFormatter(BEGDecoder decoder, MBEGDecoder groupDecoder, IndexPair[] pairs, AlleleQC formatter, int totalSubjectNum, boolean originPhased, BGZOutputParam outputParam, int cacheSize) {
        this.decoder = decoder;
        this.groupDecoder = groupDecoder;
        this.pairs = pairs;
        this.formatter = formatter;
        this.totalSubjectNum = totalSubjectNum;
        this.AN_TEMP = new byte[][]{ValueUtils.stringValueOfAndGetBytes(pairs.length), ValueUtils.stringValueOfAndGetBytes(pairs.length << 1)};
        this.outputStream = IBlockWriter.getByteBufferInstance(outputParam, cacheSize);

        int eachCodeGenotypeNum = originPhased ? 3 : 4;
        int resBlockCodeGenotypeNum = totalSubjectNum % eachCodeGenotypeNum;
        this.eachLineSize = (this.totalSubjectNum / eachCodeGenotypeNum) + (resBlockCodeGenotypeNum == 0 ? 0 : 1);
    }

    ByteBuffer decode(VolumeByteStream genotypeEncodedStream, VolumeByteStream alleleStream, GTBNode node, TaskVariant[] task, int validTasksNum) {
        throw new UnsupportedOperationException("Invalid Exception");
    }

    void writeNoGenotype(VolumeByteStream alleleStream, int chromosomeIndex, TaskVariant task, int alleleCounts, int validAllelesNum) {
        // 换行符
        this.outputStream.write(ByteCode.NEWLINE);

        // CHROM 信息
        this.outputStream.write(ChromosomeInfo.getBytes(chromosomeIndex));
        this.outputStream.write(ByteCode.TAB);

        // pos 信息
        this.outputStream.write(task.positionInfo.getCache(), 0, task.positionInfo.size());
        this.outputStream.write(ByteCode.TAB);

        // id 信息
        this.outputStream.write(ByteCode.PERIOD);
        this.outputStream.write(ByteCode.TAB);

        // allele 信息
        this.outputStream.write(alleleStream.getCache(), task.alleleStart, task.alleleLength);
        this.outputStream.write(ByteCode.TAB);

        // qual 信息
        this.outputStream.write(ByteCode.PERIOD);
        this.outputStream.write(ByteCode.TAB);

        // filter 信息
        this.outputStream.write(ByteCode.PERIOD);
        this.outputStream.write(ByteCode.TAB);

        // info 信息
        this.outputStream.write(ByteCode.AC_STRING);
        this.outputStream.write(ValueUtils.stringValueOfAndGetBytes(alleleCounts));
        this.outputStream.write(ByteCode.AN_STRING);
        if (validAllelesNum == this.pairs.length) {
            this.outputStream.write(AN_TEMP[0]);
        } else if (validAllelesNum == (this.pairs.length << 1)) {
            this.outputStream.write(AN_TEMP[1]);
        } else {
            this.outputStream.write(ValueUtils.stringValueOfAndGetBytes(validAllelesNum));
        }

        this.outputStream.write(ByteCode.AF_STRING);
        if (validAllelesNum == 0) {
            this.outputStream.write(ByteCode.PERIOD);
        } else {
            this.outputStream.write(ValueUtils.stringValueOfAndGetBytes((double) alleleCounts / validAllelesNum, 6));
        }
        this.outputStream.write(ByteCode.TAB);

        // format 信息
        this.outputStream.write(ByteCode.GT_STRING);
    }

    void close() {
        this.outputStream.close();
    }
}
