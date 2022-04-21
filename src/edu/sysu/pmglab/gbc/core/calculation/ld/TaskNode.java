package edu.sysu.pmglab.gbc.core.calculation.ld;

/**
 * @author suranyi
 */

class TaskNode {
    /**
     * 处理的任务节点
     */
    final int chromosomeIndex;

    final int minPos;
    final int maxPos;
    final int maxSearchPos;

    static TaskNode of(int chromosomeIndex, int minPos, int maxPos, int maxSearchPos) {
        return new TaskNode(chromosomeIndex, minPos, maxPos, maxSearchPos);
    }

    static TaskNode of(int chromosomeIndex, int minPos, int maxPos) {
        return new TaskNode(chromosomeIndex, minPos, maxPos, maxPos);
    }

    TaskNode(int chromosomeIndex, int minPos, int maxPos, int maxSearchPos) {
        this.chromosomeIndex = chromosomeIndex;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.maxSearchPos = maxSearchPos;
    }
}
