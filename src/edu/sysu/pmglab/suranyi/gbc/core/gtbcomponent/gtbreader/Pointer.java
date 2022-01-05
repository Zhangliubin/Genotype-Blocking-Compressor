package edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBManager;
import edu.sysu.pmglab.suranyi.gbc.core.gtbcomponent.GTBNode;

import java.io.IOException;

public class Pointer implements Cloneable {
    int chromosomeIndex;
    int nodeIndex;
    int nodeLength;
    int variantIndex;
    int variantLength;
    GTBNode node;

    GTBManager manager;
    int[] chromosomeList;

    Pointer() {
        manager = null;
        chromosomeList = null;
    }

    public Pointer(GTBManager manager) {
        this.manager = manager;
        this.chromosomeList = manager.getChromosomeList();

        // 初始化指针信息
        setChromosome(0);
    }

    protected void setChromosome(int chromosomeIndex) {
        if ((chromosomeIndex == -1) || (chromosomeIndex >= chromosomeList.length)) {
            this.chromosomeIndex = -1;
            this.nodeLength = 0;
            setNode(-1);
        } else {
            this.chromosomeIndex = chromosomeIndex;
            this.nodeLength = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).numOfNodes();
            setNode(0);
        }
    }


    protected void setNode(int nodeIndex) {
        if ((nodeIndex == -1) || (nodeIndex >= nodeLength)) {
            this.nodeIndex = -1;
            this.node = null;
            this.variantLength = 0;
            this.variantIndex = -1;
        } else {
            this.nodeIndex = nodeIndex;
            this.node = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).get(nodeIndex);
            this.variantLength = this.node.numOfVariants();
            this.variantIndex = variantLength == 0 ? -1 : 0;
        }
    }

    protected void setChromosomeNode(int chromosomeIndex, int nodeIndex) {
        if ((chromosomeIndex == -1) || (chromosomeIndex >= chromosomeList.length)) {
            this.chromosomeIndex = -1;
            this.nodeLength = 0;
        } else {
            this.chromosomeIndex = chromosomeIndex;
            this.nodeLength = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).numOfNodes();
        }

        setNode(nodeIndex);
    }

    /**
     * 按照标准索引值进行跳转
     * @param chromosomeIndex 染色体索引
     * @param nodeIndex 节点索引
     * @param variantIndex 变异位点索引
     * @return true 代表跳转成功，false 代表跳转失败
     */
    public boolean seek(int chromosomeIndex, int nodeIndex, int variantIndex) {
        // 验证数据
        if ((chromosomeIndex >= 0 && chromosomeIndex < this.chromosomeList.length)) {
            if (nodeIndex >= 0 && nodeIndex < this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).numOfNodes()) {
                if ((variantIndex >= 0 && variantIndex < this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).get(nodeIndex).numOfVariants())) {
                    this.chromosomeIndex = chromosomeIndex;
                    this.nodeIndex = nodeIndex;
                    this.node = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).get(nodeIndex);
                    this.nodeLength = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).numOfNodes();
                    this.variantIndex = variantIndex;
                    this.variantLength = this.node.numOfVariants();
                } else {
                    if (chromosomeIndex != this.chromosomeIndex) {
                        setChromosomeNode(chromosomeIndex, nodeIndex);
                    } else if (nodeIndex != this.nodeIndex) {
                        setNode(nodeIndex);
                    }

                    if (variantIndex == -1) {
                        this.variantIndex = -1;
                    } else {
                        return false;
                    }
                }
            } else {
                if (nodeIndex == -1) {
                    if (chromosomeIndex != this.chromosomeIndex) {
                        setChromosomeNode(chromosomeIndex, -1);
                    } else {
                        setNode(-1);
                    }
                } else {
                    return false;
                }
            }
        } else {
            if (chromosomeIndex == -1) {
                setChromosome(-1);
            } else {
                return false;
            }
        }

        return true;
    }

    public boolean seek(int chromosomeIndex) {
        return seek(chromosomeIndex, 0, 0);
    }

    public boolean seek(int chromosomeIndex, int nodeIndex) {
        return seek(chromosomeIndex, nodeIndex, 0);
    }

    public GTBNode getNode() {
        return node;
    }

    public boolean next() throws IOException {
        if (variantIndex == -1) {
            // 移动到下一个节点
            setNode(this.nodeIndex + 1);

            // 如果下一个节点不存在，则跳转到下一个染色体
            if (nodeIndex == -1) {
                setChromosome(this.chromosomeIndex + 1);

                // 如果已经是最后的染色体，则返回 false
                return chromosomeIndex != -1;
            }
        } else {
            variantIndex++;
            if (variantIndex >= variantLength) {
                // 位点已经读取完毕
                variantIndex = -1;
                return next();
            }
        }

        return true;
    }

    public void close() throws IOException {
        this.chromosomeIndex = -1;
        this.nodeIndex = -1;
        this.variantIndex = -1;
        this.nodeLength = 0;
        this.variantLength = 0;
    }

    @Override
    public String toString() {
        if (chromosomeIndex == -1) {
            return "GTBReader-Pointer = " +
                    "chromosomeIndex: " + (-1) + " (End of file)" +
                    ", nodeIndex: " + (-1) +
                    ", variantIndex: " + (-1) +
                    '}';
        } else {
            return "GTBReader-Pointer = " +
                    "chromosomeIndex: " + (chromosomeIndex + 1) + " / " + chromosomeList.length +
                    ", nodeIndex: " + (nodeIndex + 1) + " / " + nodeLength +
                    ", variantIndex: " + (variantIndex + 1) + " / " + variantLength +
                    '}';
        }
    }

    /**
     * 任务的克隆方法
     * @return 当前任务的拷贝
     */
    @Override
    public Pointer clone() {
        Pointer cloner = new Pointer(this.manager);
        cloner.chromosomeIndex = this.chromosomeIndex;
        cloner.nodeIndex = this.nodeIndex;
        cloner.nodeLength = this.nodeLength;
        cloner.variantIndex = this.variantIndex;
        cloner.variantLength = this.variantLength;
        cloner.node = this.node;
        return cloner;
    }
}