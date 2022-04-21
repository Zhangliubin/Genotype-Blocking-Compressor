package edu.sysu.pmglab.gbc.core.extract;

import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNode;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBNodes;

import java.util.Arrays;
import java.util.Map;

/**
 * @Data        :2021/03/09
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :任务生成器
 */

class TaskGenerator {
    public static void decompressAll(ExtractKernel kernel) {
        try {
            for (GTBNodes nodes : kernel.gtbManager.getGtbTree()) {
                for (GTBNode node : nodes) {
                    kernel.inputPipeline.put(true, TaskGTBNode.of(node));
                }
            }
        } catch (InterruptedException e) {
            // e.printStackTrace();  // 调试使用
            throw new UnsupportedOperationException(e.getMessage());
        }
    }

    public static void decompress(ExtractKernel kernel, int... chromosomeIndexes) {
        try {
            // 对染色体数据排序
            Arrays.sort(chromosomeIndexes);
            for (int chromosomeIndex : chromosomeIndexes) {
                GTBNodes nodes = kernel.gtbManager.getGTBNodes(chromosomeIndex);
                if (nodes != null) {
                    for (GTBNode node : nodes) {
                        kernel.inputPipeline.put(true, TaskGTBNode.of(node));
                    }
                }
            }
        } catch (InterruptedException e) {
            // e.printStackTrace();  // 调试使用
            throw new UnsupportedOperationException(e.getMessage());
        }
    }

