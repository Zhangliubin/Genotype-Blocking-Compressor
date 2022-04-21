package edu.sysu.pmglab.gbc.setup.windows;

import edu.sysu.pmglab.compressor.ICompressor;
import edu.sysu.pmglab.gbc.core.build.BlockSizeParameter;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBRootCache;
import edu.sysu.pmglab.unifyIO.FileStream;

import javax.swing.text.JTextComponent;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;

/**
 * @Data        :2021/07/17
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :拖拽文件
 */

public class DropAdapter {
    public final MainFrame frame;
    public final DropTargetAdapter setPathAdapter;
    public final DropTargetAdapter appendPathAdapter;
    public final DropTargetAdapter appendPathsAdapter;
    public final DropTargetAdapter loadTextAdapter;
    public final DropTargetAdapter loadAndAppendTextAdapter;
    public final DropTargetAdapter buildParameterLoader;

    public static DropAdapter getInstance(MainFrame frame) {
        return new DropAdapter(frame);
    }

    private DropAdapter(MainFrame frame) {
        this.frame = frame;

        // 设置文件转换器
        this.setPathAdapter = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                if (e.getDropTargetContext().getComponent() instanceof JTextComponent) {
                    JTextComponent source = (JTextComponent) e.getDropTargetContext().getComponent();

                    try {
                        if (source.isEnabled() && e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            // 接受拖拽来的数据
                            e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                            List<File> list = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                            if (list.size() == 1) {
                                source.setText(list.get(0).getAbsolutePath());
                            } else {
                                // 拒绝拖拽来的数据
                                frame.error("Only a single file is allowed.");
                                e.rejectDrop();
                            }
                        } else {
                            // 拒绝拖拽来的数据
                            e.rejectDrop();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        };

        // 追加文件路径转换器
        this.appendPathAdapter = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                if (e.getDropTargetContext().getComponent() instanceof JTextComponent) {
                    JTextComponent source = (JTextComponent) e.getDropTargetContext().getComponent();

                    try {
                        if (source.isEnabled() && e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            // 接受拖拽来的数据
                            e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                            List<File> list = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                            if (list.size() == 1) {
                                source.setText(source.getText() + list.get(0).getAbsolutePath());
                            } else {
                                // 拒绝拖拽来的数据
                                frame.error("Only a single file is allowed.");
                                e.rejectDrop();
                            }
                        } else {
                            // 拒绝拖拽来的数据
                            e.rejectDrop();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        };

        // 追加文件路径转换器
        this.appendPathsAdapter = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                if (e.getDropTargetContext().getComponent() instanceof JTextComponent) {
                    JTextComponent source = (JTextComponent) e.getDropTargetContext().getComponent();

                    try {
                        if (source.isEnabled() && e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            // 接受拖拽来的数据
                            e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                            List<File> list = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                            String[] fileNames = new String[list.size()];
                            for (int i = 0; i < fileNames.length; i++) {
                                fileNames[i] = list.get(i).getAbsolutePath();
                            }
                            source.setText(source.getText() + String.join("\n", fileNames) + "\n");
                        }

                    } catch (Exception ignored) {
                    }
                }
            }
        };

        // 追加文件路径转换器
        this.loadTextAdapter = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                if (e.getDropTargetContext().getComponent() instanceof JTextComponent) {
                    JTextComponent source = (JTextComponent) e.getDropTargetContext().getComponent();

                    try {
                        if (source.isEnabled() && e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            // 接受拖拽来的数据
                            e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                            List<File> list = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                            if (list.size() == 1) {
                                FileStream fs = new FileStream(list.get(0).getAbsolutePath());
                                source.setText(new String(fs.readAll()));
                                source.setCaretPosition(0);
                            } else {
                                // 拒绝拖拽来的数据
                                frame.error("Only a single file is allowed.");
                                e.rejectDrop();
                            }
                        } else {
                            // 拒绝拖拽来的数据
                            e.rejectDrop();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        };

        // 追加文件路径转换器
        this.loadAndAppendTextAdapter = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                if (e.getDropTargetContext().getComponent() instanceof JTextComponent) {
                    JTextComponent source = (JTextComponent) e.getDropTargetContext().getComponent();

                    try {
                        if (source.isEnabled() && e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            // 接受拖拽来的数据
                            e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                            List<File> list = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                            if (list.size() == 1) {
                                FileStream fs = new FileStream(list.get(0).getAbsolutePath());
                                String text = new String(fs.readAll());
                                source.setText(source.getText() + text);
                                source.setCaretPosition(text.length());
                            } else {
                                // 拒绝拖拽来的数据
                                frame.error("Only a single file is allowed.");
                                e.rejectDrop();
                            }
                        } else {
                            // 拒绝拖拽来的数据
                            e.rejectDrop();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        };

        this.buildParameterLoader = new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        // 接受拖拽来的数据
                        e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        java.util.List<File> list = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        if (list.size() == 1) {
                            try {
                                GTBManager manager = GTBRootCache.get(list.get(0).getAbsolutePath());

                                frame.buildPhasedComboBox.setSelectedItem(manager.isPhased());
                                frame.buildBlockSizeComboBox.setSelectedItem(BlockSizeParameter.getBlockSize(manager.getBlockSizeType()));
                                frame.buildCompressorComboBox.setSelectedItem(ICompressor.getCompressorName(manager.getCompressorIndex()));
                                frame.buildCompressionLevelSpinner.setValue(manager.getCompressionLevel());
                                frame.info("Succeeded to load!");
                            } catch (Exception | Error exception) {
                                frame.error("Failed to load!");
                            }
                        }
                    } else {
                        // 拒绝拖拽来的数据
                        e.rejectDrop();
                    }
                } catch (Exception ignored) {
                }
            }
        };
    }
}