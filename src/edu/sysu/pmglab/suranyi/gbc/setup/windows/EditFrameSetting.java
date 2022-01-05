package edu.sysu.pmglab.suranyi.gbc.setup.windows;

import edu.sysu.pmglab.suranyi.easytools.FileUtils;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.gbc.core.edit.EditTask;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * @Data        :2021/07/15
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :
 */


public class EditFrameSetting {
    public static void init(MainFrame frame) {
        // reset 按钮监听
        frame.editResetButton.addActionListener(e -> resetButtonClicked(frame, e));
        // run 按钮监听
        frame.editRunButton.addActionListener(e -> {
            try {
                runButtonClicked(frame, e);
            } catch (Exception exception) {
                frame.error(exception.getMessage());
            }
        });
        // cancel 按钮监听
        frame.editCancelButton.addActionListener(e -> cancelButtonClicked(frame, e));
        // 打开 GTB 树按钮
        frame.editOpenGTBTreeButton.addActionListener(e -> {
            if (frame.GTBTreeViewerInstance != null) {
                frame.GTBTreeViewerInstance.dispose();
            }
            frame.GTBTreeViewerInstance = GTBViewer.show(frame, true);
        });

        // 文件选择器按钮监听
        frame.editOutputFileButton.addActionListener(e -> fileChooserButtonClicked(frame, e));

        // edit 模式窗口监听
        frame.editSplitByChromosomeCheckBox.addActionListener(e -> splitByChromosomeCheckBoxSelected(frame, e));
        frame.editDeleteButton.addActionListener(e -> {
            try {
                frame.GTBTreeViewerInstance.deleteButtonClicked(e);
            } catch (Exception exception) {
                frame.error(exception.getMessage());
            }
        });
        frame.editRetainButton.addActionListener((e) -> {
            try {
                frame.GTBTreeViewerInstance.retainButtonClicked(e);
            } catch (Exception exception) {
                frame.error(exception.getMessage());
            }
        });
        frame.editUniqueButton.addActionListener((e) -> {
            try {
                frame.GTBTreeViewerInstance.uniqueButtonClicked(e);
            } catch (Exception exception) {
                frame.error(exception.getMessage());
            }
        });
        frame.editConcatButton.addActionListener((e) -> {
            try {
                concatFile(frame, e);
            } catch (Exception exception) {
                frame.error(exception.getMessage());
            }
        });
        frame.editResetSubjectCheckBox.addActionListener(e -> {
            try {
                resetSubjectCheckBoxSelected(frame, e);
            } catch (Exception exception) {
                frame.error(exception.getMessage());
            }
        });

        if (!frame.editResetSubjectCheckBox.isSelected()) {
            frame.resetSubjectText.setEnabled(false);
        }
    }

    /**
     * 重设样本名序列
     */
    static void resetSubjectCheckBoxSelected(MainFrame frame, ActionEvent e) {
        frame.resetSubjectText.setEnabled(frame.editResetSubjectCheckBox.isSelected());
        frame.resetSubjectText.setText("");
    }

    /**
     * 重设样本名序列
     */
    static void concatFile(MainFrame frame, ActionEvent e) {
        frame.GTBTreeViewerInstance.concatButtonClicked(e);
        frame.editOtherGTBFileText.setText("");
    }

    /**
     * 添加拖拽监听
     */
    public static void addDropAdapter(MainFrame frame, DropAdapter adapter) {
        new DropTarget(frame.editOutputFileText, DnDConstants.ACTION_COPY_OR_MOVE, adapter.setPathAdapter);
        new DropTarget(frame.editOtherGTBFileText, DnDConstants.ACTION_COPY_OR_MOVE, adapter.setPathAdapter);
    }

