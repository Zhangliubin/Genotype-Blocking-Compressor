## II. 安装和配置运行环境

  ### 1. 下载 GBC 及相关文件

  | Type                        | File                                                         |
  | --------------------------- | ------------------------------------------------------------ |
  | Source codes                | [GBC-1.1](https://github.com/Zhangliubin/Genotype-Blocking-Compressor) |
  | Example                     | [Instance](http://pmglab.top/gbc/download/example.zip)       |
  | Docker Images (Docker File) | [Dockerfile](http://pmglab.top/gbc/download/dockerfile)      |
  | Package/Software            | [gbc-1.1.jar](./download/gbc-1.1.jar)                        |

  ### 2. 系统要求

  GBC 是基于 Oracle JDK 8 开发的程序，任何支持或兼容 Oracle JDK 8 的计算机设备都可以使用。用户需要先下载安装 [Oracle JDK](https://www.oracle.com/cn/java/technologies/javase-downloads.html) 或 [Open JDK](https://openjdk.java.net/install/)。Apple Silicon 设备可以使用 [zulu JDK](https://www.azul.com/downloads/?package=jdk#download-openjdk) 作为替代。

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

  - 命令行模式：`java -Xms4g -Xmx4g -jar gbc.jar <mode> [options]`，有关 <mode> 及对应参数 [options] 的设置详见后文

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

  - API 工具：通过导入本 gbc.jar 包使用
