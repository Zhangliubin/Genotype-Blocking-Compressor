package edu.sysu.pmglab.suranyi.gbc.setup.windows;

import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleACController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleAFController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleANController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.genotype.GenotypeDPController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.genotype.GenotypeGQController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantAllelesNumController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantDPController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantMQController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantPhredQualityScoreController;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @Data        :2021/07/18
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :质控窗口设置
 */

public class QcFrameSetting {
    public static void init(MainFrame frame) {
        // 初始化参数
        frame.qcResetButton.addActionListener(e -> resetButtonClicked(frame, e));
    }

    /**
     * 重设按钮监听
     */
    static void resetButtonClicked(MainFrame frame, ActionEvent e) {
        frame.genotypeQualityControlDp.setValue(GenotypeDPController.DEFAULT);
        frame.genotypeQualityControlGq.setValue(GenotypeGQController.DEFAULT);
        frame.variantQualityControlDp.setValue(VariantDPController.DEFAULT);
        frame.variantQualityControlPhredQualityScore.setValue(VariantPhredQualityScoreController.DEFAULT);
        frame.variantQualityControlMq.setValue(VariantMQController.DEFAULT);
        frame.variantQualityControlAllelesNum.setValue(VariantAllelesNumController.DEFAULT);
        frame.variantQualityControlACMin.setValue(AlleleACController.MIN);
        frame.variantQualityControlACMax.setValue(AlleleACController.MAX);
        frame.variantQualityControlAFMin.setValue(AlleleAFController.MIN);
        frame.variantQualityControlAFMax.setValue(AlleleAFController.MAX);
        frame.variantQualityControlAN.setValue(AlleleANController.DEFAULT);
    }

    /**
     * 质控面板设置，根据不同的源设置不同的可用参数
     */
    static void reset(MainFrame frame, JPanel currentPanel) {
        if (frame.BuildPanel.equals(currentPanel)) {
            // Build 模式调用了质控面板
            frame.genotypeQualityControlGq.setEnabled(true);
            frame.genotypeQualityControlDp.setEnabled(true);
            frame.variantQualityControlPhredQualityScore.setEnabled(true);
            frame.variantQualityControlDp.setEnabled(true);
            frame.variantQualityControlMq.setEnabled(true);
            frame.variantQualityControlAllelesNum.setEnabled(true);
        } else if (frame.ExtractPanel.equals(currentPanel)) {
            // extract 模式调用了质控面板
            frame.genotypeQualityControlGq.setEnabled(false);
            frame.genotypeQualityControlDp.setEnabled(false);
            frame.variantQualityControlPhredQualityScore.setEnabled(false);
            frame.variantQualityControlDp.setEnabled(false);
            frame.variantQualityControlMq.setEnabled(false);
            frame.variantQualityControlAllelesNum.setEnabled(false);
        } else if (frame.RebuildPanel.equals(currentPanel) || frame.MergePanel.equals(currentPanel)) {
            // rebuild 模式 或 merge 调用了质控面板
            frame.genotypeQualityControlGq.setEnabled(false);
            frame.genotypeQualityControlDp.setEnabled(false);
            frame.variantQualityControlPhredQualityScore.setEnabled(false);
            frame.variantQualityControlDp.setEnabled(false);
            frame.variantQualityControlMq.setEnabled(false);
            frame.variantQualityControlAllelesNum.setEnabled(true);
        }

        frame.variantQualityControlACMin.setEnabled(true);
        frame.variantQualityControlACMax.setEnabled(true);
        frame.variantQualityControlAFMin.setEnabled(true);
        frame.variantQualityControlAFMax.setEnabled(true);
        frame.variantQualityControlAN.setEnabled(true);
    }
}