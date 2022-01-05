package edu.sysu.pmglab.suranyi.gbc.setup.windows;

import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.easytools.StringUtils;
import edu.sysu.pmglab.suranyi.gbc.coder.CoderConfig;
import edu.sysu.pmglab.suranyi.gbc.core.build.BlockSizeParameter;
import edu.sysu.pmglab.suranyi.gbc.core.common.switcher.ISwitcher;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Objects;

/**
 * @Data        :2021/07/18
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :构造模式设置
 */

public class BuildFrameSetting {
    public static void init(MainFrame frame) {
        // 初始化参数
        frame.buildBlockSizeComboBox.setModel(new DefaultComboBoxModel<>(BlockSizeParameter.getSupportedBlockSizes()));
        frame.buildCompressorComboBox.setModel(new DefaultComboBoxModel<>(ICompressor.getCompressorNames()));
        frame.buildPhasedComboBox.setModel(new DefaultComboBoxModel<>(new Boolean[]{CoderConfig.DEFAULT_PHASED_STATUS, !CoderConfig.DEFAULT_PHASED_STATUS}));
        frame.buildReorderingComboBox.setModel(new DefaultComboBoxModel<>(new Boolean[]{ISwitcher.DEFAULT_ENABLE, !ISwitcher.DEFAULT_ENABLE}));
        frame.buildBlockSizeComboBox.setSelectedItem(BlockSizeParameter.DEFAULT_BLOCK_SIZE_TYPE);
        frame.buildCompressorComboBox.setSelectedIndex(ICompressor.DEFAULT);

        // 选项监听
        frame.buildCompressorComboBox.addActionListener(e -> setCompressionLevelModel(frame, e));
        frame.buildDisableQualityControlCheckBox.addActionListener(e -> disableQualityControlSelected(frame, e));

        // 打开文件
        frame.buildOutputFileChooserButton.addActionListener(e -> fileChooserButtonClicked(frame, e));
        // reset 按钮监听
        frame.buildResetButton.addActionListener(e -> resetButtonClicked(frame, e));
        // run 按钮监听
        frame.buildRunButton.addActionListener(e -> runButtonClicked(frame, e));
        // cancel 按钮监听
        frame.buildCancelButton.addActionListener(e -> frame.pop());
    }

    /**
     * 添加拖拽监听
     */
    public static void addDropAdapter(MainFrame frame, DropAdapter adapter) {
        new DropTarget(frame.buildOutputFileText, DnDConstants.ACTION_COPY_OR_MOVE, adapter.setPathAdapter);
        new DropTarget(frame.buildParameterSettingTabPanel, DnDConstants.ACTION_COPY_OR_MOVE, adapter.buildParameterLoader);
    }

