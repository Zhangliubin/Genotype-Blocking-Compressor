package edu.sysu.pmglab.gbc.core.gtbcomponent;

import edu.sysu.pmglab.check.Assert;
import edu.sysu.pmglab.container.VolumeByteStream;
import edu.sysu.pmglab.container.array.Array;
import edu.sysu.pmglab.container.array.BaseArray;
import edu.sysu.pmglab.easytools.ArrayUtils;
import edu.sysu.pmglab.easytools.ByteCode;
import edu.sysu.pmglab.easytools.ValueUtils;
import edu.sysu.pmglab.gbc.constant.ChromosomeTags;
import edu.sysu.pmglab.gbc.core.exception.GBCExceptionOptions;
import edu.sysu.pmglab.gbc.core.exception.GTBComponentException;
import edu.sysu.pmglab.unifyIO.FileStream;
import edu.sysu.pmglab.unifyIO.InputStreamReaderStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @Data        :2021/03/04
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :GTB 树
 */

public class GTBTree implements Iterable<GTBNodes> {
    private final HashMap<Integer, GTBNodes> rootNodes;
    private int rootIndex;

    /**
     * 构造器方法
     */
    public GTBTree() {
        this(ChromosomeTags.supportedChromosomeList().length);
    }

    /**
     * 构造器方法
     * @param size 树初始尺寸，默认可容纳所有类型的染色体类型
     */
    public GTBTree(int size) {
        this.rootNodes = new HashMap<>(size);
    }

    /**
     * 构造器方法
     */
    public GTBTree(BaseArray<GTBNode> gtbNodes) {
        this(24);

        for (GTBNode node : gtbNodes) {
            add(node);
        }

        flush();
    }

    /**
     * 绑定节点树归属的根文件
     * @param rootIndex 根文件编号
     */
    public void bind(int rootIndex) {
        this.rootIndex = rootIndex;
        for (GTBNodes nodes : this) {
            nodes.bind(rootIndex);
        }
    }

    /**
     * 获取节点树归属的根文件
     */
    public int getRootIndex() {
        return this.rootIndex;
    }

    /**
     * 清除节点数据
     */
    public void clear() {
        this.rootNodes.clear();
    }

    /**
     * 从 rootNodes 中获取对应染色体的 GTBNodes
     * @param chromosomeIndex 染色体编号 (索引)
     */
    public GTBNodes get(int chromosomeIndex) {
        return this.rootNodes.get(chromosomeIndex);
    }

    /**
     * 从 rootNodes 中获取对应染色体的 GTBNodes
     * @param chromosomeIndexes 染色体编号
     */
    public GTBNodes[] get(int... chromosomeIndexes) {
        GTBNodes[] nodes = new GTBNodes[chromosomeIndexes.length];

        for (int i = 0; i < chromosomeIndexes.length; i++) {
            nodes[i] = get(chromosomeIndexes[i]);
        }

        return nodes;
    }

    /**
     * 从 rootNodes 中获取对应染色体的 GTBNodes
     * @param chromosome 染色体字符串信息
     */
    public GTBNodes get(String chromosome) {
        return get(ChromosomeTags.getIndex(chromosome));
    }

    /**
     * 从 rootNodes 中获取对应染色体的 GTBNodes
     * @param chromosomes 染色体字符串信息
     */
    public GTBNodes[] get(String... chromosomes) {
        return get(ChromosomeTags.getIndexes(chromosomes));
    }

    /**
     * 从 rootNodes 中获取对应染色体的 GTBNodes
     * @param chromosomes 染色体字符串信息
     */
    public <ChromosomeType> GTBNodes[] get(Collection<ChromosomeType> chromosomes) {
        return get(ChromosomeTags.identifyChromosome(chromosomes));
    }

