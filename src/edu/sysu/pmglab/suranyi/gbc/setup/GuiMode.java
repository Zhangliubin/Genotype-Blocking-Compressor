package edu.sysu.pmglab.suranyi.gbc.setup;

import com.formdev.flatlaf.FlatLightLaf;
import edu.sysu.pmglab.suranyi.gbc.setup.command.EntryPoint;
import edu.sysu.pmglab.suranyi.gbc.setup.windows.MainFrame;

import javax.swing.*;
import java.awt.*;

/**
 * @Data        :2020/10/11
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :GUI 模式
 */

public class GuiMode {
    public static void run() {
        // 启动主界面
        try {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
            } catch (Exception ex) {
                System.out.println("Failed to initialize LaF");
            }

            MainFrame.launch();

            // Fatal 级别的重大异常
        } catch (OutOfMemoryError e) {
            System.out.println("FATAL-402    [OOM] Java heap space out of memory, please set the larger -Xms and -Xmx.");
            System.exit(402);
        } catch (HeadlessException e) {
            System.out.println("FATAL-401    No X11 DISPLAY variable was set, but this program performed an operation which requires it.");
            System.out.println(EntryPoint.INSTANCE.parser);
        }
    }
}
