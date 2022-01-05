package edu.sysu.pmglab.suranyi.gbc.setup.windows;

import edu.sysu.pmglab.suranyi.compressor.ICompressor;
import edu.sysu.pmglab.suranyi.container.VolumeByteSafeOutputStream;
import edu.sysu.pmglab.suranyi.gbc.core.ITask;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleACController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleAFController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.allele.AlleleANController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.genotype.GenotypeDPController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.genotype.GenotypeGQController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantAllelesNumController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantDPController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantMQController;
import edu.sysu.pmglab.suranyi.gbc.core.common.qualitycontrol.variant.VariantPhredQualityScoreController;
import edu.sysu.pmglab.suranyi.gbc.core.common.switcher.ISwitcher;
import edu.sysu.pmglab.suranyi.unifyIO.partwriter.BGZOutputParam;
import edu.sysu.pmglab.suranyi.gbc.setup.command.EntryPoint;
import edu.sysu.pmglab.suranyi.threadPool.ThreadPool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Stack;

/**
 * @Data        :2021/07/12
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :GBC 图形界面模式
 */

public class MainFrame extends JFrame {
    /**
     * 主界面
     */
    JPanel MainPanel;
    JTabbedPane MainTabPanel;

    /**
     * 线程池
     */
    ThreadPool threadPool = new ThreadPool(1);

    /**
     * 选择模式标签页
     */
    JPanel SetupPanel;
    JTextField setupInputFileText;
    JButton buildInputFileChooserButton;
    JButton setupContinueButton;
    JComboBox<String> setupModeSelector;
    JButton setupResetButton;

    /**
     * build 模式标签页
     */
    JPanel BuildPanel;
    JTabbedPane buildParameterSettingTabPanel;
    JPanel buildMainSettingSubPanel;
    JTextField buildOutputFileText;
    JButton buildOutputFileChooserButton;
    JComboBox<Boolean> buildPhasedComboBox;
    JSpinner buildThreadsSpinner;
    JComboBox<String> buildCompressorComboBox;
    JSpinner buildCompressionLevelSpinner;
    JComboBox<Integer> buildBlockSizeComboBox;
    JComboBox<Boolean> buildReorderingComboBox;
    JSpinner buildWindowSizeSpinner;
    JButton buildResetButton;
    JCheckBox buildDisableQualityControlCheckBox;
    JButton buildRunButton;
    JButton buildCancelButton;

    /**
     * 质量控制标签页
     */
    JPanel QCPanel;
    JSpinner genotypeQualityControlGq;
    JSpinner genotypeQualityControlDp;
    JSpinner variantQualityControlPhredQualityScore;
    JSpinner variantQualityControlDp;
    JSpinner variantQualityControlMq;
    JSpinner variantQualityControlAllelesNum;
    JSpinner variantQualityControlACMin;
    JSpinner variantQualityControlACMax;
    JSpinner variantQualityControlAFMin;
    JSpinner variantQualityControlAFMax;
    JSpinner variantQualityControlAN;
    JButton qcResetButton;

    /**
     * Extract 模式标签页
     */
    JPanel ExtractPanel;
    JPanel extractMainParametersSubPanel;

    JTabbedPane extractParameterSettingTabPanel;
    JLabel extractOutputFileLabel;
    JTextField extractOutputFileText;
    JButton extractOutputFileChooserButton;
    JSpinner extractThreadsSpinner;
    JComboBox<String> extractPhasedComboBox;
    JCheckBox toBGZFCheckBox;
    JSpinner bgzipCompressionLevelSpinner;
    JCheckBox extractSubjectSelectionCheckBox;
    JComboBox<String> extractSiteSelectionComboBox;
    JCheckBox extractQualityControlCheckBox;
    JCheckBox hideGenotypeCheckBox;
    JButton extractResetButton;
    JButton extractRunButton;
    JButton extractCancelButton;

    JPanel extractSubjectSelectionSubPanel;
    JTextArea extractSubjectText;

    JPanel extractRangeOfPositionSubPanel;
    JComboBox<String> extractRangeChromosome;
    JSpinner extractRangeStartPos;
    JSpinner extractRangeEndPos;

    JPanel extractRandomAccessSubPanel;
    JTextField extractRandomAccessFileText;
    JButton extractRandomAccessFileChooserButton;

    JPanel extractDecompressByNodeSubPanel;
    JComboBox<String> extractNodeChromosome;
    JSpinner extractNodeIndex;
    JButton extractNodeAddButton;
    JButton openInGTBViewer;
    JTextArea extractNodeInfo;

    /**
     * Rebuild 模式标签页
     */
    JPanel RebuildPanel;

    /**
     * Merge 模式标签页
     */
    JPanel MergePanel;
    Stack<JPanel> panelStack = new Stack<>();