    /**
     * 重设重叠群信息
     * @param resourceName 资源文件
     */
    public void resetContig(String resourceName) throws IOException {
        // 资源文件名相同时不进行转换
        synchronized (this) {
            if (resourceName != null && !(ChromosomeTags.getCurrentFile().equals(resourceName))) {
                // 打开文件资源
                FileStream fileStream;
                InputStream innerResource = ChromosomeTags.class.getResourceAsStream("/contig/" + resourceName);
                if (innerResource != null) {
                    fileStream = new FileStream(new InputStreamReaderStream(innerResource));
                } else {
                    fileStream = new FileStream(resourceName);
                }

                // 新节点信息表
                HashMap<Integer, GTBNodes> newRootNodes = new HashMap<>(this.rootNodes.size());

                VolumeByteStream lineCache = new VolumeByteStream(128);
                fileStream.readLine(lineCache);

                // 解析注释行
                while ((lineCache.cacheOf(0) == ByteCode.NUMBER_SIGN) && (lineCache.cacheOf(1) == ByteCode.NUMBER_SIGN)) {
                    lineCache.reset();
                    fileStream.readLine(lineCache);
                }

                // 解析正文字段
                int count = 0;
                String[] fields = new String(lineCache.cacheOf(1, lineCache.size())).split(",");
                Array<String> chromosomes = new Array<>();
                int chromosomeIndex = ArrayUtils.indexOf(fields, "chromosome");

                Assert.that(chromosomeIndex != -1, "doesn't match to standard Chromosome Config file.");

                lineCache.reset();
                while (fileStream.readLine(lineCache) != -1) {
                    Assert.that(count >= 0 && count <= 255, "too much chromosome input (> 256)");

                    String[] groups = new String(lineCache.values()).split(",");
                    chromosomes.add(groups[chromosomeIndex]);
                    count++;
                    lineCache.reset();
                }

                fileStream.close();

                for (int chromosomeInd : this.rootNodes.keySet()) {
                    int newIndex = chromosomes.indexOf(ChromosomeTags.getString(chromosomeInd));
                    if (newIndex != -1) {
                        newRootNodes.put(newIndex, this.rootNodes.get(chromosomeInd).resetChromosome(newIndex));
                    }
                }

                this.rootNodes.clear();
                this.rootNodes.putAll(newRootNodes);
            }

            // 加载新的资源文件
            ChromosomeTags.load(resourceName);
        }
    }

    /**
     * 检验是否包含某个染色体数据
     * @param chromosomeIndex 染色体编号
     */
    public boolean contain(int chromosomeIndex) {
        return this.rootNodes.containsKey(chromosomeIndex);
    }

    /**
     * 检验是否包含某些染色体数据
     * @param chromosomeIndexes 染色体编号
     */
    public boolean[] contain(int... chromosomeIndexes) {
        boolean[] contains = new boolean[chromosomeIndexes.length];

        for (int i = 0; i < chromosomeIndexes.length; i++) {
            contains[i] = contain(chromosomeIndexes[i]);
        }

        return contains;
    }

