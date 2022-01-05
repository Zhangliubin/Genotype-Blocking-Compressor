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

public class MergeFrameSetting {
    public static void init(MainFrame frame) {
        // 添加文件监听
        frame.mergeFileAddButton.addActionListener(e -> {
            if (frame.mergeSourceFileText.getText().trim().length() != 0 && FileUtils.exists(frame.mergeSourceFileText.getText().trim())) {
                frame.allMergeFileText.append(frame.mergeSourceFileText.getText().trim() + "\n");
            }
            frame.mergeSourceFileText.setText("");
        });
        // run 按钮监听
        frame.mergeRunButton.addActionListener(e -> runButtonClicked(frame, e));
        // cancel 按钮监听
        frame.mergeCancelButton.addActionListener(e -> frame.pop());
    }

    /**
     * 添加拖拽监听
     */
    public static void addDropAdapter(MainFrame frame, DropAdapter adapter) {
        new DropTarget(frame.allMergeFileText, DnDConstants.ACTION_COPY_OR_MOVE, adapter.appendPathsAdapter);
        new DropTarget(frame.mergeSourceFileText, DnDConstants.ACTION_COPY_OR_MOVE, adapter.setPathAdapter);
        new DropTarget(frame.mergeParameterSettingTabPanel, DnDConstants.ACTION_COPY_OR_MOVE, adapter.buildParameterLoader);
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
        ArrayList<String> command = new ArrayList<>(128);

        // 根据用户设置生成命令
        StringUtils.addAll(command, "merge", frame.getInputFileName());
        StringUtils.addAll(command, frame.allMergeFileText.getText().split("\n"));
        StringUtils.addAll(command,
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

        StringUtils.addAll(command,
                "--seq-ac", frame.variantQualityControlACMin.getValue() + "-" + frame.variantQualityControlACMax.getValue(),
                "--seq-af", frame.variantQualityControlAFMin.getValue() + "-" + frame.variantQualityControlAFMax.getValue(),
                "--seq-an", String.valueOf(frame.variantQualityControlAN.getValue() + "-"),
                "--max-allele", String.valueOf(frame.variantQualityControlAllelesNum.getValue()), "-y");

        // 提交命令
        frame.threadPool.submit(() -> {
                    // 子线程执行命令
                    frame.submitCommand(ArrayUtils.toStringArray(command));

                    // 执行完成后将上一个界面设置为可用
                    frame.setAllComponentEnable(frame.panelStack.pop(), true);
                    QcFrameSetting.reset(frame, frame.MergePanel);
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
        // 设置 build 参数窗口
        frame.mergeParameterSettingTabPanel.removeAll();
        frame.mergeParameterSettingTabPanel.addTab("Main", frame.buildMainSettingSubPanel);
        frame.mergeParameterSettingTabPanel.addTab("Quality Control", frame.QCPanel);
        QcFrameSetting.reset(frame, frame.MergePanel);
        frame.mergeParameterSettingTabPanel.addTab("Source", frame.mergeSourceSubPanel);

        // 自动设置输出文件名
        frame.buildOutputFileText.setText(FileUtils.fixExtension(frame.getInputFileName(), ".gtb"));

        // 将 build 模式窗口添加到主界面
        frame.put("Merge Parameters Setting", frame.MergePanel);
    }
}