    /**
     * Edit 模式标签页
     */
    JPanel EditPanel;
    JLabel editOutputFileLabel;
    JTextField editOutputFileText;
    JButton editOutputFileButton;
    JCheckBox editSplitByChromosomeCheckBox;
    JButton editDeleteButton;
    JButton editRetainButton;
    JButton editUniqueButton;
    JTextField editOtherGTBFileText;
    JButton editConcatButton;
    JCheckBox editResetSubjectCheckBox;
    JTextArea resetSubjectText;
    JButton editResetButton;
    JButton editRunButton;
    JButton editCancelButton;
    JButton editOpenGTBTreeButton;
    GTBViewer GTBTreeViewerInstance;

    /**
     * Merge 模式标签页
     */
    JTabbedPane mergeParameterSettingTabPanel;
    JPanel mergeSourceSubPanel;
    JButton mergeFileAddButton;
    JTextField mergeSourceFileText;
    JTextArea allMergeFileText;
    JButton mergeRunButton;
    JButton mergeCancelButton;

    /**
     * Console 控制终端及日志记录器
     */
    JPanel ConsolePanel;
    JTextArea logRecorder;
    VolumeByteSafeOutputStream log;
    JTextField consoleCommandLine;
    JButton consoleRunButton;

    /**
     * rebuild 模式标签页
     */
    JTabbedPane rebuildParameterSettingTabPanel;
    JButton rebuildCancelButton;
    JButton rebuildRunButton;
    JCheckBox rebuildSubjectSelectionCheckBox;

