package edu.sysu.pmglab.gbc.core.build;

import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.easytools.FileUtils;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.allele.AlleleQC;
import edu.sysu.pmglab.gbc.core.common.qualitycontrol.variant.VariantQC;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.GTBReader;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader.Variant;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBWriter;
import edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter.GTBWriterBuilder;

import java.io.IOException;

/**
 * @author suranyi
 * @description 文件标准化 (位点拆分与合并)
 */

class NormKernel {
    final VariantQC variantQC;
    final AlleleQC alleleQC;
    final RebuildTask task;
    final boolean toMultiallelic;

    NormKernel(RebuildTask task, boolean toMultiallelic) throws IOException {
        this.task = task;
        this.variantQC = this.task.variantQC;
        this.alleleQC = this.task.alleleQC;
        this.toMultiallelic = toMultiallelic;

        if (toMultiallelic) {
            convertToMultiallelicVariants();
        } else {
            convertToBiallelicVariants();
        }
    }

    public static void submit(RebuildTask task, boolean toMultiallelic) throws IOException {
        new NormKernel(task, toMultiallelic);
    }

    void convertToBiallelicVariants() throws IOException {
        GTBReader reader = new GTBReader(this.task.getInputFile(), this.task.getInputFile().isPhased());
        if (this.task.getSubjects() != null) {
            reader.selectSubjects(this.task.getSubjects());
        }
        GTBWriterBuilder writerBuilder = new GTBWriterBuilder(this.task.isInplace() ? this.task.getOutputFileName() + ".~$temp" : this.task.getOutputFileName());
        writerBuilder.setPhased(this.task.isPhased());
        writerBuilder.setSubject(reader.getSelectedSubjects());
        writerBuilder.setReference(reader.getManager().getReference());
        writerBuilder.setCompressor(this.task.getCompressor(), this.task.getCompressionLevel());
        writerBuilder.setReordering(this.task.isReordering());
        writerBuilder.setWindowSize(this.task.getWindowSize());
        writerBuilder.setBlockSizeType(this.task.getBlockSizeType());
        writerBuilder.setThreads(this.task.getThreads());
        GTBWriter writer = writerBuilder.build();

        // 元信息
        GTBManager manager = reader.getManager();
        BaseArray<Variant> variants = null;
        BaseArray<Variant> variantsCache = new Array<>();

        variantsCache.add(new Variant());

        for (int chromosomeIndex : this.task.getInputFile().getChromosomeList()) {
            boolean condition = false;

            if (manager.contain(chromosomeIndex)) {
                reader.limit(chromosomeIndex);
                reader.seek(chromosomeIndex, 0);
                variants = reader.readVariants(variantsCache);
                condition = variants != null;
            }

            while (condition) {
                for (Variant variant : variants) {
                    if (variant.getAlternativeAlleleNum() == 2) {
                        writeToFile(variant, writer);
                    } else {
                        writeToFile(variant.split(), writer);
                    }
                }
                variantsCache.addAll(variants);
                variants.clear();
                variants = reader.readVariants(variantsCache);
                condition = variants != null;
            }
        }

        reader.close();
        writer.close();

        if (this.task.isInplace()) {
            FileUtils.rename(writerBuilder.getOutputFileName(), this.task.getOutputFileName());
        }
    }

    void convertToMultiallelicVariants() throws IOException {
        GTBReader reader = new GTBReader(this.task.getInputFile(), this.task.getInputFile().isPhased());
        if (this.task.getSubjects() != null) {
            reader.selectSubjects(this.task.getSubjects());
        }
        GTBWriterBuilder writerBuilder = new GTBWriterBuilder(this.task.isInplace() ? this.task.getOutputFileName() + ".~$temp" : this.task.getOutputFileName());
        writerBuilder.setPhased(this.task.isPhased());
        writerBuilder.setSubject(reader.getSelectedSubjects());
        writerBuilder.setReference(reader.getManager().getReference());
        writerBuilder.setCompressor(this.task.getCompressor(), this.task.getCompressionLevel());
        writerBuilder.setReordering(this.task.isReordering());
        writerBuilder.setWindowSize(this.task.getWindowSize());
        writerBuilder.setBlockSizeType(this.task.getBlockSizeType());
        writerBuilder.setThreads(this.task.getThreads());
        GTBWriter writer = writerBuilder.build();

        // 元信息
        GTBManager manager = reader.getManager();
        Array<Variant> variants = null;
        Array<Variant> variantsCache = new Array<>();

        variantsCache.add(new Variant());

        for (int chromosomeIndex : this.task.getInputFile().getChromosomeList()) {
            boolean condition = false;

            if (manager.contain(chromosomeIndex)) {
                reader.limit(chromosomeIndex);
                reader.seek(chromosomeIndex, 0);
                variants = reader.readVariants(variantsCache);
                condition = variants != null;
            }

            while (condition) {
                writeToFile(Variant.join(variants), writer);
                variantsCache.addAll(variants);
                variants.clear();
                variants = reader.readVariants(variantsCache);
                condition = variants != null;
            }
        }

        reader.close();
        writer.close();

        if (this.task.isInplace()) {
            FileUtils.rename(writerBuilder.getOutputFileName(), this.task.getOutputFileName());
        }
    }

    void writeToFile(Variant variant, GTBWriter writer) throws IOException {
        if (this.variantQC.filter(null, variant.getAlternativeAlleleNum(), 0, 0, 0, 0)) {
            return;
        }

        if (this.alleleQC.size() > 0 && this.alleleQC.filter(variant.getAC(), variant.getAN())) {
            return;
        }

        writer.write(variant);
    }

    void writeToFile(BaseArray<Variant> variants, GTBWriter writer) throws IOException {
        for (Variant variant : variants) {

            if (this.variantQC.filter(null, variant.getAlternativeAlleleNum(), 0, 0, 0, 0)) {
                return;
            }

            if (this.alleleQC.size() > 0 && this.alleleQC.filter(variant.getAC(), variant.getAN())) {
                return;
            }

            writer.write(variant);
        }
    }
}
