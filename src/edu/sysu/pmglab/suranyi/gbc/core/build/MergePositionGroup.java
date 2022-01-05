package edu.sysu.pmglab.suranyi.gbc.core.build;

import edu.sysu.pmglab.suranyi.easytools.ArrayUtils;
import edu.sysu.pmglab.suranyi.easytools.ByteCode;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * @Data        :2021/07/09
 * @Author      :suranyi
 * @Contact     :suranyi.sysu@gamil.com
 * @Description :候选合并信息
 */

class MergePositionGroup {
    final int position;
    final ArrayList<MultiAlleleGroup> groups;

    MergePositionGroup(int position, int maxSize) {
        this.position = position;
        this.groups = new ArrayList<>(maxSize);
    }

    public void add(byte[] alleles, byte allelesNum, short managerIndex, int nodeIndex, int variantIndex) {
        for (MultiAlleleGroup group : groups) {
            // 此处会尽可能合并编码相同的部分
            if (ArrayUtils.equal(group.allelesInfo, alleles) && group.getVariantCoordinate(managerIndex) == null) {
                group.add(managerIndex, nodeIndex, variantIndex);
                return;
            }
        }

        AlleleGroup pair = new AlleleGroup(alleles, allelesNum);
        pair.add(managerIndex, nodeIndex, variantIndex);
        groups.add(new MultiAlleleGroup(allelesNum, pair));
    }

    public int getSize() {
        return this.groups.size();
    }

    private void sort() {
        groups.sort((o1, o2) -> {
            // 根据等位基因个数
            int status = Integer.compare(o1.allelesNum, o2.allelesNum);
            if (status == 0) {
                return Integer.compare(o1.allelesInfo.length, o2.allelesInfo.length);
            }
            return status;
        });
    }

    public void finish() {
        if (groups.size() > 1) {
            // 排序
            sort();

            // 合并子集编码表
            out:
            while (true) {
                for (int id1 = 0; id1 < groups.size(); id1++) {
                    MultiAlleleGroup multigroup1 = groups.get(id1);
                    out2:
                    for (int id2 = id1 + 1; id2 < groups.size(); id2++) {
                        MultiAlleleGroup multigroup2 = groups.get(id2);
                        // 可合并的前提是没有重复管理器
                        if (mergeEnable(multigroup1, multigroup2)) {
                            // Mark: 一般合并过一次之后就很难再进行第二次合并，因此无序担心内存负荷问题
                            byte[] transCode = new byte[multigroup1.allelesNum];
                            byte[][] pair1Allele = multigroup1.buildAllele();
                            byte[][] pair2Allele = multigroup2.buildAllele();

                            out3:
                            for (int pairId1 = 0; pairId1 < pair1Allele.length; pairId1++) {
                                for (byte pairId2 = 0; pairId2 < pair2Allele.length; pairId2++) {
                                    if (ArrayUtils.equal(pair1Allele[pairId1], pair2Allele[pairId2])) {
                                        transCode[pairId1] = pairId2;
                                        continue out3;
                                    }
                                }

                                // 如果逛了一圈都没有出去，则说明该对不可靠 (交集关系)，继续搜索下一对
                                continue out2;
                            }

                            for (AlleleGroup group : multigroup1.pairs) {
                                for (int ind = 0; ind < group.transCode.length; ind++) {
                                    group.transCode[ind] = transCode[group.transCode[ind]];
                                }
                            }

                            // 成功合并一对后移除
                            groups.get(id2).merge(groups.get(id1));
                            groups.remove(id1);
                            continue out;
                        }
                    }
                }

                break;
            }
        }
    }

    public void freeMemory() {
        for (MultiAlleleGroup group: groups) {
            group.allelesInfo = null;
            for (AlleleGroup g: group.pairs) {
                g.transCode = null;
                g.allelesInfo = null;
                g.groups.clear();
            }
            group.pairs.clear();
        }
        groups.clear();
    }

    private boolean mergeEnable(MultiAlleleGroup group1, MultiAlleleGroup group2) {
        // 检查 managerIndex 是否冲突
        for (VariantCoordinate v1 : group1) {
            for (VariantCoordinate v2 : group2) {
                if (v1.managerIndex == v2.managerIndex) {
                    return false;
                }
            }
        }

        return true;
    }

