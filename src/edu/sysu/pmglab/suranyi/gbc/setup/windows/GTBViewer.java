package edu.sysu.pmglab.suranyi.gbc.setup.windows;

import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.gbc.constant.ChromosomeInfo;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.suranyi.gbc.core.edit.EditTask;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * @Data        :2020/10/26
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :GTB 访问器
 */

class GTBViewer extends JDialog {
    MainFrame parent;
    JTree viewerGTBTree;
    JPanel MainPanel;
    JTextArea viewerBaseInfoText;
    JScrollPane viewerBaseInfoPanel;

    DefaultMutableTreeNode root;
    boolean isDir;

    GTBManager gtbManager;
    EditTask task;

    private GTBViewer(MainFrame parent, boolean edit) {
        super(parent, "GTB Viewer", false);

        this.parent = parent;
        // 加载文件
        try {
            this.gtbManager = GTBRootCache.get(parent.getInputFileName());
        } catch (Exception | Error e) {
            parent.error(e.getMessage());
            return;
        }

        createTree();
        pack();
        setResizable(true);
        setContentPane(MainPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // 设置初始窗体位置
        initSize(700, 600);

        // 展开节点
        viewerGTBTree.expandRow(0);

        // 如果为编辑模式，则开启监听
        if (edit) {
            task = new EditTask(parent.getInputFileName());

            // 拖拽文件进行合并
            DropTargetAdapter adapter = new DropTargetAdapter() {
                @Override
                public void drop(DropTargetDropEvent e) {
                    try {
                        if (viewerGTBTree.isEnabled() && e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            // 接受拖拽来的数据
                            e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                            List<File> list = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                            String[] fileNames = new String[list.size()];
                            for (int i = 0; i < fileNames.length; i++) {
                                fileNames[i] = list.get(i).getAbsolutePath();
                            }
                            try {
                                concat(fileNames);
                            } catch (Exception exception) {
                                parent.error(exception.getMessage());
                            }
                        } else {
                            // 拒绝拖拽来的数据
                            e.rejectDrop();
                        }
                    } catch (Exception ignored) {
                    }
                }
            };
            new DropTarget(viewerGTBTree, DnDConstants.ACTION_COPY_OR_MOVE, adapter);
            new DropTarget(MainPanel, DnDConstants.ACTION_COPY_OR_MOVE, adapter);

            // 按钮监听
            viewerGTBTree.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (viewerGTBTree.isEnabled() && e.getKeyCode() == 8) {
                        try {
                            deleteButtonClicked(null);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                }
            });
            setVisible(true);
            viewerBaseInfoPanel.setVisible(false);
        } else {
            // 访问模式，则设置文件基本信息
            viewerBaseInfoText.setText(this.gtbManager.getManagerStringBuilder()
                    .listFileBaseInfo(true).listSummaryInfo(true).listSubjects(true).build());
            // 回滚指针
            viewerBaseInfoText.setCaretPosition(0);
            viewerBaseInfoText.setEditable(false);
            setVisible(true);
        }
    }

    public static GTBViewer show(MainFrame parent) {
        return new GTBViewer(parent, false);
    }

    public static GTBViewer show(MainFrame parent, boolean edit) {
        return new GTBViewer(parent, edit);
    }

    public void retainButtonClicked(ActionEvent e) {
        synchronized (this) {
            // 获取选中节点
            TreePath[] selectedPaths = viewerGTBTree.getSelectionPaths();

            if (selectedPaths != null) {
                // 按照路径的数量进行排序
                Arrays.sort(selectedPaths, Comparator.comparingInt(TreePath::getPathCount));

                // 先保留二级节点
                HashSet<String> retainAll = new HashSet<>(24);
                HashSet<String> retainChromosome = new HashSet<>(24);
                HashMap<String, ArrayList<Integer>> retainIndexes = new HashMap<>(24);

                for (TreePath path : selectedPaths) {
                    if (path.getPathCount() == 1) {
                        // 选中了根文件时，将保留全部
                        return;
                    } else {
                        String info = path.getPathComponent(1).toString();
                        String chromosome = info.substring(info.indexOf(" ") + 1, info.indexOf(":"));

                        // 先筛选出要保留全部的染色体
                        if (path.getPathCount() == 2) {
                            retainAll.add(chromosome);
                            retainChromosome.add(chromosome);
                        } else {
                            // 如果这个染色体数据是注定全部要保留的，则不添加
                            if (!retainAll.contains(chromosome)) {
                                String info2 = path.getPathComponent(2).toString();
                                String index = info2.substring(info2.indexOf(" ") + 1, info2.indexOf(":"));
                                retainChromosome.add(chromosome);
                                if (!retainIndexes.containsKey(chromosome)) {
                                    retainIndexes.put(chromosome, new ArrayList<>(32));
                                }

                                retainIndexes.get(chromosome).add(Integer.parseInt(index) - 1);
                            }
                        }
                    }
                }

                HashMap<Integer, int[]> retainNodes = new HashMap<>(retainChromosome.size());

                // 把需要保留的染色体留下来
                task.retain(ArrayUtils.toStringArray(retainChromosome));
                for (String chromosomeString : retainAll) {
                    int chromosomeIndex = ChromosomeInfo.getIndex(chromosomeString);
                    retainNodes.put(chromosomeIndex, null);
                }

                if (retainIndexes.size() > 0) {
                    for (String chromosomeString : retainIndexes.keySet()) {
                        int chromosomeIndex = ChromosomeInfo.getIndex(chromosomeString);
                        retainNodes.put(chromosomeIndex, ArrayUtils.toIntegerArray(retainIndexes.get(chromosomeString)));
                    }
                }

                task.retain(retainNodes);

                flushTree();
            }
        }
    }

