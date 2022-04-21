package edu.sysu.pmglab.gbc.core.gtbcomponent.gtbreader;

import edu.sysu.pmglab.check.Value;
import edu.sysu.pmglab.gbc.core.gtbcomponent.GTBManager;

/**
 * @Data        :2021/08/15
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :
 */

public class LimitPointer extends Pointer {
    /**
     * 边界指针，左闭右开
     */
    int startNodeIndex;
    int endNodeIndex;

    private LimitPointer(GTBManager manager, int[] chromosomeList) {
        this.manager = manager;
        this.chromosomeList = chromosomeList;
    }

    public LimitPointer(GTBManager manager, int[] chromosomeList, int startNodeIndex, int endNodeIndex) {
        this.manager = manager;
        this.chromosomeList = chromosomeList;

        this.startNodeIndex = startNodeIndex == -1 ? 0 : Value.of(startNodeIndex, 0, manager.getGTBNodes(chromosomeList[0]).numOfNodes() - 1);
        this.endNodeIndex = endNodeIndex == -1 ? manager.getGTBNodes(chromosomeList[chromosomeList.length - 1]).numOfNodes() : Value.of(endNodeIndex, 0, manager.getGTBNodes(chromosomeList[0]).numOfNodes() - 1);

        // 初始化指针信息
        setChromosome(0);
    }

    @Override
    protected void setChromosome(int chromosomeIndex) {
        if ((chromosomeIndex == -1) || (chromosomeIndex >= chromosomeList.length)) {
            this.chromosomeIndex = -1;
            this.nodeLength = 0;
            setNode(-1);
        } else {
            this.chromosomeIndex = chromosomeIndex;

            if (chromosomeList.length == 1) {
                this.nodeLength = endNodeIndex - startNodeIndex;
            } else if (this.chromosomeIndex == 0) {
                this.nodeLength = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).numOfNodes() - startNodeIndex;
            } else if (this.chromosomeIndex == chromosomeList.length - 1) {
                this.nodeLength = endNodeIndex;
            } else {
                this.nodeLength = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).numOfNodes();
            }

            setNode(0);
        }
    }


    @Override
    protected void setNode(int nodeIndex) {
        if ((nodeIndex == -1) || (nodeIndex >= nodeLength)) {
            this.nodeIndex = -1;
            this.node = null;
            this.variantLength = 0;
            this.variantIndex = -1;
        } else {
            this.nodeIndex = nodeIndex;

            if (this.chromosomeIndex == 0) {
                this.node = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).get(startNodeIndex + nodeIndex);
            } else {
                this.node = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).get(nodeIndex);
            }

            this.variantLength = this.node.numOfVariants();
            this.variantIndex = variantLength == 0 ? -1 : 0;
        }
    }

    @Override
    protected void setChromosomeNode(int chromosomeIndex, int nodeIndex) {
        if ((chromosomeIndex == -1) || (chromosomeIndex >= chromosomeList.length)) {
            this.chromosomeIndex = -1;
            this.nodeLength = 0;
        } else {
            this.chromosomeIndex = chromosomeIndex;

            if (chromosomeList.length == 1) {
                this.nodeLength = endNodeIndex - startNodeIndex;
            } else if (this.chromosomeIndex == 0) {
                this.nodeLength = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).numOfNodes() - startNodeIndex;
            } else if (this.chromosomeIndex == chromosomeList.length - 1) {
                this.nodeLength = endNodeIndex;
            } else {
                this.nodeLength = this.manager.getGTBNodes(chromosomeList[chromosomeIndex]).numOfNodes();
            }
        }

        setNode(nodeIndex);
    }

    @Override
    public LimitPointer clone() {
        LimitPointer cloner = new LimitPointer(this.manager, this.chromosomeList);
        cloner.chromosomeIndex = this.chromosomeIndex;
        cloner.nodeIndex = this.nodeIndex;
        cloner.nodeLength = this.nodeLength;
        cloner.variantIndex = this.variantIndex;
        cloner.variantLength = this.variantLength;
        cloner.node = this.node;
        cloner.startNodeIndex = this.startNodeIndex;
        cloner.endNodeIndex = this.endNodeIndex;
        return cloner;
    }
}