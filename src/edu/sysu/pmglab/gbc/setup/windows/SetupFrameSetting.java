package edu.sysu.pmglab.gbc.setup.windows;

import edu.sysu.pmglab.container.VolumeByteSafeOutputStream;
import edu.sysu.pmglab.easytools.FileUtils;
import edu.sysu.pmglab.easytools.MD5;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBRootCache;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.PrintStream;
import java.util.Objects;

/**
 * @Data        :2021/07/18
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :启动窗口参数设置 (包含 console 指令)
 */

public class SetupFrameSetting {
    public static void init(MainFrame frame) {
        // 主标签界面移除所有的子标签
        frame.MainTabPanel.removeAll();

        // 为主标签界面添加 选择模式、控制台
        frame.MainTabPanel.add("Select Mode", frame.SetupPanel);
        frame.MainTabPanel.add("Console", frame.ConsolePanel);

        // 输入文件框回车键监听
        frame.setupInputFileText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (frame.setupInputFileText.isEnabled() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    frame.setupContinueButton.doClick();
                }
            }
        });

        // 设置监听日志
        frame.logRecorder.setLineWrap(true);
        frame.logRecorder.setWrapStyleWord(false);
        frame.log = new VolumeByteSafeOutputStream(2 << 20);
        PrintStream stream = new PrintStream(frame.log);
        System.setOut(stream);

        // 为命令模式添加回车键监听
        frame.consoleCommandLine.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (frame.consoleCommandLine.isEnabled() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    frame.consoleRunButton.doClick();
                }
            }
        });

        // reset 按钮监听
        frame.setupResetButton.addActionListener(e -> resetButtonClicked(frame, e));
        // 打开文件
        frame.buildInputFileChooserButton.addActionListener(e -> fileChooserButtonClicked(frame, e));
        // continue 按钮监听
        frame.setupContinueButton.addActionListener(e -> runButtonClicked(frame, e));
    }

    /**
     * 添加拖拽监听
     */
    public static void addDropAdapter(MainFrame frame, DropAdapter adapter) {
        new DropTarget(frame.setupInputFileText, DnDConstants.ACTION_COPY_OR_MOVE, adapter.setPathAdapter);
        new DropTarget(frame.consoleCommandLine, DnDConstants.ACTION_COPY_OR_MOVE, adapter.appendPathAdapter);
    }

    /**
     * 重设按钮监听
     */
    static void resetButtonClicked(MainFrame frame, ActionEvent e) {
        GTBRootCache.clear();
        MD5.clear();
        frame.setupInputFileText.setText("");
        frame.setupModeSelector.setSelectedItem("Build");
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
            frame.setupInputFileText.setText(jfc.getSelectedFile().getAbsolutePath());
        }
    }

    /**
     * 重设按钮监听
     */
    static void runButtonClicked(MainFrame frame, ActionEvent e) {
        // 查看文件是否存在
        if (frame.getInputFileName().length() == 0) {
            frame.error("Please enter input files.");
            return;
        }

        // 去除首位空格
        frame.setupInputFileText.setText(frame.setupInputFileText.getText().trim());

        if (!FileUtils.exists(frame.getInputFileName())) {
            frame.error("No such file or directory: " + frame.getInputFileName());
            return;
        }

        switch ((String) Objects.requireNonNull(frame.setupModeSelector.getSelectedItem())) {
            case "Build":
                BuildFrameSetting.addTab(frame);
                break;
            case "Show":
                ShowFrameSetting.addTab(frame);
                break;
            case "Extract":
                ExtractFrameSetting.addTab(frame);
                break;
            case "Merge":
                MergeFrameSetting.addTab(frame);
                break;
            case "Edit":
                EditFrameSetting.addTab(frame);
                break;
            case "Rebuild":
                RebuildFrameSetting.addTab(frame);
                break;
            default:
        }
    }
}