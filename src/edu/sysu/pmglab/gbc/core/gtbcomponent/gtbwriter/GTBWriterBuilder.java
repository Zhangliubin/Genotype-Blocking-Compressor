package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbwriter;

import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBReferenceManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBSubjectManager;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.gbc.core.build.IBuildTask;

import java.io.IOException;

/**
 * @author suranyi
 * @description
 */

public class GTBWriterBuilder extends IBuildTask {
    GTBSubjectManager subjectManager = new GTBSubjectManager();
    GTBReferenceManager referenceManager = new GTBReferenceManager();

    public GTBWriterBuilder(String outputFileName) {
        this.setOutputFileName(outputFileName);
    }

    public GTBWriter build() throws IOException {
        // 检查
        return new GTBWriter(this);
    }

    public GTBWriterBuilder setSubject(String... subjects) {
        subjectManager = new GTBSubjectManager();
        subjectManager.load(String.join("\t", subjects).getBytes());

        return this;
    }

    public GTBWriterBuilder setSubject(byte[] subjects) {
        subjectManager = new GTBSubjectManager();
        subjectManager.load(subjects);

        return this;
    }

    public GTBWriterBuilder setReference(String reference) {
        referenceManager = new GTBReferenceManager();
        referenceManager.load(reference);

        return this;
    }

    public GTBWriterBuilder setReference(VolumeByteStream reference) {
        referenceManager = new GTBReferenceManager();
        referenceManager.load(reference);

        return this;
    }
}