    /**
     * 文件选择器
     */
    static void fileChooserButtonClicked(MainFrame frame, ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("GTB Format", "gtb"));
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("VCF Format", "gz", "vcf"));
        jfc.setCurrentDirectory(new File(System.getProperty("user.dir")).getParentFile());

        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            frame.buildOutputFileText.setText(jfc.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * 重设按钮监听
     */
    static void resetButtonClicked(MainFrame frame, ActionEvent e) {
        frame.buildPhasedComboBox.setSelectedItem(CoderConfig.DEFAULT_PHASED_STATUS);
        frame.buildThreadsSpinner.setValue(Math.min(Runtime.getRuntime().availableProcessors(), 4));
        frame.buildCompressorComboBox.setSelectedIndex(ICompressor.DEFAULT);
        frame.buildCompressionLevelSpinner.setValue(ICompressor.getDefaultCompressionLevel(ICompressor.DEFAULT));
        frame.buildBlockSizeComboBox.setSelectedItem(-1);
        frame.buildReorderingComboBox.setSelectedItem(ISwitcher.DEFAULT_ENABLE);
        frame.buildWindowSizeSpinner.setValue(ISwitcher.DEFAULT_SIZE);
    }

    /**
     * 运行按钮监听
     */
    static void runButtonClicked(MainFrame frame, ActionEvent e) {
        // 跳转到控制台
        frame.skipToConsoleTab();

        // 禁止点击 run 按钮
        frame.consoleRunButton.setEnabled(false);

        // 构建命令
        SmartList<String> command = new SmartList<>(128);

        // 根据用户设置生成命令
        StringUtils.addAll(command, "build", frame.getInputFileName(),
                "-o", frame.buildOutputFileText.getText().trim(),
                "-t", String.valueOf(frame.buildThreadsSpinner.getValue()),
                "-c", (String) frame.buildCompressorComboBox.getSelectedItem(),
                "-l", String.valueOf(frame.buildCompressionLevelSpinner.getValue()),
                "-bs", String.valueOf(frame.buildBlockSizeComboBox.getSelectedIndex() - 1)
                );

        if ((Boolean) frame.buildPhasedComboBox.getSelectedItem()) {
            command.add("-p");
        }

        if ((Boolean) frame.buildReorderingComboBox.getSelectedItem()) {
            command.add("-ws");
            command.add(String.valueOf(frame.buildWindowSizeSpinner.getValue()));
        } else {
            command.add("-nr");
        }

        if (frame.buildDisableQualityControlCheckBox.isSelected()) {
            StringUtils.addAll(command, "--no-qc", "-y");
        } else {
            StringUtils.addAll(command, "--gty-gq", String.valueOf(frame.genotypeQualityControlGq.getValue()),
                    "--gty-dp", String.valueOf(frame.genotypeQualityControlDp.getValue()),
                    "--seq-qual", String.valueOf(frame.variantQualityControlPhredQualityScore.getValue()),
                    "--seq-dp", String.valueOf(frame.variantQualityControlDp.getValue()),
                    "--seq-mq", String.valueOf(frame.variantQualityControlMq.getValue()),
                    "--seq-ac", frame.variantQualityControlACMin.getValue() + "-" + frame.variantQualityControlACMax.getValue(),
                    "--seq-af", frame.variantQualityControlAFMin.getValue() + "-" + frame.variantQualityControlAFMax.getValue(),
                    "--seq-an", frame.variantQualityControlAN.getValue() + "-",
                    "--max-allele", String.valueOf(frame.variantQualityControlAllelesNum.getValue()), "-y");
        }

        // 提交命令
        frame.threadPool.submit(() -> {
                    // 子线程执行命令
                    frame.submitCommand(ArrayUtils.toStringArray(command));

                    // 执行完成后将上一个界面设置为可用
                    JPanel lastPanel = frame.panelStack.pop();
                    frame.setAllComponentEnable(lastPanel, true);

                    if (!frame.buildDisableQualityControlCheckBox.isSelected()) {
                        QcFrameSetting.reset(frame, frame.BuildPanel);
                    }
                }
        );
    }

    /**
     * 选定质量控制
     */
    static void disableQualityControlSelected(MainFrame frame, ActionEvent e) {
        if (frame.buildDisableQualityControlCheckBox.isSelected()) {
            frame.buildParameterSettingTabPanel.remove(frame.QCPanel);
        } else {
            QcFrameSetting.reset(frame, frame.BuildPanel);
            frame.buildParameterSettingTabPanel.addTab("Quality Control", frame.QCPanel);
        }
    }

    /**
     * 添加界面标签，同时进行参数检查
     */
    public static void addTab(MainFrame frame) {
        // 设置 build 参数窗口
        frame.buildParameterSettingTabPanel.removeAll();
        frame.buildParameterSettingTabPanel.addTab("Main", frame.buildMainSettingSubPanel);

        // 质量控制方法
        if (!frame.buildDisableQualityControlCheckBox.isSelected()) {
            QcFrameSetting.reset(frame, frame.BuildPanel);
            frame.buildParameterSettingTabPanel.addTab("Quality Control", frame.QCPanel);
        }

        // 自动设置输出文件名
        frame.buildOutputFileText.setText(FileUtils.fixExtension(frame.getInputFileName(), ".gtb", ".vcf", ".vcf.gz", ".gz"));

        // 将 build 模式窗口添加到主界面
        frame.put("Build Parameters Setting", frame.BuildPanel);
    }

    /**
     * 设置压缩器级别
     */
    static void setCompressionLevelModel(MainFrame frame, ActionEvent e) {
        int compressorIndex = ICompressor.getCompressorIndex((String) frame.buildCompressorComboBox.getSelectedItem());

        frame.buildCompressionLevelSpinner.setModel(new SpinnerNumberModel(ICompressor.getDefaultCompressionLevel(compressorIndex),
                ICompressor.getMinCompressionLevel(compressorIndex),
                ICompressor.getMaxCompressionLevel(compressorIndex),
                1));
    }
}