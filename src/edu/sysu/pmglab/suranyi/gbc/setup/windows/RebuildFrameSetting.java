package edu.sysu.pmglab.suranyi.gbc.setup.windows;

import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.easytools.StringUtils;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

/**
 * @Data        :2021/07/18
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :构造模式设置
 */

public class RebuildFrameSetting {
    public static void init(MainFrame frame) {
        // 重设样本按键监听
        frame.rebuildSubjectSelectionCheckBox.addActionListener(e -> subjectSelectionCheckBoxSelected(frame, e));
        // run 按钮监听
        frame.rebuildRunButton.addActionListener(e -> runButtonClicked(frame, e));
        // cancel 按钮监听
        frame.rebuildCancelButton.addActionListener(e -> frame.pop());
    }

    /**
     * 添加拖拽监听
     */
    static void addDropAdapter(MainFrame frame, DropAdapter adapter) {
        new DropTarget(frame.rebuildParameterSettingTabPanel, DnDConstants.ACTION_COPY_OR_MOVE, adapter.buildParameterLoader);
    }

    static void subjectSelectionCheckBoxSelected(MainFrame frame, ActionEvent e) {
        if (frame.rebuildSubjectSelectionCheckBox.isSelected()) {
            frame.rebuildParameterSettingTabPanel.addTab("Subset of Subjects", frame.extractSubjectSelectionSubPanel);
        } else {
            frame.rebuildParameterSettingTabPanel.remove(frame.extractSubjectSelectionSubPanel);
        }
    }

    /**
     * 运行按钮监听
     */
    static void runButtonClicked(MainFrame frame, ActionEvent e) {
        if (frame.rebuildSubjectSelectionCheckBox.isSelected() && frame.extractSubjectText.getText().trim().length() == 0) {
            frame.error("`Reset Subject` is selected, but no subjects are entered.");
            return;
        }

        // 跳转到控制台
        frame.skipToConsoleTab();

        // 禁止点击 run 按钮
        frame.consoleRunButton.setEnabled(false);

        // 构建命令
        ArrayList<String> command = new ArrayList<>(128);

        // 根据用户设置生成命令
        StringUtils.addAll(command, "rebuild", frame.getInputFileName(),
                "-o", frame.buildOutputFileText.getText().trim(),
                "-t", String.valueOf(frame.buildThreadsSpinner.getValue()),
                "-c", (String) frame.buildCompressorComboBox.getSelectedItem(),
                "-bs", String.valueOf(frame.buildBlockSizeComboBox.getSelectedIndex() - 1),
                "-l", String.valueOf(frame.buildCompressionLevelSpinner.getValue()));

        if ((Boolean) frame.buildPhasedComboBox.getSelectedItem()) {
            command.add("-p");
        }

        if ((Boolean) frame.buildReorderingComboBox.getSelectedItem()) {
            command.add("-ws");
            command.add(String.valueOf(frame.buildWindowSizeSpinner.getValue()));
        } else {
            command.add("-nr");
        }

        if (frame.rebuildSubjectSelectionCheckBox.isSelected()) {
            StringUtils.addAll(command, "-s", frame.extractSubjectText.getText().trim());
        }

        StringUtils.addAll(command,
                "--seq-ac", frame.variantQualityControlACMin.getValue() + "-" + frame.variantQualityControlACMax.getValue(),
                "--seq-af", frame.variantQualityControlAFMin.getValue() + "-" + frame.variantQualityControlAFMax.getValue(),
                "--seq-an", frame.variantQualityControlAN.getValue() + "-",
                "--max-allele", String.valueOf(frame.variantQualityControlAllelesNum.getValue()), "-y");

        // 提交命令
        frame.threadPool.submit(() -> {
                    // 子线程执行命令
                    frame.submitCommand(ArrayUtils.toStringArray(command));

                    // 执行完成后将上一个界面设置为可用
                    frame.setAllComponentEnable(frame.panelStack.pop(), true);
                    QcFrameSetting.reset(frame, frame.RebuildPanel);
                }
        );
    }

    /**
     * 添加界面标签，同时进行参数检查
     */
    public static void addTab(MainFrame frame) {
        try {
            GTBRootCache.get(frame.getInputFileName());
        } catch (Exception e) {
            frame.error(e.getMessage());
            return;
        }

        // 设置 rebuild 参数窗口
        frame.rebuildParameterSettingTabPanel.removeAll();
        frame.rebuildParameterSettingTabPanel.addTab("Main", frame.buildMainSettingSubPanel);
        frame.rebuildParameterSettingTabPanel.addTab("Quality Control", frame.QCPanel);
        QcFrameSetting.reset(frame, frame.RebuildPanel);
        if (frame.rebuildSubjectSelectionCheckBox.isSelected()) {
            frame.rebuildParameterSettingTabPanel.addTab("Subset of Subjects", frame.extractSubjectSelectionSubPanel);
        }

        // 自动设置输出文件名
        frame.buildOutputFileText.setText(FileUtils.fixExtension(frame.getInputFileName(), ".gtb", ".vcf", ".vcf.gz", ".gz"));

        // 将 rebuild 模式窗口添加到主界面
        frame.put("Rebuild Parameters Setting", frame.RebuildPanel);
    }
}