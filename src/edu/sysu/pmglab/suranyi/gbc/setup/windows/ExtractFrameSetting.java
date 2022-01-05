package edu.sysu.pmglab.suranyi.gbc.setup.windows;

import edu.sysu.pmglab.suranyi.container.SmartList;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.easytools.StringUtils;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.unifyIO.partwriter.BGZOutputParam;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

/**
 * @Data :2021/07/18
 * @Author :suranyi
 * @Contact :suranyi.sysu@gamil.com
 * @Description :构造模式设置
 */

public class ExtractFrameSetting {
    public static void init(MainFrame frame) {
        // 打开文件 按钮监听
        frame.extractOutputFileChooserButton.addActionListener(e -> fileChooserButtonClicked(frame, e));

        frame.toBGZFCheckBox.addActionListener(e -> toBgzipSelected(frame, e));

        frame.extractQualityControlCheckBox.addActionListener(e -> qualityControlClicked(frame, e));
        frame.extractSubjectSelectionCheckBox.addActionListener(e -> subjectSelectionSelected(frame, e));

        // 位点选择方法 (创建对应的子模式窗口)
        frame.extractSiteSelectionComboBox.addActionListener(e -> siteSelectionSelected(frame, e));
        // random access 子模式
        frame.extractRandomAccessFileChooserButton.addActionListener(e -> fileChooserButtonClicked(frame, e));
        // decompress by node 子模式
        frame.openInGTBViewer.addActionListener(e -> GTBViewer.show(frame));
        frame.extractNodeAddButton.addActionListener(e -> nodeAddButtonClicked(frame, e));

        // reset 按钮监听
        frame.extractResetButton.addActionListener(e -> resetButtonClicked(frame, e));
        // run 按钮监听
        frame.extractRunButton.addActionListener(e -> runButtonClicked(frame, e));
        // cancel 按钮监听
        frame.extractCancelButton.addActionListener(e -> frame.pop());
    }

    /**
     * 添加拖拽监听
     */
    public static void addDropAdapter(MainFrame frame, DropAdapter adapter) {
        new DropTarget(frame.extractOutputFileText, DnDConstants.ACTION_COPY_OR_MOVE, adapter.setPathAdapter);
        new DropTarget(frame.extractRandomAccessFileText, DnDConstants.ACTION_COPY_OR_MOVE, adapter.setPathAdapter);
        new DropTarget(frame.extractSubjectText, DnDConstants.ACTION_COPY_OR_MOVE, adapter.loadAndAppendTextAdapter);
    }

