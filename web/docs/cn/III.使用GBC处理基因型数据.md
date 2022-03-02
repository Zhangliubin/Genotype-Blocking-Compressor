## III. 使用 GBC 处理基因型数据

GBC 的四种界面模式提供以下运行方式，每次运行时至多指定一种运行模式，详见参数一览表：

| 运行模式 | 指令                        | 说明                                     |
| -------- | --------------------------- | ---------------------------------------- |
| build    | build <input(s)\> [options] | 构建GTB存档                              |
| rebuild  | rebuild <input\> [options]  | 从 GTB 文件中重构建 GTB 存档             |
| extract  | extract <input\> [options]  | 提取基因组数据                           |
| edit     | edit <input\> [options]     | GTB 文件编辑                             |
| merge    | merge <input(s)\> [options] | 合并多个具有不重叠样本的 GTB 文件        |
| show     | show <input(s)\> [options]  | 查看 GTB 文件结构                        |
| index    | index <input\> [options]    | 构建与管理 contig 文件                   |
| ld       | ld <input\> [options]       | 计算 LD 系数                             |
| bgzip    | GBC bgzip <mode\> [options] | 使用 Pbgzip 进行压缩、解压、切割 gz 文件 |
| md5      | md5 <input(s)> [--o-md5]    | 校验文件的 MD5 码                        |

### 1. 构建压缩的基因型文件(GTB) —— build 运行模式

使用如下指令对基因组 VCF 文件进行压缩：

```shell
GBC build <input(s)> [options]
```