    /**
     * 检验是否包含某些染色体数据
     * @param chromosomeIndexes 染色体编号
     */
    public boolean containAll(int... chromosomeIndexes) {
        for (int chromosomeIndex : chromosomeIndexes) {
            if (!contain(chromosomeIndex)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检验是否包含某个染色体数据
     * @param chromosome 染色体编号
     */
    public boolean contain(String chromosome) {
        return contain(ChromosomeTags.getIndex(chromosome));
    }

    /**
     * 检验是否包含某个染色体数据
     * @param chromosomes 染色体编号
     */
    public boolean[] contain(String... chromosomes) {
        return contain(ChromosomeTags.getIndexes(chromosomes));
    }

    /**
     * 检验是否包含某个染色体数据
     * @param chromosomes 染色体编号
     */
    public boolean containAll(String... chromosomes) {
        for (String chromosome : chromosomes) {
            if (!contain(chromosome)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检验是否包含某个染色体数据
     * @param chromosomes 染色体编号
     */
    public <ChromosomeType> boolean[] contain(Collection<ChromosomeType> chromosomes) {
        return contain(ChromosomeTags.identifyChromosome(chromosomes));
    }

    /**
     * 检验是否包含某个染色体数据
     * @param chromosomes 染色体编号
     */
    public <ChromosomeType> boolean containAll(Collection<ChromosomeType> chromosomes) {
        return containAll(ChromosomeTags.identifyChromosome(chromosomes));
    }

    /**
     * 添加单个 GTB 节点，添加操作后需要使用 flush 保证其顺序
     * @param nodes 待添加的节点群
     */
    public void add(GTBNode... nodes) {
        for (GTBNode node : nodes) {
            if (!contain(node.chromosomeIndex)) {
                this.rootNodes.put(node.chromosomeIndex, new GTBNodes(node.chromosomeIndex));
            }

            get(node.chromosomeIndex).add(node);
        }
    }

    /**
     * 添加 GTBNodes 节点群，添加操作后需要使用 flush 保证其顺序
     * @param nodes 待添加的节点群
     */
    public void add(GTBNodes... nodes) {
        for (GTBNodes node : nodes) {
            if (contain(node.chromosomeIndex)) {
                get(node.chromosomeIndex).add(node);
            } else {
                this.rootNodes.put(node.chromosomeIndex, node);
            }
        }
    }

    /**
     * 添加 GTBTree 节点树，添加操作后需要使用 flush 保证其顺序
     * @param trees 待添加的节点树
     */
    public void add(GTBTree... trees) {
        for (GTBTree tree : trees) {
            for (GTBNodes nodes : tree) {
                add(nodes);
            }
        }
    }

    /**
     * 添加 GTBNode 节点对象
     * @param nodes 待添加的节点
     */
    public <NodeType> void add(Collection<NodeType> nodes) {
        for (NodeType nodeUnknownType : nodes) {
            if (nodeUnknownType instanceof GTBNode) {
                add((GTBNode) nodeUnknownType);
            } else if (nodeUnknownType instanceof GTBNodes) {
                add((GTBNodes) nodeUnknownType);
            } else if (nodeUnknownType instanceof GTBTree) {
                add((GTBTree) nodeUnknownType);
            } else {
                throw new GTBComponentException("unsupported node type: " + nodeUnknownType.getClass());
            }
        }
    }

    /**
     * 移除染色体
     * @param chromosomeIndexes 移除染色体编号列表
     */
    public void remove(int... chromosomeIndexes) {
        for (int chromosomeIndex : chromosomeIndexes) {
            Assert.that(contain(chromosomeIndex), GBCExceptionOptions.GTBComponentException, "chromosome=" + ChromosomeTags.getString(chromosomeIndex) + " (index=" + chromosomeIndex + ") not in current GTB file");
            this.rootNodes.remove(chromosomeIndex);
        }
    }

    /**
     * 移除染色体
     * @param chromosomes 移除染色体编号列表
     */
    public void remove(String... chromosomes) {
        remove(ChromosomeTags.getIndexes(chromosomes));
    }

    /**
     * 移除染色体
     * @param chromosomes 移除染色体编号列表
     */
    public <ChromosomeType> void remove(Collection<ChromosomeType> chromosomes) {
        remove(ChromosomeTags.identifyChromosome(chromosomes));
    }

    /**
     * 移除指定染色体对应的节点数据
     * @param chromosomeNodeIndexListUnknownType 移除节点列表
     */
    public <ChromosomeType> void remove(Map<ChromosomeType, int[]> chromosomeNodeIndexListUnknownType) {
        HashMap<Integer, int[]> chromosomeNodeIndexList = ChromosomeTags.identifyChromosome(chromosomeNodeIndexListUnknownType);

        for (int chromosomeIndex : chromosomeNodeIndexList.keySet()) {
            Assert.that(contain(chromosomeIndex), GBCExceptionOptions.GTBComponentException, "chromosome=" + ChromosomeTags.getString(chromosomeIndex) + " (index=" + chromosomeIndex + ") not in current GTB file");

            // 节点树包含该染色体编号，但移除队列中不包含任何信息，则认为是需要移除其所有的节点
            if (chromosomeNodeIndexList.get(chromosomeIndex) == null) {
                this.rootNodes.remove(chromosomeIndex);
            } else {
                // 取出每个需要剔除的节点
                get(chromosomeIndex).remove(chromosomeNodeIndexList.get(chromosomeIndex));
            }
        }
    }

    /**
     * 保留染色体
     * @param chromosomeIndexes 保留的染色体编号列表
     */
    public void retain(int... chromosomeIndexes) {
        HashMap<Integer, GTBNodes> newRootNodes = new HashMap<>(chromosomeIndexes.length);

        for (int chromosomeIndex : chromosomeIndexes) {
            Assert.that(contain(chromosomeIndex), GBCExceptionOptions.GTBComponentException, "chromosome=" + ChromosomeTags.getString(chromosomeIndex) + " (index=" + chromosomeIndex + ") not in current GTB file");
            newRootNodes.put(chromosomeIndex, get(chromosomeIndex));
        }

        this.rootNodes.clear();

        for (int chromosomeIndex : newRootNodes.keySet()) {
            this.rootNodes.put(chromosomeIndex, newRootNodes.get(chromosomeIndex));
        }
    }

    /**
     * 保留染色体
     * @param chromosomes 保留的染色体编号列表
     */
    public void retain(String... chromosomes) {
        retain(ChromosomeTags.getIndexes(chromosomes));
    }

    /**
     * 移除染色体
     * @param chromosomes 移除染色体编号列表
     */
    public <ChromosomeType> void retain(Collection<ChromosomeType> chromosomes) {
        retain(ChromosomeTags.identifyChromosome(chromosomes));
    }

    /**
     * 移除指定染色体对应的节点数据
     * @param chromosomeNodeIndexListUnknownType 移除节点列表
     */
    public <ChromosomeType> void retain(Map<ChromosomeType, int[]> chromosomeNodeIndexListUnknownType) {
        HashMap<Integer, GTBNodes> newRootNodes = new HashMap<>(chromosomeNodeIndexListUnknownType.size());
        HashMap<Integer, int[]> chromosomeNodeIndexList = ChromosomeTags.identifyChromosome(chromosomeNodeIndexListUnknownType);

        for (int chromosomeIndex : chromosomeNodeIndexList.keySet()) {
            Assert.that(contain(chromosomeIndex), GBCExceptionOptions.GTBComponentException, "chromosome=" + ChromosomeTags.getString(chromosomeIndex) + " (index=" + chromosomeIndex + ") not in current GTB file");

            if (chromosomeNodeIndexList.get(chromosomeIndex) == null) {
                newRootNodes.put(chromosomeIndex, get(chromosomeIndex));
            } else {
                newRootNodes.put(chromosomeIndex, new GTBNodes(get(chromosomeIndex).get(chromosomeNodeIndexList.get(chromosomeIndex))));
            }
        }

        this.rootNodes.clear();

        for (int chromosomeIndex : newRootNodes.keySet()) {
            this.rootNodes.put(chromosomeIndex, newRootNodes.get(chromosomeIndex));
        }
    }

    /**
     * 获取总节点数
     */
    public int numOfNodes() {
        int count = 0;
        for (GTBNodes nodes : this) {
            count += nodes.numOfNodes();
        }
        return count;
    }

    /**
     * 获取指定染色体的总节点数
     * @param chromosomeIndex 指定的染色体索引
     */
    public int numOfNodes(int chromosomeIndex) {
        Assert.that(contain(chromosomeIndex), GBCExceptionOptions.GTBComponentException, "chromosome=" + ChromosomeTags.getString(chromosomeIndex) + " (index=" + chromosomeIndex + ") not in current GTB file");
        return get(chromosomeIndex).numOfNodes();
    }

    /**
     * 获取指定染色体的总节点数
     * @param chromosome 指定的染色体
     */
    public int numOfNodes(String chromosome) {
        Assert.that(contain(chromosome), GBCExceptionOptions.GTBComponentException, "chromosome=" + chromosome + " not in current GTB file");
        return get(chromosome).numOfNodes();
    }

    /**
     * 获取总节点数
     */
    public int numOfVariants() {
        int count = 0;
        for (GTBNodes nodes : this) {
            count += nodes.numOfVariants();
        }
        return count;
    }

    /**
     * 获取染色体编号对应的变异位点数量
     * @param chromosomeIndex 指定的染色体索引
     */
    public int numOfVariants(int chromosomeIndex) {
        Assert.that(contain(chromosomeIndex), GBCExceptionOptions.GTBComponentException, "chromosome=" + ChromosomeTags.getString(chromosomeIndex) + " (index=" + chromosomeIndex + ") not in current GTB file");
        return get(chromosomeIndex).numOfVariants();
    }

    /**
     * 获取染色体编号对应的变异位点数量
     * @param chromosome 指定的染色体
     */
    public int numOfVariants(String chromosome) {
        Assert.that(contain(chromosome), GBCExceptionOptions.GTBComponentException, "chromosome=" + chromosome + " not in current GTB file");
        return get(chromosome).numOfVariants();
    }

    /**
     * 获得数据块的最大尺寸
     */
    public int getMaxDecompressedAllelesSize() {
        int maxOriginAllelesSize = 0;
        int currentOriginAllelesSize;
        for (GTBNodes nodes : this) {
            for (GTBNode node : nodes) {
                currentOriginAllelesSize = node.getOriginAllelesSizeFlag();
                if (maxOriginAllelesSize < currentOriginAllelesSize) {
                    maxOriginAllelesSize = currentOriginAllelesSize;
                }
            }
        }
        return GTBNode.rebuildMagicCode(maxOriginAllelesSize);
    }

    /**
     * 获得数据块的最大尺寸
     */
    public int getMaxDecompressedMBEGsSize() {
        int maxOriginMBEGsSize = 0;
        int currentOriginMBEGsSize;
        for (GTBNodes nodes : this) {
            for (GTBNode node : nodes) {
                currentOriginMBEGsSize = node.getOriginMBEGsSizeFlag();
                if (maxOriginMBEGsSize < currentOriginMBEGsSize) {
                    maxOriginMBEGsSize = currentOriginMBEGsSize;
                }
            }
        }
        return GTBNode.rebuildMagicCode(maxOriginMBEGsSize);
    }

    /**
     * 获取染色体编号
     */
    public int[] getChromosomeList() {
        int[] chromosomeIndexes = ArrayUtils.getIntegerKey(this.rootNodes);
        Arrays.sort(chromosomeIndexes);

        return chromosomeIndexes;
    }

    /**
     * 获取染色体的个数
     */
    public int numOfChromosomes() {
        return this.rootNodes.size();
    }

    /**
     * 构建块头部信息
     */
    public VolumeByteStream build() {
        // 获取总块数
        int nodeNum = numOfNodes();

        // 创建头部信息容器
        VolumeByteStream header = new VolumeByteStream(nodeNum * 25);

        // 写入块头部信息 25 byte
        for (GTBNodes nodes : this) {
            for (GTBNode node : nodes) {
                header.write(node.chromosomeIndex);
                header.writeIntegerValue(node.minPos);
                header.writeIntegerValue(node.maxPos);
                header.writeShortValue(node.subBlockVariantNum[0]);
                header.writeShortValue(node.subBlockVariantNum[1]);
                header.writeIntegerValue(node.compressedGenotypesSize);
                header.write(ValueUtils.value2ByteArray(node.compressedPosSize, 3));
                header.writeIntegerValue(node.compressedAlleleSize);
                header.write(node.magicCode);
            }
        }

        return header;
    }

    /**
     * 刷新 chromosome 列表
     */
    public void flush() {
        for (GTBNodes nodes : this) {
            nodes.flush();
        }
    }

    /**
     * 刷新 chromosome 列表，可传入参数表达是否去重
     */
    public void flush(boolean dropDuplicates) {
        for (GTBNodes nodes : this) {
            nodes.flush(dropDuplicates);
        }
    }

    /**
     * 是否有序的
     */
    public boolean isOrder() {
        boolean orderedGTB = true;
        for (GTBNodes nodes : this) {
            orderedGTB = (orderedGTB && nodes.checkOrdered());
        }

        return orderedGTB;
    }

    /**
     * 是否有序的
     */
    public boolean isOrder(int chromosomeIndex) {
        return get(chromosomeIndex).checkOrdered();
    }

    /**
     * 获取最大块大小
     */
    public int getMaxOriginBlockSize(int validSubjectNum) {
        int maxOriginBlockSize = 0;
        int currentNodeEstimateSize;
        for (GTBNodes nodes : this) {
            currentNodeEstimateSize = nodes.sizeOfDecompressedNodes(validSubjectNum);
            if (currentNodeEstimateSize > maxOriginBlockSize) {
                maxOriginBlockSize = currentNodeEstimateSize;

                if (maxOriginBlockSize == Integer.MAX_VALUE - 2) {
                    break;
                }
            }
        }

        return maxOriginBlockSize;
    }

    @Override
    public String toString() {
        return nodeInfo(getChromosomeList());
    }

    public String chromosomeInfo() {
        return chromosomeInfo(getChromosomeList());
    }

    public String chromosomeInfo(int[] chromosomeIndexes) {
        for (int chromosomeIndex : chromosomeIndexes) {
            Assert.that(contain(chromosomeIndex), GBCExceptionOptions.GTBComponentException, "chromosome=" + ChromosomeTags.getString(chromosomeIndex) + " (index=" + chromosomeIndex + ") not in current GTB file");
        }

        if (chromosomeIndexes.length > 0 && this.rootNodes.size() > 0) {
            StringBuilder out = new StringBuilder(2 << 15);

            // 输出节点信息
            String[] chromosomeSep = new String[]{"├─ ", "└─ "};

            for (int i = 0; i < chromosomeIndexes.length - 1; i++) {
                out.append(chromosomeSep[0]).append(get(chromosomeIndexes[i]).getRootInfo());
                out.append("\n");
            }

            out.append(chromosomeSep[1] + get(chromosomeIndexes[chromosomeIndexes.length - 1]).getRootInfo());
            return out.toString();
        } else {
            return "  <empty>";
        }
    }

    public <ChromosomeType> String chromosomeInfo(Collection<ChromosomeType> chromosomeIndexes) {
        return chromosomeInfo(ChromosomeTags.identifyChromosome(chromosomeIndexes));
    }

    public String nodeInfo(int[] chromosomeIndexes) {
        for (int chromosomeIndex : chromosomeIndexes) {
            Assert.that(contain(chromosomeIndex), GBCExceptionOptions.GTBComponentException, "chromosome=" + ChromosomeTags.getString(chromosomeIndex) + " (index=" + chromosomeIndex + ") not in current GTB file");
        }

        if (chromosomeIndexes.length > 0 && this.rootNodes.size() > 0) {
            StringBuilder out = new StringBuilder(2 << 15);

            // 输出节点信息
            String[] chromosomeSep = new String[]{"├─ ", "└─ "};
            String[] nodeSep = new String[]{"│  ", "   "};

            for (int i = 0; i < chromosomeIndexes.length - 1; i++) {
                out.append(chromosomeSep[0] + get(chromosomeIndexes[i]).getRootInfo());

                int chromosomeSize = get(chromosomeIndexes[i]).numOfNodes();
                if (chromosomeSize > 0) {
                    // 写入子块信息
                    String[] nodeInfos = get(chromosomeIndexes[i]).getNodesInfo();
                    for (int j = 0; j < chromosomeSize - 1; j++) {
                        out.append(String.format("\n%s ├─ Node %d: %s", nodeSep[0], j + 1, nodeInfos[j]));
                    }

                    // 写入最后一个子块信息
                    out.append(String.format("\n%s └─ Node %d: %s", nodeSep[0], chromosomeSize, nodeInfos[chromosomeSize - 1]));
                }

                out.append("\n");
            }

            int i = chromosomeIndexes.length - 1;
            out.append(chromosomeSep[1] + get(chromosomeIndexes[i]).getRootInfo());
            int chromosomeSize = get(chromosomeIndexes[i]).numOfNodes();
            if (chromosomeSize > 0) {
                // 写入子块信息
                String[] nodeInfos = get(chromosomeIndexes[i]).getNodesInfo();
                for (int j = 0; j < chromosomeSize - 1; j++) {
                    out.append(String.format("\n%s ├─ Node %d: %s", nodeSep[1], j + 1, nodeInfos[j]));
                }

                // 写入最后一个子块信息
                out.append(String.format("\n%s └─ Node %d: %s", nodeSep[1], chromosomeSize, nodeInfos[chromosomeSize - 1]));
            }

            return out.toString();
        } else {
            return "  <empty>";
        }
    }

    @Override
    public Iterator<GTBNodes> iterator() {
        return new Iterator<GTBNodes>() {
            private final int[] chromosomeList = getChromosomeList();
            private int seek = 0;

            @Override
            public boolean hasNext() {
                return this.seek < chromosomeList.length;
            }

            @Override
            public GTBNodes next() {
                return rootNodes.get(this.chromosomeList[this.seek++]);
            }
        };
    }
}
