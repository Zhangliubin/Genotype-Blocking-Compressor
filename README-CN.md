## I. 简介

GenoType Blocking Compressor (简称 GBC) 是一个基因型数据分块压缩器，它旨在为 VCF 文件中的基因型数据创建一个统一、灵活的结构 GenoType Block (简称 GTB)。使用 GTB 格式替代传统的 gz 格式，用户可以实现更少的硬盘空间占用、更快速的数据访问与提取、更方便的群体文件管理以及更高效的数据分析功能。GBC 不仅包含了压缩、解压、查询数据，也包括基本质量控制、连锁不平衡计算、利用分块压缩的结构进行群体文件管理的功能。

> 技术问题请联系 张柳彬(suranyi.sysu@gmail.com)

## II. 安装和配置运行环境

### 1. 下载 GBC 及相关文件

| Type                        | File                                                         |
| --------------------------- | ------------------------------------------------------------ |
| Source codes                | [GBC-1.1](https://github.com/Zhangliubin/Genotype-Blocking-Compressor) |
| Example                     | [Instance](http://pmglab.top/gbc/download/example.zip)       |
| Docker Images (Docker File) | [Dockerfile](http://pmglab.top/gbc/download/dockerfile)      |
| Package/Software            | [gbc-1.1.jar](./download/gbc-1.1.jar)                        |

### 2. 系统要求

- ZSTD 是 Facebook 的 Yann Colle 基于 c 语言开发的一个无损数据压缩算法，它以 Java Native Interface 的形式接入到 Java 中进行使用（即 [zstd-jni](https://github.com/luben/zstd-jni)）。zstd-jni 的维护者 Luben 发布了 [免编译版的 jar 包程序](https://repo1.maven.org/maven2/com/github/luben/zstd-jni/)。gbc 集成了 [zstd-jni-1.4.9-5.jar](https://repo1.maven.org/maven2/com/github/luben/zstd-jni/1.4.9-5/zstd-jni-1.4.9-5.jar)，该版本在多数计算机设备上都可以免编译使用。对于无法正常驱动 ZSTD 的系统，我们提供了 LZMA 压缩的版本作为替代，仅需在压缩时添加参数 `-c LZMA` 或 `-c 1`。

- GBC 是基于 Oracle JDK 8 开发的程序，任何支持或兼容 Oracle JDK 8 的计算机设备都可以使用。用户需要先下载安装 [Oracle JDK](https://www.oracle.com/cn/java/technologies/javase-downloads.html) 或 [Open JDK](https://openjdk.java.net/install/)。Apple Silicon 设备可以使用 [zulu JDK](https://www.azul.com/downloads/?package=jdk#download-openjdk) 作为替代。

### 3. 运行要求

- GBC 需要更多的内存以处理大型基因组文件，建议始终分配不小于 4GB 的堆内存运行 gbc 程序：

```shell
java -Xms4g -Xmx4g -jar gbc.jar
```

- 在 Linux 系统或 MacOs 系统，用户可以通过设置 Shell alias 指令，以更精简的指令使用 GBC：

```shell
# Linux: gbc 的路径必须是绝对路径
echo "alias GBC='java -Xmx4g -Xmx4g -jar ~/bin/gbc.jar'" >> /.bashrc
source ~/.bashrc

# MacOs: gbc 的路径必须是绝对路径
echo "alias GBC='java -Xmx4g -Xmx4g -jar ~/bin/gbc.jar'" >> /.zshrc
source ~/.zshrc		  
```

配置完成后，用户可以在终端使用 `GBC <mode> [options]` 指令直接使用 GBC 软件。

### 4. 示例数据

本文档中使用了 3 个主要的数据集：

- [assoc.hg19.vcf.gz](http://pmglab.top/kggseq/download.htm) , 10.42 MB, 983 samples, 78895 variants.
- [1000G Phase3 v5 Shapeit2 Reference (hg19)](http://pmglab.top/genotypes/) , 27.45 GB, 2504 samples, 84801880 variants.
- batch.all, 181.790 GB, 992 samples, 38517169 variants. (注：该数据集包含大量与测序质量有关的信息，在本例中用于展示 GBC 的质控功能)

### 5. 启动 GBC

GBC 拥有四种界面模式（或使用方式）：命令行模式、命令行交互调用模式、图形界面模式、API 工具，四种方式采用如下方式启动：

- 命令行模式：`java -Xms4g -Xmx4g -jar gbc.jar <mode> [options]`，有关 \<mode\> 及对应参数 [options] 的设置详见后文

  ```shell
  java -Xms4g -Xmx4g -jar gbc.jar build ./example/assoc.hg19.vcf.gz
  ```

- 命令行交互调用模式：

  - 在命令行中使用 `java -Xms4g -Xmx4g -jar gbc.jar -i` 启动命令行交互调用模式
  - 在命令行运行模式中添加参数 `-i` ，程序将在一次指令运行结束后进入交互模式

  ```shell
  java -Xms4g -Xmx4g -jar gbc.jar build ./example/assoc.hg19.vcf.gz -i
  ```

- 图形界面模式：

  - 配置 JDK 环境变量后，图形界面操作系统可以双击启动界面模式，但此时无法指定运行内存大小
  - 使用命令行启动界面模式：`java -Xms4g -Xmx4g -jar gbc.jar`


![setup](images/setup.jpg)

- API 工具：通过导入本 gbc.jar 包使用

## III. 使用 GBC 处理基因型数据

GBC 的四种界面模式提供以下运行方式，每次运行时至多指定一种运行模式，详见参数一览表：

| 运行模式 | 指令                                                  | 说明                                     |
| -------- | ----------------------------------------------------- | ---------------------------------------- |
| build    | build <inputFileName1, inputFileName2, ...> [options] | 构建GTB存档                              |
| rebuild  | rebuild \<inputFileName\> [options]                   | 从 GTB 文件中重构建 GTB 存档             |
| merge    | merge <GTBFileName1, GTBFileName2, ...> [options]     | 合并多个具有不重叠样本的 GTB 文件        |
| show     | show <inputFileName1, inputFileName2, ...> [options]  | 查看 GTB 文件结构                        |
| extract  | extract \<inputFileName\> [options]                   | 提取基因组数据                           |
| edit     | edit \<inputFileName\> [options]                      | GTB 文件编辑                             |
| ld       | ld \<inputFileName\> [options]                        | 计算 LD 系数                             |
| index    | index \<inputFileName\> [options]                     | 构建与管理 contig 文件                   |
| bgzip    | bgzip \<inputFileName\> [options]                     | 使用 Pbgzip 进行压缩、解压、切割 gz 文件 |
| md5      | md5 <inputFileName1, inputFileName2, ...> [options]   | 校验文件的 MD5 码                        |

### 1. 构建压缩的 GTB 文件 —— build 运行模式

使用如下指令对基因组 VCF 文件进行压缩：

```shell
GBC build <inputFileName1, inputFileName2, ...> [options]
```

- GBC 可以为符合 [VCF 文件规范](https://samtools.github.io/hts-specs/VCFv4.2.pdf) 的文件进行压缩，GBC 的所有操作都是假定文件格式是符合此规范的；
- inputFileName 可以是单个 .vcf 文件或 .vcf.gz 文件，也可以是包含这些文件的文件夹路径。当路径为文件夹路径时， GBC 会筛选出该文件夹（及其子文件夹）中所有的 vcf 文件或 .vcf.gz 文件进行压缩。请注意，GBC 仅根据文件的扩展名判断文件类型，因此正确的文件扩展名才能够进行压缩；
- 为了便于统一全局的符号，GBC 当前仅支持对于人类基因组的压缩，CHROM当前只支持 1-22, X, Y 以及带有 chr 前缀的 1-22, X, Y；对于其他物种，需要先使用 `GBC index <inputFileName>` 构建种群的 contig 文件，压缩时传入参数 `--contig <contigFile>` 进行压缩；
- GBC 对多个文件进行合并压缩时，要求这些文件具有相同的样本序列（样本顺序可以不一致）。若一个文件的样本序列是其他文件的子序列，它也可以被正确压缩，缺失的样本基因型将被替换为 .|.。

#### 1.1. 命令行运行示例

```shell
# 压缩 assoc.hg19.vcf.gz 文件
GBC build ./example/assoc.hg19.vcf.gz

# 压缩 1000GP3 分群体文件夹
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

| 参数<img width=150/>     | 参数类型/用法                             | 描述                                                         |
| :----------------------- | ----------------------------------------- | ------------------------------------------------------------ |
| --output<br />-o         | -o outputFileName                         | 设置输出文件名 (默认为 inputFileName1.gtb)                   |
| --phased<br />-p         | -p                                        | 设置基因型数据为有向型                                       |
| --threads<br />-t        | -t [x], [x] 是 ≥ 1 的整数                 | 并行压缩的线程数 (默认: 4)                                   |
| --blockSizeType<br />-bs | -bs [x], [x] 是 [0, 7] 范围内的整数       | 设定每个压缩块的最大位点数，根据 $2^{7+x}$​ 换算得到真实的块大小值 (默认: 6) |
| --no-reordering<br />-nr | -nr                                       | 不使用 AMDO 算法对基因型阵列进行重排列                       |
| --windowSize<br />-ws    | -ws [x]，[x] 是 ≥ 2 的整数                | 设定 AMDO 算法采样窗口的大小，仅当 -r true 时该参数起作用 (默认: 24) |
| --compressor<br />-c     | -c [x], [x] 是 0~3 的整数或对应的压缩器名 | 设置压缩数据的算法 (0: ZSTD, 1: LZMA, 2: EMPTY)，预留的 2 和 3 可以由其他开发人员配置 |
| --level<br />-l          | -l [x], [x] 是 ≤ 31 的整数                | 压缩器参数，较大的参数将有助于提升压缩比，但也会带来时间上的额外开销 (ZSTD: 0~22, 默认值: 16; LZMA: 0~9, 默认值: 3) |
| --readyParas<br />-rp    | -rp gtbFileName                           | 使用外置 GTB 文件的参数作为模版参数 (覆盖默认的 -p, -bs, -c, -l) |
| --yes<br />-y            | -y 或 --yes                               | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

**质量控制参数：**

| 参数<img width=150/> | 参数类型/用法                                          | 描述                                                         |
| -------------------- | ------------------------------------------------------ | ------------------------------------------------------------ |
| --gty-gq             | --gty-gq [x], [x] 是 ≥ 0 的整数                        | 样本基因型GQ分数值小于指定值时，该基因型将被替换为“.\|.” (默认: 20) |
| --gty-dp             | --gty-dp [x], [x] 是 ≥ 0 的整数                        | 样本基因型DP分数值小于指定值时，该基因型将被替换为“.\|.” (默认: 4) |
| --seq-qual           | --seq-qual [x], [x] 是 ≥ 0 的整数                      | 位点QUAL分数值小于指定值时，该位点将被移除 (默认: 30)        |
| --seq-dp             | --seq-dp  [x], [x] 是 ≥ 0 的整数                       | 位点DP分数值小于指定值时，该位点将被移除 (默认: 0)           |
| --seq-mq             | --seq-mq [x], [x] 是 ≥ 0 的整数                        | 位点MQ分数值小于指定值时，该位点将被移除 (默认: 20)          |
| --seq-ac             | --seq-ac minAC-, --seq-ac -maxAC, --seq-ac minAC-maxAC | 位点等位基因计数小于minAC或不在[minAC, maxAC]内时，该位点将被移除 |
| --seq-af             | --seq-af minAF-, --seq-af -maxAF, --seq-ac minAF-maxAF | 位点等位基因频率小于minAF或不在[minAF, maxAF]内时，该位点将被移除 |
| --seq-an             | --seq-an minAN-, --seq-an -maxAN, --seq-an minAN-maxAN | 位点非缺失等位基因个数小于 minAN 时，该位点将被移除 (默认: 0) |
| --max-allele         | --max-allele [x], [x] 是 [2, 15] 范围内的整数          | 排除等位基因种类超过 [x] 的位点                              |
| --no-qc              | --no-qc                                                | 禁用所有的质量控制方法                                       |

#### 1.3. 参数建议

- 提取基因型数据后会损失基因型有关的质量信息，而低质量的测序数据是不可信的。因此 GBC 的质控流程默认是启动的 （如果存在质量信息的话）。若不需要进行质控，可以设置参数 --no-qc；
- AMDO 算法 (-r true) 在多数情况下都能显著提高压缩效果（提升压缩比和压缩速度）；
- GBC 在空压缩器下 (-c EMPTY) 将不会进行数据的压缩，该模式可以作为基线比较各个压缩器之间的性能差异；
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
IBuildTask task = new BuildTask("./example/assoc.hg19.vcf.gz").setPhased(true);
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
GBC rebuild <inputFileName> [options]
```

- 重构模式用于永久地修改 GTB 文件信息，这些操作会涉及到重新解压 GTB 文件、重新压缩的过程，因此该方法也能用于进行文件排序；
- 对于节点级别的操作 (移除某些节点、保留某些节点) 而不涉及其他修改文件头部信息，我们更建议使用 edit 模式完成。因为这些操作不需要解压文件这一过程，从而可以大幅地提升速度。

#### 2.1. 命令行运行示例

```shell
# 重构 assoc.hg19.gtb 文件，设置基因型为有向、块大小为 4096，6 线程
GBC rebuild ./example/assoc.hg19.gtb --phased --threads 6 --blockSizeType 5 --output ./example/assoc.hg19.unphased.gtb
```

#### 2.2. 参数一览

**压缩器参数：**

| 参数<img width=150/>     | 参数类型/用法                             | 描述                                                         |
| :----------------------- | ----------------------------------------- | ------------------------------------------------------------ |
| --output<br />-o         | -o outputFileName                         | 设置输出文件名 (默认与输入文件名相同，即覆盖原文件)          |
| --phased<br />-p         | -p                                        | 设置基因型数据为有向型                                       |
| --threads<br />-t        | -t [x], [x] 是 ≥ 1 的整数                 | 并行压缩的线程数 (默认: 4)                                   |
| --blockSizeType<br />-bs | -bs [x], [x] 是 [0, 7] 范围内的整数       | 设定每个压缩块的最大位点数，根据 $2^{7+x}$ 换算得到真实的块大小值 |
| --no-reordering<br />-nr | -nr                                       | 不使用 AMDO 算法对基因型阵列进行重排列                       |
| --windowSize<br />-ws    | -ws [x]，[x] 是 ≥ 2 的整数                | 设定 AMDO 算法采样窗口的大小，仅当 -r true 时该参数起作用 (默认: 24) |
| --compressor<br />-c     | -c [x], [x] 是 0~3 的整数或对应的压缩器名 | 设置压缩数据的算法 (0: ZSTD, 1: LZMA, 2: EMPTY)，预留的 2 和 3 可以由其他开发人员配置 |
| --level<br />-l          | -l [x], [x] 是 ≤ 31 的整数                | 压缩器参数，较大的参数将有助于提升压缩比，但也会带来时间上的额外开销 (ZSTD: 0~22, 默认值: 16; LZMA: 0~9, 默认值: 9) |
| --readyParas<br />-rp    | -rp gtbFileName                           | 使用外置 GTB 文件的参数作为模版参数 (覆盖默认的 -p, -bs, -l, -u) |
| --yes<br />-y            | -y 或 --yes                               | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

**样本选择：**

| 参数<img width=150/> | 参数类型/用法                                           | 描述                     |
| -------------------- | ------------------------------------------------------- | ------------------------ |
| --subject<br />-s    | --subject subject1,subject2,…<br />或 --subject @文件名 | 保留指定样本的基因型数据 |

**位点选择 (在一次指令运行时，--retain, --delete 至多只能指定一个)：**

| 参数<img width=150/> | 参数类型/用法                                                | 描述         |
| -------------------- | ------------------------------------------------------------ | ------------ |
| --retain             | 支持两种不同的命令格式：  --retain chrom=1,2,3  --retain chrom=1,2,3-node=1,2,3 | 保留指定节点 |
| --delete             | 支持两种不同的命令格式：  --delete chrom=1,2,3  --delete chrom=1,2,3-node=1,2,3 | 移除指定节点 |

**位点过滤参数：**

| 参数<img width=150/> | 参数类型/用法                                          | 描述                                                         |
| -------------------- | ------------------------------------------------------ | ------------------------------------------------------------ |
| --seq-ac             | --seq-ac minAC-, --seq-ac -maxAC, --seq-ac minAC-maxAC | 位点等位基因计数小于minAC或不在[minAC, maxAC]内时，该位点将被移除 |
| --seq-af             | --seq-af minAF-, --seq-af -maxAF, --seq-ac minAF-maxAF | 位点等位基因频率小于minAF或不在[minAF, maxAF]内时，该位点将被移除 |
| --seq-an             | --seq-an minAN-, --seq-an -maxAN, --seq-an minAN-maxAN | 位点非缺失等位基因个数小于 minAN 时，该位点将被移除 (默认: 0) |
| --max-allele         | --max-allele [x], [x] 是 [2, 15] 范围内的整数          | 排除等位基因种类超过 [x] 的位点                              |

#### 2.3. 在 Java 中使用 API 工具

```java
// 重构 assoc.hg19.gtb 文件，设置基因型为无向、块大小为 4096，6 线程
IBuildTask task = new RebuildTask("./example/assoc.hg19.gtb", "./example/assoc.hg19.unphased.gtb")
        .setPhased(true)
        .setParallel(6)
        .setBlockSizeType(5);
task.submit();
```

### 3. 合并多个 GTB 文件 —— merge 运行模式

使用如下指令合并多个 GTB 文件：

```shell
GBC merge <GTBFileName1, GTBFileName2, ...> [options]
```

- 这些文件必须具有不重叠的样本名。样本名重叠时，可以使用 `edit <GTBFileName> --reset-subject subject1,subject2,...` 或  `edit <GTBFileName> --reset-subject @file` 重设样本名；
- 为了确保内存可控，输入文件必须是有序的。文件无序时，可以使用 `rebuild <GTBFileName>` 进行排序。

#### 3.1. 命令行运行示例

```shell
# 合并 1000GP3 数据集
GBC merge ./example/1000GP3/AMR.gtb ./example/1000GP3/AFR.gtb \
./example/1000GP3/EAS.gtb ./example/1000GP3/EUR.gtb ./example/1000GP3/SAS.gtb \ 
--phased false --threads 6 --blockSizeType 5 \
--output ./example/1000GP3.gtb
```

#### 3.2. 参数一览

**压缩器参数：**

| 参数<img width=150/>     | 参数类型/用法                             | 描述                                                         |
| :----------------------- | ----------------------------------------- | ------------------------------------------------------------ |
| --output<br />-o         | -o outputFileName                         | 设置输出文件名 (默认与第一个文件名相同，即覆盖第一个文件)    |
| --phased<br />-p         | -p true 或 -p false                       | 设置基因型数据为无向/有向型 (默认: false)                    |
| --threads<br />-t        | -t [x], [x] 是 ≥ 1 的整数                 | 并行压缩的线程数 (默认: 4)                                   |
| --blockSizeType<br />-bs | -bs [x], [x] 是 [0, 7] 范围内的整数       | 设定每个压缩块的最大位点数，根据 $2^{7+x}$​ 换算得到真实的块大小值 (默认: 6) |
| --reordering<br />-r     | -r true 或 -r false                       | 使用 AMDO 算法对基因型阵列进行重排列 (默认: true)            |
| --windowSize<br />-ws    | -ws [x]，[x] 是 ≥ 2 的整数                | 设定 AMDO 算法采样窗口的大小，仅当 -r true 时该参数起作用 (默认: 24) |
| --compressor<br />-c     | -c [x], [x] 是 0~3 的整数或对应的压缩器名 | 设置压缩数据的算法 (0: ZSTD, 1: LZMA, 2: EMPTY)，预留的 2 和 3 可以由其他开发人员配置 |
| --level<br />-l          | -l [x], [x] 是 ≤ 31 的整数                | 压缩器参数，较大的参数将有助于提升压缩比，但也会带来时间上的额外开销 (ZSTD: 0~22, 默认值: 16; LZMA: 0~9, 默认值: 3) |
| --readyParas<br />-rp    | -rp gtbFileName                           | 使用外置 GTB 文件的参数作为模版参数 (覆盖默认的 -p, -bs, -c, -l) |
| --yes<br />-y            | -y 或 --yes                               | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

**位点过滤参数：**

| 参数<img width=150/> | 参数类型/用法                                                | 描述                                                         |
| -------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --seq-ac             | 支持两种不同的命令格式：--seq-ac minAC 和 --seq-ac minAC-maxAC | 位点等位基因计数小于minAC或不在[minAC, maxAC]内时，该位点将被移除 |
| --seq-af             | 支持两种不同的命令格式：--seq-af minAF 和 --seq-af minAF-maxAF | 位点等位基因频率小于minAF或不在[minAF, maxAF]内时，该位点将被移除 |
| --seq-an             | --seq-an minAN                                               | 位点非缺失等位基因个数小于 minAN 时，该位点将被移除 (默认: 0) |
| --max-allele         | --max-allele [x], [x] 是 [2, 15] 范围内的整数                | 排除等位基因种类超过 [x] 的位点                              |

#### 3.3. 在 Java 中使用 API 工具

```java
// 合并 1000GP3 文件，设置基因型为无向、块大小为 8192，10 线程
IBuildTask task = new MergeTask(new String[]{"./example/1000GP3/AMR.gtb",
        "./example/1000GP3/EAS.gtb", "./example/1000GP3/EUR.gtb", 
        "./example/1000GP3/SAS.gtb", "./example/1000GP3/AFR.gtb", },
        "./example/1000GP3.gtb")
        .setPhased(false)
        .setParallel(10)
        .setBlockSizeType(6);
task.submit();
```

### 4 查看 GTB 文件的基本信息 —— show 运行模式

使用如下指令查看 GTB 文件：

```shell
GBC show <inputFileName1, inputFileName2, ...> [options]
```

#### 4.1. 命令行运行示例

```shell
# 查看压缩的 1000GP3.gtb 文件信息
GBC show ./example/1000GP3.phased.gtb

# 查看压缩的 assoc.hg19.gtb 所有信息
GBC show ./example/assoc.hg19.gtb --full

# 查看 1000GP3.gtb 文件的所有样本名
GBC show ./example/1000GP3.phased.gtb --list-subject-only > ./example/1000GP3.phased.subjects.txt
```

#### 4.2. 参数一览

| 参数<img width=150/> | 参数类型/用法                 | 描述                                                         |
| -------------------- | ----------------------------- | ------------------------------------------------------------ |
| --list-md5           | --list-md5                    | 计算文件的 MD5 码，大文件计算将会花费更多时间                |
| --list-baseInfo      | --list-baseInfo               | 列出文件的基本信息 (GTB 文件的前 2 字节)                     |
| --list-subject       | --list-subject                | 列出所有的样本名                                             |
| --list-subject-only  | --list-subject-only           | 仅列出所有的文件名                                           |
| --list-tree          | --list-tree                   | 列出 GTB 节点树（染色体级别）                                |
| --list-node          | --list-node                   | 列出 GTB 节点树（所有的节点）                                |
| --list-chromosome    | --list-chromosome 1,2,...,X,Y | 列出指定染色体的节点树 (搭配 --list-node，可以列出指定染色体下所有的节点信息) |
| --full, -f           | --full 或 -f                  | 列出所有信息 (除了 md5 码)                                   |

#### 4.3. 在 Java 中使用 API 工具

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

### 5. 从压缩的 GTB 文件中提取数据 —— extract 运行模式

使用如下指令对 GTB 文件进行数据提取：

```shell
GBC extract <inputFileName> [options]
```

#### 5.1. 命令行运行示例

```shell
# 解压 assoc.hg19.gtb 的所有位点，并将输出文件压缩为 bgzf 格式
GBC extract ./example/assoc.hg19.gtb --output ./example/assoc.hg19.simple.vcf.gz

# 从 1000GP3 中提取 1 号染色体 10177-1000000 的数据
GBC extract ./example/1000GP3.phased.gtb --range 1:10177-1000000

# 从 1000GP3 中提取 1,2,3,4,5 号染色体数据， 并按照染色体编号分别保存为 bgz 格式
GBC extract ./example/1000GP3.phased.gtb --retain chrom=1,2,3,4,5 --o-bgz --split -o ./example/extract

# 根据 随机位置文件 ./example/query_1000GP3.txt 获取指定位点数据
GBC extract ./example/1000GP3.phased.gtb --ramdom ./example/query_1000GP3.txt

# 在 assoc.hg19 中提取等位基因计数 1~20 的位点
GBC extract ./example/assoc.hg19.gtb --filterByAC 1-20
```

#### 5.2. 参数一览

**输出参数：**

| 参数<img width=150/> | 参数类型/用法                  | 描述                                                         |
| -------------------- | ------------------------------ | ------------------------------------------------------------ |
| --output<br />-o     | 文件路径                       | 输出文件名 (默认为 inputFileName.vcf, 在 --o-bgz 参数下则默认为: inputFileName.vcf.gz) |
| --o-bgz              | --o-bgz                        | 使用 bgzip 压缩输出文件 (当指定了 -o 参数并且该文件名以 .gz 结尾时，该命令会被自动执行) |
| --level<br />-l      | -l [x], [x] 是 [0, 9] 内的整数 | 设置 bgzip 压缩的级别 (默认: 5)                              |
| --threads<br />-t    | -t [x], [x] 是 ≥ 1 的整数      | 并行解压线程数量 (默认: 4)，用于提高解压速度。               |
| --phased<br />-p     | -p true 或 -p false            | 强制转换基因型数据向型，默认情况下基因型向型与压缩时指定的phased一致 |
| --split              | --split true 或 --split false  | 按照染色体编号分别组织数据                                   |
| --hideGT<br />-hg    | -hg true 或 -hg false          | 不输出基因型数据                                             |
| --yes<br />-y        | -y 或 --yes                    | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

**样本选择：**

| 参数<img width=150/> | 参数类型/用法                                                | 描述                     |
| -------------------- | ------------------------------------------------------------ | ------------------------ |
| --subject<br />-s    | 不同样本名使用逗号分隔：  --subject subject1,subject2,…<br />或使用 --subject @文件名 | 提取指定样本的基因型数据 |

**位点选择 (默认解压所有位点)：**

| 参数<img width=150/> | 参数类型/用法                                                | 描述                               |
| -------------------- | ------------------------------------------------------------ | ---------------------------------- |
| --random             | 文件路径。文件中一行代表一个位点，每一行的格式为：chrom,pos 或 chrom<\t>pos | 根据保存随机查询位点的文件进行访问 |
| --range              | 支持三种不同的命令格式：  --range chrom 或  --range chrom:start 或 --range chrom:start-end | 按照位点范围进行提取               |
| --node               | 支持两种不同的命令格式：  --node chrom=1,2,3 或 --node chrom=1,2,3-node=1,2,3 | 按照GTB节点进行提取                |

**位点过滤参数：**

| 参数<img width=150/>   | 参数类型/用法                                                | 描述                                                   |
| ---------------------- | ------------------------------------------------------------ | ------------------------------------------------------ |
| --filterByAC<br />-fAC | 支持两种不同的命令格式：--filterByAC minAC 或 --filterByAC minAC-maxAC | 提取变异位点等位基因数在指定范围内的位点基因型数据。   |
| --filterByAF<br />-fAF | 支持两种不同的命令格式：  --filterByAF minAF 或 --filterByAF minAF-maxAF | 提取变异位点等位基因频率在指定范围内的位点基因型数据。 |
| --filterByAN<br />-fAN | --filterByAN minAN                                           | 提取变异位点有效等位基因数大于指定值的位点基因型数据。 |

#### 5.3. 在 Java 中使用 API 工具

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
task.decompress(1, 10177, 1000000);

// 从 1000GP3 中提取 3 号染色体数据
task = new ExtractTask("./example/1000GP3.phased.gtb");
task.decompress(3);

// 根据 随机位置文件 ./example/query_1000GP3.txt 获取指定位点数据
task = new ExtractTask("./example/1000GP3.phased.gtb");
task.decompressByPositionFromFile("query_1000GP3.txt", "\n", ",");

// 在 assoc.hg19 中提取等位基因计数 1~20 的位点
task = new ExtractTask("./example/1000GP3.phased.gtb").filterByAC(1, 20);
task.decompressAll();
```

### 6. 编辑 GTB 文件 —— edit 运行模式

使用如下指令对 GTB 文件进行编辑：

```shell
GBC edit <inputFileName> [options]
```

编辑模式可以在不解压基因组文件下进行节点的操作，可以快速进行文件切片、剔除、合并操作。编辑模式建议在图形界面中使用，更为直观。

#### 6.1. 命令行运行示例

```shell
# 分别提取出节点1和节点2
GBC edit ./example/assoc.hg19.gtb --output ./example/node1.gtb --retain chrom=1-node=1
GBC edit ./example/assoc.hg19.gtb --output ./example/node2.gtb --retain chrom=1-node=2

# 合并为 node1_2_2.gtb
GBC edit ./example/node1.gtb --output ./example/node1_2_2.gtb --concat ./example/node2.gtb

# 直接提取节点1和节点2
GBC edit ./example/assoc.hg19.gtb --output ./example/node1_2_1.gtb --retain chrom=1-node=1,2

# 使用md5校验两种方式生成的文件是否包含相同数据
GBC edit ./example/node1_2_1.gtb ./example/node1_2_2.gtb

# 将整体的 1000GP3 文件重新拆分为按染色体编号组织的数据
GBC edit ./example/1000GP3.phased.gtb --split --output ./example/1000GP3.phased
```

#### 6.2. 参数一览

| **参数**<img width=150/> | **参数类型**                                                 | **描述**                                                     |
| ------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --output<br />-o         | -o outputFileName                                            | 输出文件名，未传入该参数时，编辑后的文件**将覆盖原文件**。   |
| --retain                 | 支持两种不同的命令格式：  --retain chrom=1,2,3  --retain chrom=1,2,3-node=1,2,3 | 保留指定节点                                                 |
| --delete                 | 支持两种不同的命令格式：  --delete chrom=1,2,3  --delete chrom=1,2,3-node=1,2,3 | 移除指定节点                                                 |
| --concat                 | --merge gtbFileName1 gtbFileName1 …                          | 合并GTB文件，这些 GTB 文件需要有相同的样本名序列、相同的向型、相同的组合编码标记 |
| --unique                 | --unique                                                     | 节点去重                                                     |
| --reset-subject          | --reset-subject subject1,subject2,... 或 --reset-subject @file | 重设样本名。输入的样本名长度必须与原文件相同                 |
| --split, -s              | --split true 或 --split false                                | 按照染色体编号拆分文件                                       |
| --yes<br />-y            | -y 或 --yes                                                  | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

在一次指令运行时，--retain, --delete, --merge, --unique 至多只能指定一个 (GUI 编辑模式、API 调用则无此限制)。

#### 6.3. 在 Java 中使用 API 工具

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

### 7. 计算连锁不平衡系数 —— ld 运行模式

使用如下指令对 **有序 GTB 文件**计算连锁不平衡系数：

```shell
GBC ld <inputFileName> [options]
```

#### 7.1. 命令行运行示例

```shell
# 计算 1000 GP3 的连锁不平衡系数
GBC ld ./example/1000GP3.phased.gtb --model --geno --window-bp 10000 --maf 0.1 --min-r2 0.2
GBC ld ./example/1000GP3.phased.gtb --model --hap
```

#### 7.2. 参数一览

**输出参数:** 

| **参数**<img width=150/> | **参数类型**                   | **描述**                                                     |
| ------------------------ | ------------------------------ | ------------------------------------------------------------ |
| --output<br />-o         | -o outputFileName              | 设置输出文件名，                                             |
| --o-bgz                  | --o-bgz                        | 使用 bgzip 压缩输出文件 (当指定了 -o 参数并且该文件名以 .gz 结尾时，该命令会被自动执行) |
| --level<br />-l          | -l [x], [x] 是 [0, 9] 内的整数 | 设置 bgzip 压缩的级别 (默认: 5)                              |
| --threads<br />-t        | -t [x], [x] 是 ≥ 1 的整数      | 并行计算线程数量 (默认: 4)，用于提高计算速度                 |
| --yes<br />-y            | -y 或 --yes                    | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

**LD 计算参数:** 

| **参数**<img width=150/> | **参数类型**                                                 | **描述**                                                     |
| ------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --model<br />-m          | Haplotype LD: hap, --hap, --hap--r2, --hap-ld<br />Genotype LD: geno, --geno, --geno--r2, --geno-ld<br /> | 设置用于计算的 LD 模型，当前适配 Haplotype LD 计算和 Genotype LD 计算 (默认: phased 的 GTB 文件将计算 Haplotype LD，unphased 的 GTB 文件将计算 Genotype LD) |
| --window-bp<br />-bp     | -bp [x], [x] 是 ≥ 1 的整数                                   | 设置计算成对位点的最大物理距离 (默认: 10000)                 |
| --min-r2                 | --min-r2 [x], [x] 是 [0, 1] 范围内的浮点数                   | 排除相关系数小于 --min-r2 的位点对 (默认: 0.2)               |
| --maf                    | --maf [x], [x] 是 [0, 1] 范围内的浮点数                      | 排除次级等位基因频率 (MAF) 低于指定 --maf 的位点 (默认: 0.05) |

**样本选择与位点选择:** 

| **参数**<img width=150/> | **参数类型**                                                 | **描述**                       |
| ------------------------ | ------------------------------------------------------------ | ------------------------------ |
| --range                  | 支持三种不同的命令格式：  --range chrom 或  --range chrom:start 或 --range chrom:start-end | 计算指定范围内位点间的 LD 系数 |
| --subject<br />-s        | --subject subject1,subject2,…<br />或 --subject @文件名      | 计算指定样本的 LD 系数         |

#### 7.3. 在 Java 中使用 API 工具

```java
LDTask task = new LDTask("./example/1000GP3.phased.gtb");
task.setWindowSizeBp(10000)
        .setMaf(0.1)
        .setMinR2(0.2)
        .setLdModel(ILDModel.GENOTYPE_LD);
task.submit();
```

### 8. 辅助工具

#### 8.1. Index 构建 contig 文件

```shell
GBC index inputFileName
```

在执行其他指令时，通过传入 --contig contigFIle 指定对应的 contig 文件，否则将默认以人类基因组 contig 进行压缩。

#### 8.2. 并行 bgzip 压缩工具

GBC 集成了纯 Java 版本的并行 bgzip 压缩工具，使用如下指令运行 bgzip 工具：

```shell
GBC bgzip inputFileName [options]
```

| **参数**<img width=150/> | **参数类型**                                             | **描述**                                                     |
| ------------------------ | -------------------------------------------------------- | ------------------------------------------------------------ |
| --output<br />-o         | 文件路径，未传入该参数时自动生产                         | 输出文件名                                                   |
| --decompress<br />-d     | -d 或 --decompress                                       | 解压数据                                                     |
| --cut<br />-c            | 支持两种不同的命令格式：  --cut start 或 --cut start-end | 切割 bgzf 文件包含 [start, end] 在内的完整的 gzblocks        |
| --threads<br />-t        | -t [x], [x] 是 ≥ 1 的整数                                | 并行解压线程数量 (默认: 4)，用于提高解压速度                 |
| --level<br />-l          | -l [x], [x] 是 [0, 9] 内的整数                           | 使用 bgzip 压缩时的压缩级别                                  |
| --yes, -y                | -y 或 --yes                                              | 当输出文件已存在时，软件会询问是否覆盖。添加该参数则不询问，直接覆盖输出文件 |

#### 8.3. MD5 码校验工具

使用如下指令校验文件的 MD5 码：

```shell
GBC md5 <inputFileName1, inputFileName2, ...> [--o-md5]
```

其中，可选参数 `--o-md5`表示在输入文件的位置导出相应的 md5 文件。

## IV. 命令行交互模式

命令行交互模式的输入参数与普通命令行模式一致，但此时多次的运行都是在同一次程序生命周期中进行，可以减少 JVM 启动、热点代码 JIT 的时间开销。

在输入程序时，不再需要指定 `java -Xmx4g -Xms4g -jar gbc.jar` 除了兼容普通命令行参数外，还有以下四种额外的参数：

| 参数            | 描述                              |
| --------------- | --------------------------------- |
| exit, q, quit   | 退出程序                          |
| clear           | 清空屏幕 (实际上打印出多行空白行) |
| reset           | 清空缓冲区                        |
| 以“#”开头的指令 | 注释，不执行任何操作              |

![interactive-mode](images/interactive-mode.png)

## V. API 文档

- commandParser: http://pmglab.top/commandParser/
- unifyIO:
- 

 
