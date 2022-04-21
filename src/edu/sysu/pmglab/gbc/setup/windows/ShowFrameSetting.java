package edu.sysu.pmglab.gbc.setup.windows;

import edu.sysu.pmglab.container.array.StringArray;
import edu.sysu.pmglab.easytools.StringUtils;

import javax.swing.*;

/**
 * @Data        :2021/07/18
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :构造模式设置
 */

public class ShowFrameSetting {
    /**
     * 添加界面标签，同时进行参数检查
     */
    public static void addTab(MainFrame frame) {
        // 跳转到控制台
        frame.skipToConsoleTab();

        // 禁止点击 run 按钮
        frame.consoleRunButton.setEnabled(false);

        // 构建命令
        StringArray command = new StringArray(128);

        // 根据用户设置生成命令
        StringUtils.addAll(command, "show", frame.getInputFileName(),
                "--full");

        // 提交命令
        frame.threadPool.submit(() -> {
                    frame.submitCommand(command.toArray(), false);

                    // 子线程执行命令
                    GTBViewer.show(frame);

                    // 执行完成后将上一个界面设置为可用
                    JPanel lastPanel = frame.panelStack.pop();
                    frame.setAllComponentEnable(lastPanel, true);

                    if (lastPanel.equals(frame.QCPanel)) {
                        QcFrameSetting.reset(frame, frame.BuildPanel);
                    }
                }
        );
    }
}