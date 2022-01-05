package edu.sysu.pmglab.suranyi.gbc.core.extract;

import edu.sysu.pmglab.suranyi.check.Value;
import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNode;

import java.util.HashSet;

/**
 * @Data        :2021/02/14
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :任务 GTB 节点
 */

class TaskGTBNode {
    final GTBNode node;

    /**
     * 任务类型, type = 0 解压全部数据, type = 1 解压指定范围数据，type = 2 解压任务数据
     */
    final int taskType;
    int minPos;
    int maxPos;
    final HashSet<Integer> taskPos;

    static TaskGTBNode of(GTBNode node) {
        return new TaskGTBNode(node);
    }

    static TaskGTBNode of(GTBNode node, int minPos, int maxPos) {
        return new TaskGTBNode(node, minPos, maxPos);
    }

    static TaskGTBNode of(GTBNode node, int[] taskPos) {
        return new TaskGTBNode(node, taskPos);
    }

    TaskGTBNode(GTBNode node) {
        this.node = node;
        this.taskType = 0;
        this.taskPos = null;
    }

    TaskGTBNode(GTBNode node, int minPos, int maxPos) {
        this.node = node;
        this.taskPos = null;
        if ((minPos <= this.node.minPos) && (maxPos >= this.node.maxPos)) {
            this.taskType = 0;
        } else {
            this.taskType = 1;
            this.minPos = Value.of(minPos, node.minPos, node.maxPos);
            this.maxPos = Value.of(maxPos, node.minPos, node.maxPos);
        }
    }

    TaskGTBNode(GTBNode node, int[] taskPos) {
        this.node = node;
        this.taskType = 2;
        this.taskPos = ArrayUtils.toSet(taskPos);
    }
}