    public static void decompressByNodeIndex(ExtractKernel kernel, Map<Integer, int[]> chromosomeNodeIndexes) {
        try {
            for (GTBNodes nodes : kernel.gtbManager.getGtbTree()) {
                if (chromosomeNodeIndexes.containsKey(nodes.chromosomeIndex)) {
                    // 解压全部染色体
                    if (chromosomeNodeIndexes.get(nodes.chromosomeIndex) == null) {
                        for (GTBNode node : nodes) {
                            kernel.inputPipeline.put(true, TaskGTBNode.of(node));
                        }
                    } else {
                        // 对任务节点索引进行排序，避免输出文件乱序
                        Arrays.sort(chromosomeNodeIndexes.get(nodes.chromosomeIndex));
                        for (int index : chromosomeNodeIndexes.get(nodes.chromosomeIndex)) {
                            kernel.inputPipeline.put(true, TaskGTBNode.of(nodes.get(index)));
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            // e.printStackTrace();  // 调试使用
            throw new UnsupportedOperationException(e.getMessage());
        }
    }

    public static void decompress(ExtractKernel kernel, int chromosomeIndex, int minPos, int maxPos) {
        // 提取任务子块
        GTBNodes nodes = kernel.gtbManager.getGTBNodes(chromosomeIndex);

        if (nodes != null) {
            try {
                if (kernel.gtbManager.isOrderedGTB()) {
                    // 支持快速随机访问
                    if (nodes.intersectPos(minPos, maxPos)) {

                        for (GTBNode node : nodes) {
                            if (node.maxPos < minPos) {
                                continue;
                            }

                            if (node.minPos > maxPos) {
                                break;
                            }

                            kernel.inputPipeline.put(true, TaskGTBNode.of(node, minPos, maxPos));
                        }
                    }
                } else {
                    // 不支持快速随机访问，此时排序后的 GTBNodes 最大位置不一定准确
                    for (GTBNode node : nodes) {
                        // 无交集时跳转到下一个节点
                        if ((node.maxPos < minPos) || (node.minPos > maxPos)) {
                            continue;
                        }
                        kernel.inputPipeline.put(true, TaskGTBNode.of(node, minPos, maxPos));
                    }
                }
            } catch (InterruptedException e) {
                // e.printStackTrace();  // 调试使用
                throw new UnsupportedOperationException(e.getMessage());
            }
        }
    }

    public static void decompress(ExtractKernel kernel, Map<Integer, int[]> chromosomePositions) {
        GTBNodes nodes;
        int[] positions;
        int taskPos;
        int taskIndexStart;
        int count;

        try {
            if (kernel.gtbManager.isOrderedGTB()) {
                // 支持快速随机访问
                for (int chromosomeIndex : kernel.gtbManager.getChromosomeList()) {
                    if (chromosomePositions.containsKey(chromosomeIndex)) {
                        // 对应染色体编号的队列任务非空，取出并去重、排序
                        positions = ArrayUtils.dropDuplicated(chromosomePositions.get(chromosomeIndex));
                        Arrays.sort(positions);

                        // 提取该染色体编号对应的 GTB 簇
                        nodes = kernel.gtbManager.getGTBNodes(chromosomeIndex);

                        // 当有任务节点被包含在节点信息中时，进一步搜索
                        if (nodes.intersectPos(positions)) {
                            taskIndexStart = 0;

                            nextNode:
                            for (GTBNode node : nodes) {
                                // 记录该节点可能包含的任务个数
                                count = 0;

                                for (int i = taskIndexStart; i < positions.length; i++) {
                                    taskPos = positions[i];

                                    // 若节点覆盖了该位置范围，则进行记录
                                    if (node.contain(taskPos)) {
                                        // 第一个被记录的任务节点，更新其索引值
                                        if (count == 0) {
                                            taskIndexStart = i;
                                        }

                                        count++;
                                    } else if (node.minPos > taskPos) {
                                        // 当前节点不可能包含该任务，跳转至下一个任务
                                        taskIndexStart = i + 1;
                                    } else if (node.maxPos < taskPos) {
                                        // 送出之前的数据
                                        if (count > 0) {
                                            kernel.inputPipeline.put(true, TaskGTBNode.of(node, ArrayUtils.copyOfRange(positions, taskIndexStart, taskIndexStart + count)));
                                        }

                                        // 记录当前的任务索引标记，下一次也从上一个位置开始搜索，避免有的节点横跨多个块
                                        taskIndexStart = (i == 0) ? 0 : i - 1;
                                        continue nextNode;
                                    }
                                }

                                if (count > 0) {
                                    kernel.inputPipeline.put(true, TaskGTBNode.of(node, ArrayUtils.copyOfRange(positions, taskIndexStart, taskIndexStart + count)));
                                    break;
                                }
                            }
                        }
                    }
                }
            } else {
                // 不支持快速随机访问
                for (int chromosomeIndex : kernel.gtbManager.getChromosomeList()) {
                    if (chromosomePositions.containsKey(chromosomeIndex)) {
                        // 对应染色体编号的队列任务非空，取出并去重、排序
                        positions = ArrayUtils.dropDuplicated(chromosomePositions.get(chromosomeIndex));
                        Arrays.sort(positions);

                        // 提取该染色体编号对应的 GTB 簇
                        nodes = kernel.gtbManager.getGTBNodes(chromosomeIndex);

                        // 最大的位置比该节点簇最小位置仍要小，则略过
                        if ((nodes.numOfNodes() > 0) && (positions[positions.length - 1] < nodes.get(0).minPos)) {
                            continue;
                        }

                        // 当有任务节点被包含在节点信息中时，进一步搜索
                        taskIndexStart = 0;

                        nextNode:
                        for (GTBNode node : nodes) {
                            while (taskIndexStart < positions.length && positions[taskIndexStart] < node.minPos) {
                                taskIndexStart++;
                            }

                            // 记录该节点可能包含的任务个数
                            count = 0;

                            for (int i = taskIndexStart; i < positions.length; i++) {
                                taskPos = positions[i];

                                // 若节点覆盖了该位置范围，则进行记录
                                if (node.contain(taskPos)) {
                                    count++;
                                } else if (node.maxPos < taskPos) {
                                    // 送出之前的数据
                                    if (count > 0) {
                                        kernel.inputPipeline.put(true, TaskGTBNode.of(node, ArrayUtils.copyOfRange(positions, taskIndexStart, taskIndexStart + count)));
                                    }

                                    continue nextNode;
                                }
                            }

                            if (count > 0) {
                                kernel.inputPipeline.put(true, TaskGTBNode.of(node, ArrayUtils.copyOfRange(positions, taskIndexStart, taskIndexStart + count)));
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            // e.printStackTrace();  // 调试使用
            throw new UnsupportedOperationException(e.getMessage());
        }
    }
}
