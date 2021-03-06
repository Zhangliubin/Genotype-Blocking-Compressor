/**
 * @Data :2021/08/15
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :
 */

package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.compressor.IDecompressor;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.unifyIO.FileStream;

import java.io.IOException;
import java.util.Arrays;

class DecompressionCache {
    int chromosomeIndex = -2;
    int nodeIndex = -2;

    final VolumeByteStream undecompressedCache;
    final VolumeByteStream genotypesCache;
    final VolumeByteStream allelesPosCache;
    final FileStream fileStream;
    final IDecompressor decompressor;
    final TaskVariant[] taskVariants;
    boolean isGTDecompress;

    public DecompressionCache(GTBManager manager) throws IOException {
        this(manager, true);
    }

    public DecompressionCache(GTBManager manager, boolean decompressGT) throws IOException {
        if (decompressGT) {
            this.genotypesCache = new VolumeByteStream(manager.getMaxDecompressedMBEGsSize());
            this.allelesPosCache = new VolumeByteStream(manager.getMaxDecompressedAllelesSize());
            this.undecompressedCache = new VolumeByteStream(2 << 20);
            this.decompressor = IDecompressor.getInstance(manager.getCompressorIndex());

            this.taskVariants = new TaskVariant[manager.getBlockSize()];
            for (int i = 0; i < taskVariants.length; i++) {
                this.taskVariants[i] = new TaskVariant();
            }

            this.fileStream = manager.getFileStream();
        } else {
            this.genotypesCache = new VolumeByteStream(0);
            this.allelesPosCache = new VolumeByteStream(manager.getMaxDecompressedAllelesSize());
            this.undecompressedCache = new VolumeByteStream(2 << 20);
            this.decompressor = IDecompressor.getInstance(manager.getCompressorIndex());

            this.taskVariants = new TaskVariant[manager.getBlockSize()];
            for (int i = 0; i < taskVariants.length; i++) {
                this.taskVariants[i] = new TaskVariant();
            }

            this.fileStream = manager.getFileStream();
        }
    }

    public void fill(Pointer pointer) throws IOException {
        fill(pointer, true);
    }

    public void fill(Pointer pointer, boolean decompressGT) throws IOException {
        if ((pointer.nodeIndex != this.nodeIndex || pointer.node.chromosomeIndex != this.chromosomeIndex)) {
            // ????????????????????? block ???????????????????????????
            this.nodeIndex = pointer.nodeIndex;
            this.chromosomeIndex = pointer.node.chromosomeIndex;
            GTBNode node = pointer.getNode();

            undecompressedCache.makeSureCapacity(node.compressedAlleleSize, node.compressedGenotypesSize, node.compressedPosSize);

            /* ??????????????????????????? */
            undecompressedCache.reset();
            allelesPosCache.reset();
            this.fileStream.seek(node.blockSeek + node.compressedGenotypesSize);
            this.fileStream.read(undecompressedCache, node.compressedPosSize);
            decompressor.decompress(undecompressedCache, allelesPosCache);

            /* ????????????????????????????????? */
            int taskNums = node.numOfVariants();
            for (int i = 0; i < taskNums; i++) {
                this.taskVariants[i].setPosition(allelesPosCache.cacheOf(i << 2), allelesPosCache.cacheOf(1 + (i << 2)),
                                allelesPosCache.cacheOf(2 + (i << 2)), allelesPosCache.cacheOf(3 + (i << 2)))
                        .setIndex(i)
                        .setDecoderIndex(i < node.subBlockVariantNum[0] ? 0 : 1);
            }

            /* ?????? allele ??????????????? */
            undecompressedCache.reset();
            allelesPosCache.reset();
            this.fileStream.read(undecompressedCache, node.compressedAlleleSize);
            decompressor.decompress(undecompressedCache, allelesPosCache);

            /* ?????? allele ?????? */
            int lastIndex = 0;
            int startPos;
            int endPos = this.taskVariants[0].index == 0 ? -1 : 0;

            for (int i = 0; i < taskNums; i++) {
                startPos = allelesPosCache.indexOfN(ByteCode.SLASH, endPos, this.taskVariants[i].index - lastIndex);
                endPos = allelesPosCache.indexOfN(ByteCode.SLASH, startPos + 1, 1);
                this.taskVariants[i].setAlleleInfo(startPos + 1, endPos);

                // ?????? lastIndex ??????
                lastIndex = this.taskVariants[i].index + 1;
            }

            /* ?????? position ????????????????????? */
            Arrays.sort(this.taskVariants, 0, taskNums, TaskVariant::compareVariant);

            /* ?????? genotype ??????????????? */
            decompressGT(node, pointer, decompressGT);
        }
    }

    private void decompressGT(GTBNode node, Pointer pointer, boolean decompressGT) throws IOException {
        /* ?????? genotype ??????????????? */
        if (decompressGT) {
            undecompressedCache.reset();
            genotypesCache.reset();
            this.fileStream.seek(node.blockSeek);
            this.fileStream.read(undecompressedCache, node.compressedGenotypesSize);
            decompressor.decompress(undecompressedCache, genotypesCache);
            isGTDecompress = true;
        } else {
            isGTDecompress = false;
        }
    }

    public void close() throws IOException {
        undecompressedCache.close();
        genotypesCache.close();
        allelesPosCache.close();
        fileStream.close();
        decompressor.close();
    }
}