    MainFrame() {
        setTitle("GBC Setup");

        pack();
        setResizable(false);
        setContentPane(MainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 设置窗口大小
        initSize(700, 500);

        // 添加监听器
        addListener();

        // 设为可见
        setVisible(true);
    }

    public static MainFrame launch() {
        // 启动
        return new MainFrame();
    }

    void addListener() {
        // 注册拖拽输入数据方式
        DropAdapter adapter = DropAdapter.getInstance(this);

        // 初始界面设定
        SetupFrameSetting.init(this);
        SetupFrameSetting.addDropAdapter(this, adapter);

        // build 界面设定
        BuildFrameSetting.init(this);
        BuildFrameSetting.addDropAdapter(this, adapter);

        // extract 界面设定
        ExtractFrameSetting.init(this);
        ExtractFrameSetting.addDropAdapter(this, adapter);

        // edit 界面设定
        EditFrameSetting.init(this);
        EditFrameSetting.addDropAdapter(this, adapter);

        // 质控面板设定
        QcFrameSetting.init(this);

        // merge 界面设定
        MergeFrameSetting.init(this);
        MergeFrameSetting.addDropAdapter(this, adapter);

        // rebuild 界面设定
        RebuildFrameSetting.init(this);
        RebuildFrameSetting.addDropAdapter(this, adapter);

        // console 窗口设置 Run 指令监听
        consoleRunButton.addActionListener(this::consoleRunButtonClicked);
    }

    /**
     * 命令行执行指令监听器
     */
    void consoleRunButtonClicked(ActionEvent e) {
        if (consoleCommandLine.getText().trim().equalsIgnoreCase("clear")) {
            logRecorder.setText("");
            consoleCommandLine.setText("");
        } else {
            // 提交任务
            String[] args = consoleCommandLine.getText().split(" ");
            consoleCommandLine.setText("");
            submitCommand(args);
        }
    }

    void submitCommand(String[] args) {
        submitCommand(args, true);
    }

    void submitCommand(String[] args, boolean submit) {
        // 运行按钮不可点击
        consoleRunButton.setEnabled(false);

        // 添加运行日志
        addConsoleLog(">>> " + String.join(" ", args) + "\n");

        // 提交任务
        try {
            if (submit) {
                try {
                    EntryPoint.run(args);
                } catch (Exception | Error e) {
                    error(e.getMessage());
                }
            }
        } finally {
            // 写入结果
            addConsoleLog(new String(Arrays.copyOfRange(log.getCache(), 0, log.size())));
            addConsoleLog("\n");
            log.reset();
            consoleRunButton.setEnabled(true);
        }
    }

    /**
     * 获取输入文件名
     */
    String getInputFileName() {
        return this.setupInputFileText.getText().trim();
    }

    /**
     * 初始化窗口大小
     */
    void initSize(int width, int height) {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        this.setBounds((dimension.width - width) >> 1, (dimension.height - height) >> 1,
                width, height);
    }

    public int error(String msg) {
        Object[] options = {"OK"};
        return JOptionPane.showOptionDialog(this, msg, "Error", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
    }

    public int info(String msg) {
        Object[] options = {"OK"};
        return JOptionPane.showOptionDialog(this, msg, "Information", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }

    public int warn(String msg) {
        Object[] options = {"Cancel", "Stop it"};
        return JOptionPane.showOptionDialog(this, msg, "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
    }

    /**
     * 添加标签页
     */
    void put(String title, JPanel panel) {
        JPanel currentPanel = (JPanel) MainTabPanel.getSelectedComponent();

        // 将上一个窗口设置为不可用
        setAllComponentEnable(currentPanel, false);

        // 往栈中添加该页面
        panelStack.push(currentPanel);

        // 添加新标签页
        MainTabPanel.insertTab(title, null, panel, null, MainTabPanel.getTabCount() - 1);

        // 自动跳转标签页
        MainTabPanel.setSelectedComponent(panel);
    }

    void skipToConsoleTab() {
        JPanel currentPanel = (JPanel) MainTabPanel.getSelectedComponent();

        // 将上一个窗口设置为不可用
        setAllComponentEnable(currentPanel, false);

        // 往栈中添加该页面
        panelStack.push(currentPanel);

        // 自动跳转标签页
        MainTabPanel.setSelectedComponent(ConsolePanel);
    }

    void pop() {
        // 将上一个窗口设置为可用
        JPanel lastPanel = panelStack.pop();
        setAllComponentEnable(lastPanel, true);

        // 移除当前标签页
        MainTabPanel.remove(MainTabPanel.getSelectedIndex());
        MainTabPanel.setSelectedComponent(lastPanel);
    }


    /**
     * 递归地设置所有组件不可用
     * @param enable 状态
     */
    void setAllComponentEnable(Component component, boolean enable) {
        if (component instanceof Container) {
            component.setEnabled(enable);

            if (!(component instanceof JComboBox || component instanceof JSpinner)) {
                for (Component subComponent : ((Container) component).getComponents()) {
                    setAllComponentEnable(subComponent, enable);
                }
            }

        } else {
            component.setEnabled(enable);
        }
    }

    /**
     * 添加控制台记录
     */
    void addConsoleLog(String log) {
        logRecorder.append(log);
        logRecorder.setCaretPosition(logRecorder.getText().trim().length());
    }

    /**
     * 自定义初始化组件
     */
    void createUIComponents() {
        // build 参数
        buildThreadsSpinner = new JSpinner(new SpinnerNumberModel(ITask.INIT_THREADS, 1,ITask.AVAILABLE_PROCESSORS, 1));
        buildCompressionLevelSpinner = new JSpinner(new SpinnerNumberModel(ICompressor.getDefaultCompressionLevel(ICompressor.DEFAULT), ICompressor.getMinCompressionLevel(ICompressor.DEFAULT), ICompressor.getMaxCompressionLevel(ICompressor.DEFAULT), 1));
        buildWindowSizeSpinner = new JSpinner(new SpinnerNumberModel(ISwitcher.DEFAULT_SIZE, ISwitcher.MIN, ISwitcher.MAX, 1));

        // 质控参数
        genotypeQualityControlDp = new JSpinner(new SpinnerNumberModel(GenotypeDPController.DEFAULT, GenotypeDPController.MIN, GenotypeDPController.MAX, 1));
        genotypeQualityControlGq = new JSpinner(new SpinnerNumberModel(GenotypeGQController.DEFAULT, GenotypeGQController.MIN, GenotypeGQController.MAX, 1));
        variantQualityControlDp = new JSpinner(new SpinnerNumberModel(VariantDPController.DEFAULT, VariantDPController.MIN, VariantDPController.MAX, 1));
        variantQualityControlPhredQualityScore = new JSpinner(new SpinnerNumberModel(VariantPhredQualityScoreController.DEFAULT, VariantPhredQualityScoreController.MIN, VariantPhredQualityScoreController.MAX, 1));
        variantQualityControlMq = new JSpinner(new SpinnerNumberModel(VariantMQController.DEFAULT, VariantMQController.MIN, VariantMQController.MAX, 1));
        variantQualityControlAllelesNum = new JSpinner(new SpinnerNumberModel(VariantAllelesNumController.DEFAULT, VariantAllelesNumController.MIN, VariantAllelesNumController.MAX, 1));
        variantQualityControlACMin = new JSpinner(new SpinnerNumberModel(AlleleACController.MIN, AlleleACController.MIN, AlleleACController.MAX, 1));
        variantQualityControlACMax = new JSpinner(new SpinnerNumberModel(AlleleACController.MAX, AlleleACController.MIN, AlleleACController.MAX, 1));
        variantQualityControlAFMin = new JSpinner(new SpinnerNumberModel(AlleleAFController.MIN, AlleleAFController.MIN, AlleleAFController.MAX, 0.001));
        variantQualityControlAFMax = new JSpinner(new SpinnerNumberModel(AlleleAFController.MAX, AlleleAFController.MIN, AlleleAFController.MAX, 0.001));
        variantQualityControlAN = new JSpinner(new SpinnerNumberModel(AlleleANController.DEFAULT, AlleleANController.MIN, AlleleANController.MAX, 1));

        // extract
        extractThreadsSpinner = new JSpinner(buildThreadsSpinner.getModel());
        bgzipCompressionLevelSpinner = new JSpinner(new SpinnerNumberModel(BGZOutputParam.DEFAULT_LEVEL, BGZOutputParam.MIN_LEVEL, BGZOutputParam.MAX_LEVEL, 1));
        extractRangeStartPos = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        extractRangeEndPos = new JSpinner(new SpinnerNumberModel(Integer.MAX_VALUE, 1, Integer.MAX_VALUE, 1));
        extractNodeIndex = new JSpinner(new SpinnerNumberModel(-1, -1, 8388608, 1));
    }
}