    public void deleteButtonClicked(ActionEvent e) throws IOException {
        synchronized (this) {
            // 获取选中节点
            TreePath[] selectedPaths = viewerGTBTree.getSelectionPaths();

            if (selectedPaths != null) {
                // 先删除二级节点
                HashMap<String, ArrayList<TreePath>> threeLevelPaths = new HashMap<>(selectedPaths.length);
                for (TreePath path : selectedPaths) {
                    if (path.getPathCount() == 1) {
                        parent.error("Top-level nodes are not allowed to be deleted!");
                        return;
                    } else {
                        String info = path.getPathComponent(1).toString();
                        String chromosome = info.substring(info.indexOf(" ") + 1, info.indexOf(":"));

                        if (path.getPathCount() == 2) {
                            task.delete(chromosome);
                        } else {
                            if (!threeLevelPaths.containsKey(chromosome)) {
                                threeLevelPaths.put(chromosome, new ArrayList<>(32));
                            }
                            threeLevelPaths.get(chromosome).add(path);
                        }
                    }
                }

                // 再删除三级节点
                for (String chromosome : threeLevelPaths.keySet()) {
                    if (gtbManager.getGtbTree().contain(chromosome)) {
                        ArrayList<TreePath> paths = threeLevelPaths.get(chromosome);
                        int[] nodeIndexes = new int[paths.size()];
                        for (int i = 0; i < nodeIndexes.length; i++) {
                            String info = paths.get(i).getPathComponent(2).toString();
                            String index = info.substring(info.indexOf(" ") + 1, info.indexOf(":"));
                            nodeIndexes[i] = Integer.parseInt(index) - 1;
                        }

                        task.delete(chromosome, nodeIndexes);
                    }
                }
            }

            flushTree();
        }
    }

    public void concatButtonClicked(ActionEvent e) {
        synchronized (this) {
            task.concat(parent.editOtherGTBFileText.getText().trim());
            flushTree();
        }
    }

    public void concat(String... filenames)  {
        synchronized (this) {
            task.concat(filenames);
            flushTree();
        }
    }

    public void uniqueButtonClicked(ActionEvent e) {
        synchronized (this) {
            task.unique();
            flushTree();
        }
    }

    void initSize(int width, int height) {
        // 设置初始窗体位置
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        this.setBounds((dimension.width - width) >> 1, (dimension.height - height) >> 1,
                width, height);
    }

    /**
     * 创建树表
     */
    private void createTree() {
        root.setUserObject(gtbManager.getFileName());

        // 设置根节点为文件名
        int count;
        for (int chromosomeIndex : this.gtbManager.getChromosomeList()) {
            count = 1;
            DefaultMutableTreeNode chromNode = new DefaultMutableTreeNode(this.gtbManager.getGTBNodes(chromosomeIndex).getRootInfo());
            for (GTBNode nodes : this.gtbManager.getGTBNodes(chromosomeIndex)) {
                chromNode.add(new DefaultMutableTreeNode(String.format("Node %d: %s", count++, nodes)));
            }
            this.root.add(chromNode);
        }
    }

    void flushTree() {
        root.removeAllChildren();
        createTree();
        viewerGTBTree.cancelEditing();
        viewerGTBTree.clearSelection();
        viewerGTBTree.resetKeyboardActions();
        viewerGTBTree.updateUI();
        viewerGTBTree.expandRow(0);
    }

    private void createUIComponents() {
        root = new DefaultMutableTreeNode();
        viewerGTBTree = new JTree(root);
    }
}