    /**
     * 文件选择器
     */
    static void fileChooserButtonClicked(MainFrame frame, ActionEvent e) {
        JButton jbt = (JButton) e.getSource();
        JFileChooser jfc = new JFileChooser();

        if (jbt == frame.extractOutputFileChooserButton) {
            jfc.addChoosableFileFilter(new FileNameExtensionFilter("GTB Format", "gtb"));
            jfc.addChoosableFileFilter(new FileNameExtensionFilter("VCF Format", "gz", "vcf"));
            jfc.setCurrentDirectory(new File(System.getProperty("user.dir")).getParentFile());

            jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                frame.extractOutputFileText.setText(jfc.getSelectedFile().getAbsolutePath());
            }
        } else if (jbt == frame.extractRandomAccessFileChooserButton) {
            jfc.setCurrentDirectory(new File(System.getProperty("user.dir")).getParentFile());

            jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                frame.extractRandomAccessFileText.setText(jfc.getSelectedFile().getAbsolutePath());
            }
        }

    }

    /**
     * 解压为 bgzip 格式监听器
     */
    static void toBgzipSelected(MainFrame frame, ActionEvent e) {
        if (frame.toBGZFCheckBox.isSelected()) {
            if (!frame.extractOutputFileText.getText().endsWith(".gz")) {
                if (frame.extractOutputFileText.getText().endsWith(".vcf")) {
                    frame.extractOutputFileText.setText(frame.extractOutputFileText.getText() + ".gz");
                } else {
                    frame.extractOutputFileText.setText(frame.extractOutputFileText.getText() + ".vcf.gz");
                }
            }
        } else {
            if (frame.extractOutputFileText.getText().endsWith(".vcf.gz")) {
                frame.extractOutputFileText.setText(frame.extractOutputFileText.getText().substring(0, frame.extractOutputFileText.getText().length() - 3));
            } else if (frame.extractOutputFileText.getText().endsWith(".gz")) {
                frame.extractOutputFileText.setText(frame.extractOutputFileText.getText().substring(0, frame.extractOutputFileText.getText().length() - 3) + ".vcf");
            }
        }
    }

    /**
     * 解压为 bgzip 格式监听器
     */
    static void subjectSelectionSelected(MainFrame frame, ActionEvent e) {
        if (frame.extractSubjectSelectionCheckBox.isSelected()) {
            frame.extractParameterSettingTabPanel.addTab("Subset of Subjects", frame.extractSubjectSelectionSubPanel);
        } else {
            frame.extractParameterSettingTabPanel.remove(frame.extractSubjectSelectionSubPanel);
        }
    }

    /**
     * 解压为 bgzip 格式监听器
     */
    static void siteSelectionSelected(MainFrame frame, ActionEvent e) {
        // 移除所有标签
        frame.extractParameterSettingTabPanel.remove(frame.extractRandomAccessSubPanel);
        frame.extractParameterSettingTabPanel.remove(frame.extractDecompressByNodeSubPanel);
        frame.extractParameterSettingTabPanel.remove(frame.extractRangeOfPositionSubPanel);
        switch ((String) Objects.requireNonNull(frame.extractSiteSelectionComboBox.getSelectedItem())) {
            case "Range":
                frame.extractParameterSettingTabPanel.addTab("Site Selection: Range of Position", frame.extractRangeOfPositionSubPanel);
                break;
            case "Random":
                frame.extractParameterSettingTabPanel.addTab("Site Selection: Random Access", frame.extractRandomAccessSubPanel);
                break;
            case "Node":
                frame.extractParameterSettingTabPanel.addTab("Site Selection: NodeIndex", frame.extractDecompressByNodeSubPanel);
                break;
            default:
                break;
        }
    }

    /**
     * 质控监听
     */
    static void qualityControlClicked(MainFrame frame, ActionEvent e) {
        if (frame.extractQualityControlCheckBox.isSelected()) {
            QcFrameSetting.reset(frame, frame.ExtractPanel);
            frame.extractParameterSettingTabPanel.addTab("Quality Control", frame.QCPanel);
        } else {
            frame.extractParameterSettingTabPanel.remove(frame.QCPanel);
        }
    }

    /**
     * 重设按钮监听
     */
    static void resetButtonClicked(MainFrame frame, ActionEvent e) {
        frame.toBGZFCheckBox.setSelected(false);
        toBgzipSelected(frame, null);

        frame.bgzipCompressionLevelSpinner.setValue(BGZOutputParam.DEFAULT_LEVEL);
        frame.extractThreadsSpinner.setValue(Math.min(Runtime.getRuntime().availableProcessors(), 4));
        frame.extractPhasedComboBox.setSelectedIndex(0);
        frame.extractSubjectSelectionCheckBox.setSelected(false);
        frame.extractQualityControlCheckBox.setSelected(false);
        frame.hideGenotypeCheckBox.setSelected(false);

        // 重设标签页信息
        frame.extractParameterSettingTabPanel.removeAll();
        frame.extractParameterSettingTabPanel.addTab("Main", frame.extractMainParametersSubPanel);
        frame.extractSiteSelectionComboBox.setSelectedIndex(0);
        frame.extractRandomAccessFileText.setText("");
        frame.extractSubjectText.setText("");
        frame.extractRangeChromosome.setSelectedItem(String.valueOf(1));
        frame.extractRangeStartPos.setValue(1);
        frame.extractRangeEndPos.setValue(Integer.MAX_VALUE);
        frame.extractNodeInfo.setText("");
        frame.extractNodeIndex.setValue(-1);
        frame.extractNodeChromosome.setSelectedItem(String.valueOf(1));
    }

    /**
     * 运行按钮监听
     */
    static void runButtonClicked(MainFrame frame, ActionEvent e) {
        // 检验输入信息
        if (frame.extractSubjectSelectionCheckBox.isSelected() && frame.extractSubjectText.getText().length() == 0) {
            frame.error("`Subject Selection` is selected, but no subjects are entered.");
            return;
        }

        // 运行子模式
        String[] runningMode;

        switch ((String) Objects.requireNonNull(frame.extractSiteSelectionComboBox.getSelectedItem())) {
            case "Range":
                runningMode = new String[]{"--range", frame.extractRangeChromosome.getSelectedItem() + ":" + frame.extractRangeStartPos.getValue() + "-" + frame.extractRangeEndPos.getValue()};
                break;
            case "Random":
                if (frame.extractRandomAccessFileText.getText().trim().length() == 0) {
                    frame.error("Please enter a position file.\nFormat: \n    1,10177\n    1,10178");
                    return;
                } else if (!FileUtils.exists(frame.extractRandomAccessFileText.getText())) {
                    frame.error("No such file: " + frame.extractRandomAccessFileText.getText());
                    return;
                } else if (FileUtils.isDirectory(frame.extractRandomAccessFileText.getText())) {
                    frame.error("Parse the folder: " + frame.extractRandomAccessFileText.getText());
                    return;
                } else {
                    runningMode = new String[]{"--random", frame.extractRandomAccessFileText.getText().trim()};
                    System.out.println((Arrays.toString(runningMode)));
                }
                break;
            case "Node":
                runningMode = ArrayUtils.merge(new String[]{"--node"}, frame.extractNodeInfo.getText().trim().replace(",", ";").replace(" ", "").replace("\n", " ").split(" "));
                if (runningMode.length != 2) {
                    frame.error("Format error: " + frame.extractRandomAccessFileText.getText());
                }
                break;
            default:
                // all 模式
                runningMode = new String[]{};
        }

        // 跳转到控制台
        frame.skipToConsoleTab();

        // 禁止点击 run 按钮
        frame.consoleRunButton.setEnabled(false);

        // 构建命令
        SmartList<String> command = new SmartList<>(128);

        // 根据用户设置生成命令
        StringUtils.addAll(command, "extract", frame.getInputFileName(), "-o", frame.extractOutputFileText.getText().trim()
                , frame.hideGenotypeCheckBox.isSelected() ? "-hg" : "");

        if (frame.extractOutputFileText.getText().trim().endsWith(".gz")) {
            frame.toBGZFCheckBox.setSelected(true);
        }

        if (frame.toBGZFCheckBox.isSelected()) {
            StringUtils.addAll(command, "--o-bgz", "-l", String.valueOf(frame.bgzipCompressionLevelSpinner.getValue()));
        } else {
            StringUtils.addAll(command, "--o-text");
        }

        if (frame.extractPhasedComboBox.getSelectedIndex() != 0) {
            StringUtils.addAll(command, "-p", frame.extractPhasedComboBox.getSelectedIndex() == 1 ? "true" : "false");
        }

        // 添加位点选择信息
        StringUtils.addAll(command, runningMode);

        if (frame.extractSubjectSelectionCheckBox.isSelected()) {
            StringUtils.addAll(command, "-s", frame.extractSubjectText.getText().trim());
        }

        if (frame.extractQualityControlCheckBox.isSelected()) {
            StringUtils.addAll(command, "--seq-ac", frame.variantQualityControlACMin.getValue() + "-" + frame.variantQualityControlACMax.getValue(),
                    "--seq-af", frame.variantQualityControlAFMin.getValue() + "-" + frame.variantQualityControlAFMax.getValue(),
                    "--seq-an", frame.variantQualityControlAN.getValue() + "-");
        }

        command.add("-y");

        // 提交命令
        frame.threadPool.submit(() -> {
                    // 子线程执行命令
                    frame.submitCommand(ArrayUtils.toStringArray(command));

                    // 执行完成后将上一个界面设置为可用
                    JPanel lastPanel = frame.panelStack.pop();
                    frame.setAllComponentEnable(lastPanel, true);

                    if (frame.extractQualityControlCheckBox.isSelected()) {
                        QcFrameSetting.reset(frame, frame.ExtractPanel);
                    }
                }
        );
    }

    /**
     * 节点添加按钮监听
     */
    static void nodeAddButtonClicked(MainFrame frame, ActionEvent actionEvent) {
        if ((int) frame.extractNodeIndex.getValue() == -1) {
            frame.extractNodeInfo.append("chrom=" + frame.extractNodeChromosome.getSelectedItem() + "\n");
        } else {
            frame.extractNodeInfo.append("chrom=" + frame.extractNodeChromosome.getSelectedItem() + ", node=" + frame.extractNodeIndex.getValue() + "\n");
        }
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

        /* 初始化参数检查 */
        frame.extractParameterSettingTabPanel.removeAll();
        frame.extractParameterSettingTabPanel.addTab("Main", frame.extractMainParametersSubPanel);
        if (frame.extractQualityControlCheckBox.isSelected()) {
            QcFrameSetting.reset(frame, frame.ExtractPanel);
            frame.extractParameterSettingTabPanel.addTab("Quality Control", frame.QCPanel);
        }
        if (frame.extractSubjectSelectionCheckBox.isSelected()) {
            frame.extractParameterSettingTabPanel.addTab("Subset of Subjects", frame.extractSubjectSelectionSubPanel);
        }

        /* 自动设置输出文件名 */
        frame.extractOutputFileText.setText(FileUtils.fixExtension(frame.getInputFileName(), ".vcf", ".gtb", ".vcf.gtb", ".vcf.gz.gtb"));
        siteSelectionSelected(frame, null);
        toBgzipSelected(frame, null);

        frame.put("Extract Parameters Setting", frame.ExtractPanel);
    }
}