    static class AlleleGroup implements Iterable<VariantCoordinate> {
        public byte[] allelesInfo;
        public byte[] transCode;
        boolean trans = false;
        public final ArrayList<VariantCoordinate> groups = new ArrayList<>(2);


        public AlleleGroup(byte[] allelesInfo, byte allelesNum) {
            this.allelesInfo = allelesInfo;

            this.transCode = new byte[allelesNum];

            for (byte i = 0; i < allelesNum; i++) {
                this.transCode[i] = i;
            }
        }

        public void add(short managerIndex, int nodeIndex, int variantIndex) {
            groups.add(new VariantCoordinate(managerIndex, nodeIndex, variantIndex));
        }

        public VariantCoordinate getVariantCoordinate(int managerIndex) {
            for (VariantCoordinate pair : groups) {
                if (pair.managerIndex == managerIndex) {
                    return pair;
                }
            }

            return null;
        }

        @Override
        public Iterator<VariantCoordinate> iterator() {
            return groups.iterator();
        }
    }

    static class MultiAlleleGroup implements Iterable<VariantCoordinate> {
        public byte allelesNum;
        public byte[] allelesInfo;
        public final ArrayList<AlleleGroup> pairs = new ArrayList<>(1);

        public MultiAlleleGroup(byte allelesNum, AlleleGroup group) {
            this.pairs.add(group);
            this.allelesNum = allelesNum;
            this.allelesInfo = group.allelesInfo;
        }

        VariantCoordinate getVariantCoordinate(int managerIndex) {
            VariantCoordinate variant;
            for (AlleleGroup group : pairs) {
                variant = group.getVariantCoordinate(managerIndex);
                if (variant != null) {
                    return variant;
                }
            }

            return null;
        }

        AlleleGroup getAlleleGroup(int managerIndex) {
            VariantCoordinate variant;
            for (AlleleGroup group : pairs) {
                variant = group.getVariantCoordinate(managerIndex);
                if (variant != null) {
                    return group;
                }
            }

            return null;
        }

        public void add(short managerIndex, int nodeIndex, int variantIndex) {
            if (pairs.size() == 1) {
                pairs.get(0).add(managerIndex, nodeIndex, variantIndex);
            }
        }

        public void merge(MultiAlleleGroup group) {
            // 交由 multi 托管 allele 信息
            group.allelesInfo = null;
            for (AlleleGroup g: group.pairs) {
                g.trans = false;
                for (int i = 0; i < g.transCode.length; i++) {
                    if (g.transCode[i] != i) {
                        g.trans = true;
                        break;
                    }
                }
            }
            pairs.addAll(group.pairs);
        }

        private byte[][] buildAllele() {
            int lastPointer = 0;
            int searchPointer = 0;
            int alleleIndex = 0;
            byte[][] out = new byte[allelesNum][];

            while (searchPointer < allelesInfo.length) {
                if (allelesInfo[searchPointer] == ByteCode.TAB) {
                    out[alleleIndex++] = ArrayUtils.copyOfRange(allelesInfo, lastPointer, searchPointer);
                    lastPointer = searchPointer + 1;
                    break;
                }

                searchPointer++;
            }

            searchPointer += 1;
            while (searchPointer < allelesInfo.length) {
                if (allelesInfo[searchPointer] == ByteCode.COMMA) {
                    out[alleleIndex++] = ArrayUtils.copyOfRange(allelesInfo, lastPointer, searchPointer);
                    lastPointer = searchPointer + 1;
                }

                searchPointer++;
            }

            out[alleleIndex] = ArrayUtils.copyOfRange(allelesInfo, lastPointer, allelesInfo.length);

            return out;
        }

        @Override
        public Iterator<VariantCoordinate> iterator() {
            return new Iterator<VariantCoordinate>() {
                int pointer;
                Iterator<VariantCoordinate>[] iterators = new Iterator[pairs.size()];

                {
                    for (int i = 0; i < iterators.length; i++) {
                        iterators[i] = pairs.get(i).iterator();
                    }
                }

                @Override
                public boolean hasNext() {
                    return iterators[pointer].hasNext() || ((pointer < pairs.size() - 1) && iterators[pointer + 1].hasNext());
                }

                @Override
                public VariantCoordinate next() {
                    if (iterators[pointer].hasNext()) {
                        return iterators[pointer].next();
                    } else {
                        pointer++;
                        return iterators[pointer].next();
                    }
                }
            };
        }
    }
}