- GBC 可以为符合 [VCF 文件规范](https://samtools.github.io/hts-specs/VCFv4.2.pdf) 的文件进行压缩，GBC 的所有操作都是假定文件格式是符合此规范的；
- inputFileName 可以是单个 .vcf 文件或 .vcf.gz 文件，也可以是包含这些文件的文件夹路径。当路径为文件夹路径时， GBC 会筛选出该文件夹（及其子文件夹）中所有的 vcf 文件或 .vcf.gz 文件进行压缩。请注意，GBC 仅根据文件的扩展名判断文件类型，因此正确的文件扩展名才能够进行压缩；
- 为了便于统一全局的符号，GBC 当前仅支持对于人类基因组的压缩，CHROM当前只支持 1-22, X, Y, MT 以及带有 chr 前缀的 1-22, X, Y, MT；对于其他物种，需要先[构建 contig 文件](#8. 构建 contig 文件以自定义染色体标签);
- GBC 对多个文件进行合并压缩时，要求这些文件具有相同的样本序列（样本顺序可以不一致）。若一个文件的样本序列是其他文件的子序列，它也可以被正确压缩，缺失的样本基因型将被替换为 .|.。

#### 1.1. 命令行运行示例

```shell
# 压缩 assoc.hg19.vcf.gz 文件 (phased 基因型)
GBC build ./example/assoc.hg19.vcf.gz -p

# 压缩 1000GP3 分群体文件夹 (AFR, AMR... 是文件夹，包含多个染色体文件)
GBC build ./example/1000GP3/AFR
GBC build ./example/1000GP3/AMR
GBC build ./example/1000GP3/EAS
GBC build ./example/1000GP3/EUR
GBC build ./example/1000GP3/SAS

# 压缩 batchAll 文件，并设置质控参数为 gq >= 5, dp >= 30
GBC build ./example/batchAll.vcf.gz --gty-gq 30 --gty-dp 5
```

#### 1.2. 参数一览

**压缩器参数：**

| 参数                     | 参数类型/用法                | 描述                                                         |
| :----------------------- | ---------------------------- | ------------------------------------------------------------ |
| --contig                 | --contig <file>            | 指定染色体标签文件                                           |
| --output<br />-o         | -o <file>                  | 设置输出文件名                                               |
| --threads<br />-t        | -t <int, ≥1>                | 并行压缩的线程数 (默认: 4)                                   |
| --phased<br />-p         | -p                           | 设置基因型数据为有向型                                       |
| --blockSizeType<br />-bs | -bs <int, 0~7>             | 设定每个压缩块的最大位点数，根据 $2^{7+x}$​ 换算得到真实的块大小值 (默认: -1) |
| --no-reordering<br />-nr | -nr                          | 不使用 AMDO 算法对基因型阵列进行重排列                       |
| --windowSize<br />-ws    | -ws <1~131072>             | 设定 AMDO 算法采样窗口的大小 (默认: 24)                      |
| --compressor<br />-c     | -c [0/1]<br />-c [ZSTD/LZMA] | 设置压缩数据的算法 (0: ZSTD, 1: LZMA, 2: EMPTY)，预留的 2 和 3 可以由其他开发人员配置 |
| --level<br />-l          | -l <int>                   | 压缩器参数，较大的参数将有助于提升压缩比，但也会带来时间上的额外开销 (ZSTD: 0~22, 默认值: 16; LZMA: 0~9, 默认值: 3) |
| --readyParas<br />-rp    | -rp <file>                 | 使用外置 GTB 文件的参数作为模版参数 (覆盖默认的 -p, -bs, -c, -l) |
| --yes<br />-y            | -y                           | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

**质量控制参数：**

| 参数         | 参数类型/用法                                                | 描述                                                         |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --gty-gq     | --gty-gq <int, ≥0>                                         | 样本基因型GQ分数值小于指定值时，该基因型将被替换为“.\|.” (默认: 20) |
| --gty-dp     | --gty-dp <int, ≥0>                                         | 样本基因型DP分数值小于指定值时，该基因型将被替换为“.\|.” (默认: 4) |
| --seq-qual   | --seq-qual <int, ≥0>                                         | 位点QUAL分数值小于指定值时，该位点将被移除 (默认: 30)        |
| --seq-dp     | --seq-dp <int, ≥0>                                           | 位点DP分数值小于指定值时，该位点将被移除 (默认: 0)           |
| --seq-mq     | --seq-mq <int, ≥0>                                           | 位点MQ分数值小于指定值时，该位点将被移除 (默认: 20)          |
| --seq-ac     | --seq-ac minAC-<br />--seq-ac -maxAC<br />--seq-ac minAC-maxAC | 位点等位基因计数小于minAC、大于maxAC或不在[minAC, maxAC]内时，该位点将被移除 |
| --seq-af     | --seq-af minAF-<br />--seq-af -maxAF<br />--seq-ac minAF-maxAF | 位点等位基因频率小于minAF、大于maxAF或不在[minAF, maxAF]内时，该位点将被移除 |
| --seq-an     | --seq-an minAN-<br />--seq-an -maxAN<br />--seq-an minAN-maxAN | 位点有效等位基因个数小于minAN、大于maxAN或不在[minAN, maxAN]内时，该位点将被移除 |
| --max-allele | --max-allele <int, 2~15>                                   | 排除等位基因种类超过 [x] 的位点                              |
| --no-qc      | --no-qc                                                      | 禁用所有的质量控制方法                                       |

#### 1.3. 参数建议

- 提取基因型数据后会损失基因型有关的质量信息，而低质量的测序数据是不可信的。因此 GBC 的质控流程默认是启动的 （如果存在质量信息的话）。若不需要进行质控，可以设置参数 --no-qc；
- AMDO 算法在多数情况下都能显著提高压缩效果（提升压缩比和压缩速度）；
- 并行模式下，GBC 压缩的速度取决于磁盘 IO 速度。因此，在磁盘速度较慢的主机上运行 GBC 时，并行可能不会起到加速作用；
- Java 中单个数组对象最大长度为 2GB，基于此计算出样本量-块大小参数换算表如下：

| 块大小参数 | 块位点个数 | 样本量范围    | 块大小参数 | 块位点个数 | 样本量范围     |
| ---------- | ---------- | ------------- | ---------- | ---------- | -------------- |
| -bs 7      | 16384      | $\le 131071$  | -bs 3      | 1024       | $\le 2097151$  |
| -bs 6      | 8192       | $\le 262143$  | -bs 2      | 512        | $\le 4194303$  |
| -bs 5      | 4096       | $\le 524287$  | -bs 1      | 256        | $\le 8388607$  |
| -bs 4      | 2048       | $\le 1048575$ | -bs 0      | 128        | $\le 16777215$ |

#### 1.4. 在 Java 中使用 API 工具

```java
// 压缩 assoc.hg19.vcf.gz 文件, 设置为有向，并自定义过滤器
BuildTask task = new BuildTask("./example/assoc.hg19.vcf.gz");
task.setPhased(true);
task.addAlleleQC(new IAlleleQC() {
    @Override
    public boolean filter(int alleleCount, int validAllelesNum) {
        float alleleFreq = (float) alleleCount / validAllelesNum;
        // compress variants with 0 < MAF < 0.05
        return !((alleleFreq > 0 && alleleFreq < 0.05) || (alleleFreq < 1 && alleleFreq > 0.95));
    }

    @Override
    public boolean empty() {
        return false;
    }
});
task.submit();

// 压缩 1000GP3 分群体文件夹
for (String population: new String[]{"AFR", "AMR", "EAS", "EUR", "SAS"}){
    task = new BuildTask("./example/1000GP3/" + population);
    task.submit();
}

// 压缩 batchAll 文件，并设置质控参数为 gq >= 5, dp >= 30
task = new BuildTask("./example/batch.all", "./example/batch.all.filterGQ5_DP30.gtb")
        .setGenotypeQualityControlDp(30)
        .setGenotypeQualityControlGq(5);
task.submit();
```

### 2. 重构 GTB 文件 —— rebuild 运行模式

使用如下指令对 GTB 文件进行重构：

```shell
GBC rebuild <input> [options]
```

- 重构模式用于永久地修改 GTB 文件信息，这些操作会涉及到重新解压 GTB 文件、重新压缩的过程，因此该方法也能用于进行文件排序；
- 对于节点级别的操作 (移除某些节点、保留某些节点) 及头文件修改，我们更建议使用 edit 模式完成。因为这些操作不需要解压文件这一过程，从而可以大幅地提升速度。

#### 2.1. 命令行运行示例

```shell
# 重构 assoc.hg19.gtb 文件，设置基因型为有向、块大小为 4096，6 线程
GBC rebuild ./example/assoc.hg19.gtb --phased --threads 6 --blockSizeType 5 --output ./example/assoc.hg19.unphased.gtb
```

#### 2.2. 参数一览

**压缩器参数：**

| 参数                     | 参数类型/用法                | 描述                                                         |
| :----------------------- | ---------------------------- | ------------------------------------------------------------ |
| --contig                 | --contig <file>            | 指定染色体标签文件                                           |
| --output<br />-o         | -o <file>                  | 设置输出文件名 (默认与输入文件名相同，即覆盖原文件)          |
| --threads<br />-t        | -t <int, ≥1>                | 并行压缩的线程数 (默认: 4)                                   |
| --phased<br />-p         | -p                           | 设置基因型数据为有向型                                       |
| --blockSizeType<br />-bs | -bs <int, 0~7>             | 设定每个压缩块的最大位点数，根据 $2^{7+x}$ 换算得到真实的块大小值 (默认: -1) |
| --no-reordering<br />-nr | -nr                          | 不使用 AMDO 算法对基因型阵列进行重排列                       |
| --windowSize<br />-ws    | -ws <int, 1~131072>        | 设定 AMDO 算法采样窗口的大小 (默认: 24)                      |
| --compressor<br />-c     | -c [0/1]<br />-c [ZSTD/LZMA] | 设置压缩数据的算法 (0: ZSTD, 1: LZMA, 2: EMPTY)，预留的 2 和 3 可以由其他开发人员配置 |
| --level<br />-l          | -l <int>                   | 压缩器参数，较大的参数将有助于提升压缩比，但也会带来时间上的额外开销 (ZSTD: 0~22, 默认值: 16; LZMA: 0~9, 默认值: 3) |
| --readyParas<br />-rp    | -rp <file>                 | 使用外置 GTB 文件的参数作为模版参数 (覆盖默认的 -p, -bs, -c, -l) |
| --yes<br />-y            | -y                           | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

**位点标准化：**

| 参数           | 参数类型/用法  | 描述                                         |
| -------------- | -------------- | -------------------------------------------- |
| --multiallelic | --multiallelic | 将多个二等位基因位点合并为一个多等位基因位点 |
| --biallelic    | --biallelic    | 将多等位基因位点分解为多个二等位基因位点     |

**坐标对齐：**

| 参数                  | 参数类型/用法                          | 描述                                                         |
| --------------------- | -------------------------------------- | ------------------------------------------------------------ |
| --align               | --align <file>                       | 根据指定的 GTB 文件过滤位点和调整参考碱基                    |
| --check-allele        | --check-allele                         | 校正潜在的等位基因互补错误 (A 和 C，T 和 G)                  |
| --p-value             | --p-value <float, 0.000001~0.5>        | 使用卡方检验识别罕见变异潜在的错误等位基因标签  (默认: 0.05) |
| --freq-gap            | --freq-gap <float, 0.000001~0.5>       | 使用等位基因频率差值识别罕见变异潜在的错误等位基因标签       |
| --no-ld               | --no-ld                                | 默认情况下，使用 LD 结构校正常见变异的潜在错误等位基因标签。使用该参数禁用常见变异的校正 |
| --min-r               | --min-r <float, 0.5~1.0>               | 用于确定具有翻转 LD 模式的关联系数阈值 (默认: 0.8)           |
| --flip-scan-threshold | --flip-scan-threshold <float, 0.5~1.0> | 邻近位点中，具有 --flip-scan-threshold 比例的翻转 LD 模式 (相反符号的强相关系数) 将被修正标签 (默认: 0.8) |
| --maf                 | --maf <float, 0~1>                     | 对于常见变异 (次级等位基因频率 MAF >= --maf) 使用邻近位点的 LD 结构识别潜在的错误等位基因标签 (默认: 0.05) |
| --window-bp<br />-bp  | -bp <int, ≥1>                         | 用于 LD 计算的位点的最大物理距离 (默认: 10000)               |
| --method<br />-m      | -m [union/intersection]                | 不同文件中的位点保留策略 (交集 or 并集) (默认: intersection) |

**子集选择：**

| 参数              | 参数类型/用法                                                | 描述                               |
| ----------------- | ------------------------------------------------------------ | ---------------------------------- |
| --subject<br />-s | --subject <string>,<string>,…<br />--subject @<file>   | 保留指定样本的基因型数据           |
| --retain          | --retain chrom=<string>,<string>,...<br />--retain chrom=<string>,<string>,...;node=<int>,<int>,... | 保留指定节点                       |
| --delete          | --delete chrom=<string>,<string>,...<br />--delete chrom=<string>,<string>,...;node=<int>,<int>,... | 移除指定节点                       |
| --range<br />-r   | --range <chrom><br />--range <chrom>:<start>-<br />--range <chrom>:-<end><br />--range <chrom>:<start>-<end> | 按照位点范围进行提取               |
| --random          | --random <file>                                            | 根据保存随机查询位点的文件进行访问 |

**位点过滤：**

| 参数         | 参数类型/用法                                                | 描述                                                         |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --seq-ac     | --seq-ac <minAc>-<br />--seq-ac -<maxAc><br />--seq-ac <minAc>-<maxAc> | 位点等位基因计数小于minAC、大于maxAC或不在[minAC, maxAC]内时，该位点将被移除 |
| --seq-af     | --seq-af <minAf>-<br />--seq-af -<maxAf><br />--seq-af <minAf>-<maxAf> | 位点等位基因频率小于minAF、大于maxAF或不在[minAF, maxAF]内时，该位点将被移除 |
| --seq-an     | --seq-an<minAn>-<br />--seq-an -<maxAn><br />--seq-an <minAn>-<maxAn> | 位点有效等位基因个数小于minAN、大于maxAN或不在[minAN, maxAN]内时，该位点将被移除 |
| --max-allele | --max-allele <int, 2~15>                                   | 排除等位基因种类超过 [x] 的位点                              |

#### 2.3. 在 Java 中使用 API 工具

```java
// 重构 assoc.hg19.gtb 文件，设置基因型为无向、块大小为 4096，6 线程，并提取有突变的位点
RebuildTask task = new RebuildTask("./example/assoc.hg19.gtb", "./example/assoc.hg19.unphased.gtb");
task.setPhased(true)
        .setParallel(6)
        .setBlockSizeType(5)
        .filterByAC(1, 1965);

task.rebuildAll();
```

### 3. 从压缩的 GTB 文件中提取数据 —— extract 运行模式

使用如下指令对 GTB 文件进行数据提取：

```shell
GBC extract <input> [options]
```

#### 3.1. 命令行运行示例

```shell
# 解压 assoc.hg19.gtb 的所有位点，并将输出文件压缩为 bgzf 格式
GBC extract ./example/assoc.hg19.gtb --output ./example/assoc.hg19.simple.vcf.gz

# 从 1000GP3 中提取 1 号染色体 10177-1000000 的数据
GBC extract ./example/1000GP3.phased.gtb --range 1:10177-1000000

# 从 1000GP3 中提取 1,2,3,4,5 号染色体数据， 并按照染色体编号分别保存为文本格式
GBC extract ./example/1000GP3.phased.gtb --node chrom=1,2,3,4,5 --o-text

# 根据 随机位置文件 ./example/query_1000GP3.txt 获取指定位点数据
GBC extract ./example/1000GP3.phased.gtb --ramdom ./example/query_1000GP3.txt

# 在 assoc.hg19 中提取等位基因计数 1~20 的位点
GBC extract ./example/assoc.hg19.gtb --seq-ac 1-20
```

#### 3.2. 参数一览

**输出参数：**

| 参数              | 参数类型/用法     | 描述                                                         |
| ----------------- | ----------------- | ------------------------------------------------------------ |
| --contig          | --contig <file> | 指定染色体标签文件                                           |
| --output<br />-o  | -o <file>       | 设置输出文件名                                               |
| --o-text          | --o-text          | 输出为文本格式文件 (当指定了 -o 参数并且该文件名不以 .gz 结尾时，该命令会被自动执行) |
| --o-bgz           | --o-bgz           | 使用 bgzip 压缩输出文件 (当指定了 -o 参数并且该文件名以 .gz 结尾时，该命令会被自动执行) |
| --level<br />-l   | -l <int, 0~9>   | 设置 bgzip 压缩的级别 (默认: 5)                              |
| --threads<br />-t | -t <int, ≥1>     | 并行压缩的线程数 (默认: 4)                                   |
| --phased<br />-p  | -p [true/false]   | 强制转换基因型数据向型，默认情况下基因型向型与压缩时指定的phased一致 |
| --hideGT<br />-hg | -hg               | 不输出基因型数据                                             |
| --yes<br />-y     | -y                | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

**子集选择：**

| 参数              | 参数类型/用法                                                | 描述                                                         |
| ----------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --subject<br />-s | --subject <string>,<string>,…<br />--subject @<file>   | 提取指定样本的基因型数据                                     |
| --range<br />-r   | --range <chrom><br />--range <chrom>:<start>-<br />--range <chrom>:-<end><br />--range <chrom>:<start>-<end> | 按照位点范围进行提取                                         |
| --random          | --random <file>                                            | 根据保存随机查询位点的文件进行访问（每一行代表一个位点，使用 ',' 或 '\t' 作为染色体与位置值的分隔符） |
| --node            | --node chrom=<string>,<string>,...<br />--node chrom=<string>,<string>,...;node=<int>,<int>,... | 按照GTB节点进行提取                                          |

**位点过滤参数：**

| 参数     | 参数类型/用法                                                | 描述                                                         |
| -------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --seq-ac | --seq-ac <minAc>-<br />--seq-ac -<maxAc><br />--seq-ac <minAc>-<maxAc> | 位点等位基因计数小于minAC、大于maxAC或不在[minAC, maxAC]内时，该位点将被移除 |
| --seq-af | --seq-af <minAf>-<br />--seq-af -<maxAf><br />--seq-af <minAf>-<maxAf> | 位点等位基因频率小于minAF、大于maxAF或不在[minAF, maxAF]内时，该位点将被移除 |
| --seq-an | --seq-an<minAn>-<br />--seq-an -<maxAn><br />--seq-an <minAn>-<maxAn> | 位点有效等位基因个数小于minAN、大于maxAN或不在[minAN, maxAN]内时，该位点将被移除 |

#### 3.3. 在 Java 中使用 API 工具

```java
// 解压 assoc.hg19.gtb 的所有位点
ExtractTask task = new ExtractTask("./example/assoc.hg19.gtb");
task.decompressAll();

// 解压 assoc.hg19.gtb 的所有位点，并将输出文件压缩为 bgzf 格式
task = new ExtractTask("./example/assoc.hg19.gtb", "./example/assoc.hg19.simple.vcf.gz")
        .setCompressToBGZF();
task.decompressAll();

// 从 1000GP3 中提取 1 号染色体 10177-1000000 的数据
task = new ExtractTask("./example/1000GP3.phased.gtb");
task.decompressByRange(1, 10177, 1000000);

// 从 1000GP3 中提取 3 号染色体数据
task = new ExtractTask("./example/1000GP3.phased.gtb");
task.decompressByChromosome(3);

// 根据 随机位置文件 ./example/query_1000GP3.txt 获取指定位点数据
task = new ExtractTask("./example/1000GP3.phased.gtb");
task.decompressByPositionFromFile("query_1000GP3.txt", "\n", ",");

// 在 assoc.hg19 中提取等位基因计数 1~20 的位点
task = new ExtractTask("./example/1000GP3.phased.gtb").filterByAC(1, 20);
task.decompressAll();
```

### 4. 编辑 GTB 文件 —— edit 运行模式

使用如下指令对 GTB 文件进行编辑：

```shell
GBC edit <input> [options]
```

编辑模式可以在不解压基因组文件下进行节点的操作，可以快速进行文件切片、剔除、合并操作。编辑模式建议在图形界面中使用，更为直观。

#### 4.1. 命令行运行示例

```shell
# 分别提取出节点1和节点2
GBC edit ./example/assoc.hg19.gtb --output ./example/node1.gtb --retain "chrom=1;node=1"
GBC edit ./example/assoc.hg19.gtb --output ./example/node2.gtb --retain "chrom=1;node=2"

# 合并为 node1_2_2.gtb
GBC edit ./example/node1.gtb --output ./example/node1_2_2.gtb --concat ./example/node2.gtb

# 直接提取节点1和节点2
GBC edit ./example/assoc.hg19.gtb --output ./example/node1_2_1.gtb --retain "chrom=1;node=1,2"

# 使用md5校验两种方式生成的文件是否包含相同数据
GBC md5 ./example/node1_2_1.gtb ./example/node1_2_2.gtb

# 将整体的 1000GP3 文件重新拆分为按染色体编号组织的数据
GBC edit ./example/1000GP3.phased.gtb --split --output ./example/1000GP3.phased
```

#### 4.2. 参数一览

| **参数**         | **参数类型**                                                 | **描述**                                                     |
| ---------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --contig         | --contig <file>                                            | 指定染色体标签文件                                           |
| --output<br />-o | -o <file>                                                  | 输出文件名，未传入该参数时，编辑后的文件**将覆盖原文件**     |
| --unique         | --unique                                                     | 节点去重                                                     |
| --delete         | --delete chrom=<string>,<string>,...<br />--delete chrom=<string>,<string>,...;node=<int>,<int>,... | 移除指定节点                                                 |
| --retain         | --retain chrom=<string>,<string>,...<br />--retain chrom=<string>,<string>,...;node=<int>,<int>,... | 保留指定节点                                                 |
| --concat         | --concat <file> <file> ...                               | 合并GTB文件，这些 GTB 文件需要有相同的样本名序列、相同的向型、相同的组合编码标记 |
| --reset-subject  | --subject <string>,<string>,…<br />--subject @<file>   | 重设样本名。输入的样本名长度必须与原文件相同                 |
| --split, -s      | --split                                                      | 按照染色体编号拆分文件                                       |
| --yes<br />-y    | -y                                                           | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

在一次指令运行时，--retain, --delete, --merge, --unique 至多只能指定一个 (GUI 编辑模式、API 调用则无此限制)。

#### 4.3. 在 Java 中使用 API 工具

```java
// 分别提取出节点1和节点2
EditTask task = new EditTask("./example/assoc.hg19.gtb");
task.setOutputFileName("./example/node1.gtb").retain(1, new int[]{1}).submit();
task.setOutputFileName("./example/node2.gtb").retain(1, new int[]{2}).submit();

// 直接提取节点1和节点2
task.setOutputFileName("./example/node1_2_1.gtb").retain(1, new int[]{1, 2}).submit();

// 合并节点1和节点2
task = new EditTask("./example/node1.gtb")
        .concat("./example/node2.gtb")
        .setOutputFileName("./example/node1_2_2.gtb");
task.submit();

// 将整体的 1000GP3 文件重新拆分为按染色体编号组织的数据
task = new EditTask("./example/1000GP3.phased.gtb")
        .split(true);
task.submit();
```

### 5. 合并多个 GTB 文件 —— merge 运行模式

使用如下指令合并多个 GTB 文件：

```shell
GBC merge <input(s)> [options]
```

- 这些文件必须具有不重叠的样本名。样本名重叠时，可以使用 `edit <GTBFileName> --reset-subject subject1,subject2,...` 或  `edit <GTBFileName> --reset-subject @file` 重设单个文件的样本信息；
- 输入文件必须是有序的。文件无序时，可以使用 `rebuild <input>` 进行排序；
- 多个文件合并 ($\ge3$) 时，合并的顺序由样本个数作为权重的最小堆确定。

#### 5.1. 命令行运行示例

```shell
# 合并 1000GP3 数据集
GBC merge ./example/1000GP3/AMR.gtb ./example/1000GP3/AFR.gtb \
./example/1000GP3/EAS.gtb ./example/1000GP3/EUR.gtb ./example/1000GP3/SAS.gtb \ 
--phased --threads 6 --blockSizeType 5 \
--output ./example/1000GP3.gtb
```

#### 5.2. 参数一览

**压缩器参数：**

| 参数                     | 参数类型/用法                | 描述                                                         |
| :----------------------- | ---------------------------- | ------------------------------------------------------------ |
| --contig                 | --contig <file>            | 指定染色体标签文件                                           |
| --method<br />-m         | -m [union/intersection]      | 不同文件中的位点保留策略 (交集 or 并集) (默认: intersection) |
| --biallelic              | --biallelic                  | 将合并后的多等位基因位点分解为二等位基因位点                 |
| --output<br />-o         | -o <file>                  | 设置输出文件名 (默认与输入的第一个文件名相同，即覆盖原文件)  |
| --threads<br />-t        | -t <int, ≥1>                | 并行压缩的线程数 (默认: 4)                                   |
| --phased<br />-p         | -p                           | 设置基因型数据为有向型                                       |
| --blockSizeType<br />-bs | -bs <int, 0~7>             | 设定每个压缩块的最大位点数，根据 $2^{7+x}$ 换算得到真实的块大小值 (默认: -1) |
| --no-reordering<br />-nr | -nr                          | 不使用 AMDO 算法对基因型阵列进行重排列                       |
| --windowSize<br />-ws    | -ws <int, 1~131072>        | 设定 AMDO 算法采样窗口的大小 (默认: 24)                      |
| --compressor<br />-c     | -c [0/1]<br />-c [ZSTD/LZMA] | 设置压缩数据的算法 (0: ZSTD, 1: LZMA, 2: EMPTY)，预留的 2 和 3 可以由其他开发人员配置 |
| --level<br />-l          | -l <int>                   | 压缩器参数，较大的参数将有助于提升压缩比，但也会带来时间上的额外开销 (ZSTD: 0~22, 默认值: 16; LZMA: 0~9, 默认值: 3) |
| --readyParas<br />-rp    | -rp <file>                 | 使用外置 GTB 文件的参数作为模版参数 (覆盖默认的 -p, -bs, -c, -l) |
| --yes<br />-y            | -y                           | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

**识别潜在的错误等位基因标签：**

| 参数                  | 参数类型/用法                          | 描述                                                         |
| --------------------- | -------------------------------------- | ------------------------------------------------------------ |
| --check-allele        | --check-allele                         | 校正潜在的等位基因互补错误 (A 和 C，T 和 G)                  |
| --p-value             | --p-value <float, 0.000001~0.5>        | 使用卡方检验识别罕见变异潜在的错误等位基因标签  (默认: 0.05) |
| --freq-gap            | --freq-gap <float, 0.000001~0.5>       | 使用等位基因频率差值识别罕见变异潜在的错误等位基因标签       |
| --no-ld               | --no-ld                                | 默认情况下，使用 LD 结构校正常见变异的潜在错误等位基因标签。使用该参数禁用常见变异的校正 |
| --min-r               | --min-r <float, 0.5~1.0>               | 用于确定具有翻转 LD 模式的关联系数阈值 (默认: 0.8)           |
| --flip-scan-threshold | --flip-scan-threshold <float, 0.5~1.0> | 邻近位点中，具有 --flip-scan-threshold 比例的翻转 LD 模式 (相反符号的强相关系数) 将被修正标签 (默认: 0.8) |
| --maf                 | --maf <float, 0~1>                     | 对于常见变异 (次级等位基因频率 MAF >= --maf) 使用邻近位点的 LD 结构识别潜在的错误等位基因标签 (默认: 0.05) |
| --window-bp<br />-bp  | -bp <int, ≥1>                         | 用于 LD 计算的位点的最大物理距离 (默认: 10000)               |

**位点过滤：**

| 参数         | 参数类型/用法                                                | 描述                                                         |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --seq-ac     | --seq-ac <minAc>-<br />--seq-ac -<maxAc><br />--seq-ac <minAc>-<maxAc> | 位点等位基因计数小于minAC、大于maxAC或不在[minAC, maxAC]内时，该位点将被移除 |
| --seq-af     | --seq-af <minAf>-<br />--seq-af -<maxAf><br />--seq-af <minAf>-<maxAf> | 位点等位基因频率小于minAF、大于maxAF或不在[minAF, maxAF]内时，该位点将被移除 |
| --seq-an     | --seq-an<minAn>-<br />--seq-an -<maxAn><br />--seq-an <minAn>-<maxAn> | 位点有效等位基因个数小于minAN、大于maxAN或不在[minAN, maxAN]内时，该位点将被移除 |
| --max-allele | --max-allele <int, 2~15>                                   | 排除等位基因种类超过 [x] 的位点                              |

#### 5.3. 在 Java 中使用 API 工具

```java
// 合并 1000GP3 文件，设置基因型为无向、块大小为 8192，10 线程
MergeTask task = new MergeTask(new String[]{"./example/1000GP3/AMR.gtb",
        "./example/1000GP3/EAS.gtb", "./example/1000GP3/EUR.gtb",
        "./example/1000GP3/SAS.gtb", "./example/1000GP3/AFR.gtb"},
        "./example/1000GP3.gtb");
task.setPhased(false)
        .setParallel(10)
        .setBlockSizeType(6);
task.submit();
```

### 6 查看 GTB 文件的基本信息 —— show 运行模式

使用如下指令查看 GTB 文件：

```shell
GBC show <input> [options]
```

#### 6.1. 命令行运行示例

```shell
# 查看压缩的 1000GP3.gtb 文件信息
GBC show ./example/1000GP3.phased.gtb

# 查看压缩的 assoc.hg19.gtb 所有信息
GBC show ./example/assoc.hg19.gtb --full

# 查看 1000GP3.gtb 文件的所有样本名
GBC show ./example/1000GP3.phased.gtb --list-subject-only > ./example/1000GP3.phased.subjects.txt

# 查看 1000GP3.gtb 文件的所有位点信息
GBC show ./example/1000GP3.phased.gtb --list-site
```

#### 6.2. 参数一览

**公共参数：**

| 参数                | 参数类型/用法                                 | 描述                 |
| ------------------- | --------------------------------------------- | -------------------- |
| --contig            | --contig <file>                             | 指定染色体标签文件   |
| --assign-chromosome | --assign-chromosome <string>,<string>,... | 列出指定染色体的信息 |

**GTB 汇总信息参数：**

| 参数            | 参数类型/用法   | 描述                                          |
| --------------- | --------------- | --------------------------------------------- |
| --list-md5      | --list-md5      | 计算文件的 MD5 码，大文件计算将会花费更多时间 |
| --list-baseInfo | --list-baseInfo | 列出文件的基本信息 (GTB 文件的前 2 字节)      |
| --list-subject  | --list-subject  | 列出所有的样本名                              |
| --list-tree     | --list-tree     | 列出 GTB 节点树（染色体级别）                 |
| --list-node     | --list-node     | 列出 GTB 节点树（所有的节点）                 |
| --full, -f      | --full          | 列出所有信息 (除了 md5 码)                    |

**GTB 访问参数：**

| 参数                 | 参数类型/用法        | 描述                                                 |
| -------------------- | -------------------- | ---------------------------------------------------- |
| --list-subject-only  | --list-subject-only  | 仅列出所有的样本名                                   |
| --list-position-only | --list-position-only | 列出坐标信息 (CHROM, POSITION)                       |
| --list-site          | --list-site          | 列出详细的位点信息 (CHROM, POSITION, REF, ALT, INFO) |
| --list-gt            | --list-gt            | 列出基因型频率                                       |

#### 6.3. 在 Java 中使用 API 工具

```java
GTBManager manager = GTBRootCache.get("./example/assoc.hg19.gtb");
ManagerStringBuilder stringBuilder = manager.getManagerStringBuilder();
// 计算文件的 md5 码
stringBuilder.calculateMd5(true);
// 列出文件基本信息
stringBuilder.listFileBaseInfo(true);
// 列出染色体树表
stringBuilder.listChromosomeInfo(true);
System.out.println(stringBuilder.build());
```

### 7. 构建或转换 contig 文件以自定义染色体标签 —— index 运行模式

使用如下指令对 VCF 文件进行染色体标签管理 (构建或转换)：

```shell
GBC index <input (VCF or GTB)> [options]
```

#### 7.1. 命令行运行示例

```shell
# 构建不同物种的 contig 文件 (http://dog10kgenomes.org/download.html)
GBC index ./example/Canis_familiaris.V85.vcf.gz --deep-scan

# 转换 contig 信息
GBC index ./example/assoc.hg19.gtb -to ./example/Canis_familiaris.V85.contig
```

#### 7.2. 参数一览

| **参数**                 | **参数类型** | **描述**                                                     |
| ------------------------ | ------------ | ------------------------------------------------------------ |
| --deep-scan              | --deep-scan  | 扫描所有的位点构建 contig 文件 (默认情况下只扫描注释行)      |
| --output<br />-o         | -o <file>    | 设置输出文件名 (默认与输入的第一个文件名相同，即覆盖原文件)  |
| --from-contig<br />-from | -from <file> | 指定当前 GTB 文件的染色体标签映射文件                        |
| --to-contig<br />-to     | -to <file>   | 为指定的 GTB 文件重设染色体标签映射文件                      |
| --yes<br />-y            | -y           | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

#### 7.3. 在 Java 中使用 API 工具

```java
// 加载染色体标签文件
ChromosomeInfo.load("./example/origin.contig");

// 重设文件的染色体标签
GTBManager manager = GTBRootCache.get("./example/assoc.hg19.gtb");
manager.resetContig("./example/new.contig");
manager.toFile("./example/assoc.hg19.newcotig.gtb");
```

### 8. 计算连锁不平衡系数 —— ld 运行模式

使用如下指令对 **有序 GTB 文件**计算连锁不平衡系数：

```shell
GBC ld <input> [options]
```

#### 8.1. 命令行运行示例

```shell
# 计算 1000 GP3 的连锁不平衡系数
GBC ld ./example/1000GP3.phased.gtb --model --geno --window-bp 10000 --maf 0.1 --min-r2 0.2
GBC ld ./example/1000GP3.phased.gtb --model --hap
```

#### 8.2. 参数一览

**输出参数:** 

| **参数**          | **参数类型**    | **描述**                                                     |
| ----------------- | --------------- | ------------------------------------------------------------ |
| --output<br />-o  | -o <file>     | 输出文件名，未传入该参数时，编辑后的文件**将覆盖原文件**     |
| --o-text          | --o-text        | 输出为文本格式文件 (当指定了 -o 参数并且该文件名不以 .gz 结尾时，该命令会被自动执行) |
| --o-bgz           | --o-bgz         | 使用 bgzip 压缩输出文件 (当指定了 -o 参数并且该文件名以 .gz 结尾时，该命令会被自动执行) |
| --level<br />-l   | -l <int, 0~9> | 设置 bgzip 压缩的级别 (默认: 5)                              |
| --threads<br />-t | -t <int, ≥1>   | 并行压缩的线程数 (默认: 4)                                   |
| --yes<br />-y     | -y              | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

**LD 计算参数:** 

| **参数**             | **参数类型**           | **描述**                                                     |
| -------------------- | ---------------------- | ------------------------------------------------------------ |
| --contig             | --contig <file>      | 指定染色体标签文件                                           |
| --model<br />-m      | -m <string>          | 设置用于计算的 LD 模型，当前适配 Haplotype LD (hap, --hap, --hap-ld, --hap-r2) 计算和 Genotype LD (geno, --geno, --geno-ld, --geno-r2) 计算 (默认: phased 的 GTB 文件将计算 Haplotype LD，unphased 的 GTB 文件将计算 Genotype LD) |
| --window-bp<br />-bp | -bp <int, ≥1>         | 设置计算成对位点的最大物理距离 (默认: 10000)                 |
| --window-kb<br />-kb | -kb <int, ≥1>         | 设置计算成对位点的最大物理距离 (1kb = 1000bp)                |
| --min-r2             | --min-r2 <float, 0~1> | 排除相关系数小于 --min-r2 的位点对 (默认: 0.2)               |
| --maf                | --maf <float, 0~1>    | 排除次级等位基因频率 (MAF) 低于指定 --maf 的位点 (默认: 0.05) |

**样本选择与位点选择:** 

| **参数**          | **参数类型**                                                 | **描述**                   |
| ----------------- | ------------------------------------------------------------ | -------------------------- |
| --subject<br />-s | --subject <string>,<string>,…<br />--subject @<file>   | 计算指定样本的 LD 系数     |
| --range<br />-r   | --range <chrom><br />--range <chrom>:<start>-<br />--range <chrom>:-<end><br />--range <chrom>:<start>-<end> | 计算指定范围位点的 LD 系数 |

#### 8.3. 在 Java 中使用 API 工具

```java
LDTask task = new LDTask("./example/1000GP3.phased.gtb");
task.setWindowSizeBp(10000)
        .setMaf(0.1)
        .setMinR2(0.2)
        .setLdModel(ILDModel.GENOTYPE_LD);
task.submit();
```

### 9. 辅助工具

#### 9.1. 并行 bgzip 压缩工具

GBC 集成了纯 Java 版本的并行 bgzip 压缩工具，使用如下指令运行 bgzip 工具：

```shell
GBC bgzip <mode> [options]
```

| **模式**   | **描述**                      |
| ---------- | ----------------------------- |
| compress   | 使用并行 bgzip 压缩文件       |
| convert    | 将 gz 格式转换为 bgz 格式     |
| decompress | 解压缩                        |
| extract    | 提取指定范围的数据            |
| concat     | 使用 bgzip 压缩时的压缩级别   |
| md5        | 计算 gz 文件解压内容的 md5 码 |

#### 9.2. MD5 码校验工具

使用如下指令校验文件的 MD5 码：

```shell
GBC md5 <input(s)> [--o-md5]
```

其中，可选参数 `--o-md5`表示在输入文件的位置导出相应的 md5 文件。

 