    /**
     * 文件选择器
     */
    static void fileChooserButtonClicked(MainFrame frame, ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("GTB Format (*.gtb)", "gtb"));
        jfc.addChoosableFileFilter(new FileNameExtensionFilter("VCF Format (*.vcf, *.gz)", "gz", "vcf"));
        jfc.setCurrentDirectory(new File(System.getProperty("user.dir")).getParentFile());

        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (jfc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            frame.editOutputFileText.setText(jfc.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * 按染色体编号提取文件监听器
     */
    static void splitByChromosomeCheckBoxSelected(MainFrame frame, ActionEvent e) {
        if (e.getSource() == frame.editSplitByChromosomeCheckBox) {
            if (frame.editSplitByChromosomeCheckBox.isSelected()) {
                frame.editOutputFileLabel.setText("Output Dir ");
                if (frame.editOutputFileText.getText().endsWith(".gtb")) {
                    frame.editOutputFileText.setText(frame.getInputFileName().substring(0, frame.getInputFileName().length() - 4));
                }
            } else {
                frame.editOutputFileLabel.setText("Output File");
                if (!frame.editOutputFileText.getText().endsWith(".gtb")) {
                    frame.editOutputFileText.setText(frame.editOutputFileText.getText() + ".gtb");
                }
            }
        }
    }

    /**
     * 重设按钮监听
     */
    static void resetButtonClicked(MainFrame frame, ActionEvent e) {
        GTBRootCache.clear(frame.getInputFileName());
        frame.GTBTreeViewerInstance.gtbManager = GTBRootCache.get(frame.getInputFileName());
        if (frame.GTBTreeViewerInstance != null) {
            frame.GTBTreeViewerInstance.flushTree();
        } else {
            frame.GTBTreeViewerInstance = GTBViewer.show(frame, true);
        }

        frame.editSplitByChromosomeCheckBox.setSelected(false);
        frame.editResetSubjectCheckBox.setSelected(false);
        frame.editOtherGTBFileText.setText("");
        frame.resetSubjectText.setText("");
    }

    /**
     * 运行按钮监听
     */
    static void runButtonClicked(MainFrame frame, ActionEvent e) {
        // 检查样本名
        if (frame.editResetSubjectCheckBox.isSelected()) {
            if (frame.resetSubjectText.getText().length() == 0) {
                frame.error("`Reset Subject` is selected, but no subjects are entered.");
                return;
            }
        }

        // 运行
        frame.GTBTreeViewerInstance.setEnabled(false);
        frame.setAllComponentEnable(frame.EditPanel, false);

        new Thread(() -> {
            try {
                // 检查输出文件夹是否存在
                if (frame.editSplitByChromosomeCheckBox.isSelected()) {
                    if (FileUtils.exists(frame.editOutputFileText.getText()) && FileUtils.isDirectory(frame.editOutputFileText.getText())) {
                        // 否则删除输出文件
                        FileUtils.delete(frame.editOutputFileText.getText());
                    }

                    // 重建输出文件
                    FileUtils.mkdir(frame.editOutputFileText.getText());
                }

                // 检查重设样本名
                if (frame.editResetSubjectCheckBox.isSelected()) {
                    frame.GTBTreeViewerInstance.task.resetSubjects(frame.resetSubjectText.getText().split(","));
                }

                frame.GTBTreeViewerInstance.task.setOutputFileName(frame.editOutputFileText.getText());
                frame.GTBTreeViewerInstance.task.split(frame.editSplitByChromosomeCheckBox.isSelected());
                frame.GTBTreeViewerInstance.task.submit();

                // 重新加载任务，避免先前数据造成干扰
                frame.GTBTreeViewerInstance.task = new EditTask(frame.getInputFileName());
                frame.GTBTreeViewerInstance.gtbManager = GTBRootCache.get(frame.getInputFileName());
                frame.GTBTreeViewerInstance.flushTree();
            } catch (Exception ioException) {
                frame.error(ioException.getMessage());
            } finally {
                frame.GTBTreeViewerInstance.setEnabled(true);
                frame.setAllComponentEnable(frame.EditPanel, true);

                if (!frame.editResetSubjectCheckBox.isSelected()) {
                    frame.resetSubjectText.setEnabled(false);
                }
            }
        }).start();
    }

    /**
     * 取消按钮监听
     */
    static void cancelButtonClicked(MainFrame frame, ActionEvent e) {
        frame.GTBTreeViewerInstance.dispose();
        frame.GTBTreeViewerInstance = null;
        GTBRootCache.clear(frame.getInputFileName());
        frame.pop();
    }

    /**
     * 添加界面标签
     */
    public static void addTab(MainFrame frame) {
        try {
            GTBRootCache.get(frame.getInputFileName());
        } catch (Exception e) {
            frame.error(e.getMessage());
            return;
        }

        // 自动设置输出文件名
        frame.editOutputFileText.setText(FileUtils.fixExtension(frame.getInputFileName(), ".gtb"));
        if (frame.editSplitByChromosomeCheckBox.isSelected()) {
            frame.editOutputFileText.setText(frame.getInputFileName().substring(0, frame.getInputFileName().length() - 4));
        }

        // 将 edit 模式窗口添加到主界面
        frame.put("Edit Parameters Setting", frame.EditPanel);

        // 创建 GTBTree 文件
        frame.GTBTreeViewerInstance = GTBViewer.show(frame, true);
    }
}