## I. Introduction

GBC (short for GenoType Blocking Compressor) is a blocking compressor for genotype data, which aims at creating an unified and flexible structure-GenoType Block (GTB) for genotype data in the variant call format (VCF) files. There will be a less occupation of hard disk space, a faster data access and extraction function, a more convenient management of population files and a more efficient precess of data analysis with the GTB structure compared with the conventional gz format. GBC not only contains the function of compression, decompression, data query, but also includes basic quality control, LD calculation and file management of population files with block compression structure. 

> For any technical questions, please contact Liubin Zhang (suranyi.sysu@gmail.com).

## II. Environment configuration and software installation

### 1. Download GBC

| Type                        | File                                  | Size     |
| --------------------------- | ------------------------------------- | -------- |
| Package                     | [gbc.jar](./download/gbc.jar)         | 7.53 MB  |
| Source codes                | [gbc.zip](./download/source-code.zip) | 7.25 MB  |
| Example                     | [Instance](./download/example.zip)    | 16.49 MB |
| Docker Images (Docker File) | [Dockerfile](./download/dockerfile)   | 282 B    |
| latest gbc (version 1.1)    | [gbc-1.1.jar](./download/gbc-1.1.jar) | 7.57 MB  |

### 2. System requirements

- ZSTD is a lossless data compression algorithm based on C language developed by Yann Colle of Facebook, which is used by accessing to Java in the form of Java Native Interface ([zstd-jni](https://github.com/luben/zstd-jni)). Luben, the vindicator of zstd-jni, has released a [compile-free version of java package for ZSTD](https://repo1.maven.org/maven2/com/github/luben/zstd-jni/). [zstd-jni-1.4.9-5.jar](https://repo1.maven.org/maven2/com/github/luben/zstd-jni/1.4.9-5/zstd-jni-1.4.9-5.jar) has already been integrated into the GBC, which is compile-free on most of the computer devices except for some limited devices (such as Mac M1), with which users are required to compile by themselves.

- GBC is developed based on Oracle JDK 8. It is available on any computer device that supports or is compatible with Oracle JDK 8. Users are required to download and install the [Oracle JDK](https://www.oracle.com/cn/java/technologies/javase-downloads.html) or [Open JDK](https://openjdk.java.net/install/) firstly.

### 3. Operation requirements

- The GBC requires more memory to deal with large genome files, and we recommend allocating no less than 4GB of heap memory to run the GBC programs:

  ```shell
  java -Xms4g -Xmx4g -jar gbc.jar
  ```

- Under the Linux or MacOs, users may use the GBC with more streamlined instructions by setting the Shell alias directive:

  ```shell
  # Linux: the path of the GBC should be an absolute path
  echo "alias GBC='java -Xmx4g -Xmx4g -jar ~/bin/gbc.jar'" >> /.bashrc
  source ~/.bashrc
  
  # MacOs: the path of the GBC should be an absolute path
  echo "alias GBC='java -Xmx4g -Xmx4g -jar ~/bin/gbc.jar'" >> /.zshrc
  source ~/.zshrc
  ```

After configuration, users can use the GBC at the terminal by `GBC <mode> [options]`.

### 4. Instance

There are three main datasets used as examples:

- [assoc.hg19.vcf.gz](http://pmglab.top/kggseq/download.htm) , 10.42 MB, 983 samples, 78895 variants.
- [1000G Phase3 v5 Shapeit2 Reference (hg19)](http://pmglab.top/genotypes/), 27.45 GB, 2504 samples, 84801880 variants.
- batch.all, 181.790 GB, 992 samples, 38517169 variants. (Note: this dataset includes more information about the sequencing quality, and it was used for showing the quality control function of the GBC).

### 5. Start the GBC with four modes

GBC has four interface modes (use-pattern): command line mode, command line interactive mode, graphical interface mode and API tools. Starting ways of four different modes are as following:

- Command line mode: `java -Xms4g -Xmx4g -jar gbc.jar <mode> [options]`. The settings for \<mode\> and the corresponding parameter [options] are described below.

  ```shell
  java -Xms4g -Xmx4g -jar gbc.jar --build ./example/assoc.hg19.vcf.gz
  ```

- Command line interactive mode:

  - Use `java -Xms4g -Xmx4g -jar gbc.jar -i` in the command line mode to start the command line interactive invocation mode;
  - Add the parameter `-i` after the command in the command line mode, the program will enter interactive mode once after finishing the command:

  ```shell
  java -Xms4g -Xmx4g -jar gbc.jar --build ./example/assoc.hg19.vcf.gz -i
  ```

- Graphical interface mode:

  - After configuring the JDK environment variables, double-click to enter the GUI operating system (the running memory size cannot be specified in this way).
  - Start the graphical interface mode by command line: `java -Xms4g -Xmx4g -jar gbc.jar`


![setup-en](images/setup-en.jpg)

- API tools: Use by importing the gbc.jar package.

## III. Handle the genotype data by GBC

The four interface modes of the GBC provide the following operational modes, and at most one operation mode is specified at each run time, as shown following:

| Operational Mode | Command                                                 | Illustration                                                 |
| ---------------- | ------------------------------------------------------- | ------------------------------------------------------------ |
| build            | build \<inputFileName1, inputFileName2, ...\> [options] | Build *.gtb for vcf/vcf.gz files.                            |
| rebuild          | rebuild \<GTBFileName\> [options]                       | Rebuild *.gtb with new parameters.                           |
| merge            | merge <GTBFileName1, GTBFileName2, ...> [options]       | Merge multiple GTB files (with non-overlapping subject sets) into a single GTB file. |
| show             | show <inputFileName1, inputFileName2, ...> [options]    | Display summary of the GTB File.                             |
| extract          | extract \<inputFileName\> [options]                     | Variant filtration and decompress the corresponding information from *.gtb files. |
| edit             | edit \<inputFileName\> [options]                        | Edit the GTB file directly.                                  |
| ld               | ld \<inputFileName\> [options]                          | Calculate pairwise the linkage disequilibrium or genotypic correlation. |
| index            | index \<inputFileName\> [options]                       | Index contig file for specified VCF file.                    |
| bgzip            | bgzip \<inputFileName\> [options]                       | Use parallel bgzip to compress a single file.                |
| md5              | md5 <inputFileName1, inputFileName2, ...> [options]     | Calculate a message-digest fingerprint (checksum) for file.  |

### 1. Build the compressed GTB file - operational mode for "build"

Compress the genomic VCF file with the following command:

```shell
GBC build <inputFileName1, inputFileName2, ...> [options]
```

- GBC help compress the file in compliance with [VCF  Specification](https://samtools.github.io/hts-specs/VCFv4.2.pdf), and all GBC operations are based on the assumption that the file format is compliant with this specification;
- The inputFileName can be a single .vcf file or .vcf.gz file, and it can also be the path of the folder containing all the files to be compressed. When a folder path is given, the GBC will help filter out all .vcf or .vcf.gz files in this folder (and its sub-folders) for compression.
- The GBC currently only supports compression of the human genome, for chromosome, GBC only supports 1-22, X, Y and 1-22, X, Y with the "chr" prefix (such as chr1, chrY); For other species, please use command  `GBC index <inputFileName>`  to build the contig file first, and users should add the command `--contig <contigFile>` when compressing.
- When using GBC to combine and compress multiple files,  the files are required to have the same samples (can be disordered). If the sample of a file is a subset of other files, it can also be compressed correctly, and the missing genotypes will be replaced by .|.;

#### 1.1. Run the examples with the command line

```shell
# Compress the assoc.hg19.vcf.gz file
GBC build ./example/assoc.hg19.vcf.gz

# Compress the files of different populations in 1000GP3
GBC build ./example/1000GP3/AFR
GBC build ./example/1000GP3/AMR
GBC build ./example/1000GP3/EAS
GBC build ./example/1000GP3/EUR
GBC build ./example/1000GP3/SAS

# Compress the batchAll file, and set a criterion for quality control (gq >= 5, dp >= 30)
GBC build ./example/batchAll.vcf.gz --gty-gq 30 --gty-dp 5
```

#### 1.2. List of parameters

**Parameters for compressor**

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| :------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --output<br />-o           | -o outputFileName                                            | Set the output file.                                         |
| --phased<br />-p           | -p true or -p false                                          | Set the status of the genotype: phased (true) or unphased (false) (default: false). |
| --threads<br />-t          | -t [x], [x] is an integer ≥ 1                                | Set the number of threads (default: 4).                      |
| --blockSizeType<br />-bs   | -bs [x], [x] is an integer in the range of [0, 7]            | Set the maximum size of each block (the true block size is $2^{7+x}$​​ ) (default: 6). |
| --reordering<br />-r       | -r true or -r false                                          | Use the AMDO algorithm to reorder the genotype arrays (default: true). |
| --windowSize<br />-ws      | -ws [x], [x] is an integer ≥ 2                               | Set the window size of the AMDO algorithm (Execute only if --reordering is true) (default: 24). |
| --compressor<br />-c       | -c [x], [x] is an integer in the range of [0, 3] or the corresponding name of compressor | Set the algorithm for compression (0: ZSTD, 1: LZMA, 2: EMPTY) , and the reserved 2 and 3 can be configured by other developers. |
| --level<br />-l            | -l [x], [x] is an integer ≤ 31 (default: -1, indicates the default value for each compressor) | Parameters for compression algorithm the larger number, the higher compression ratio with slower speed (ZSTD: 0~22, default: 16; LZMA: 0~9, default: 3). |
| --readyParas<br />-rp      | -rp gtbFileName                                              | Use the template parameters (-p, -bs, -c, -l) from the external file. |
| --yes<br />-y              | -y or --yes                                                  | The output file will be directly overwritten without asking if this parameter is specified. |

**Other parameters for Quality Control**

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --gty-gq                   | --gty-gq [x], [x] is an integer ≥ 0                          | Exclude genotypes with the minimal genotyping quality (Phred Quality Score) per genotype < gq. (default: 20) |
| --gty-dp                   | --gty-dp [x], [x] is an integer ≥ 0                          | Exclude genotypes with the minimal read depth per genotype < dp. (default: 4) |
| --seq-qual                 | --seq-qual [x], [x] is an integer ≥ 0                        | Exclude variants with the minimal overall sequencing quality score (Phred Quality Score) per variant < qual. (default: 30) |
| --seq-dp                   | --seq-dp [x], [x] is an integer ≥ 0                          | Exclude variants with the minimal overall sequencing read depth per variant < dp. (default: 0) |
| --seq-mq                   | --seq-mq [x], [x] is an integer ≥ 0                          | Exclude variants with the minimal overall mapping quality score (Mapping Quality Score) per variant < mq. (default: 20) |
| --seq-ac                   | two forms are allowed: --seq-ac minAC and --seq-ac minAC-maxAC | Exclude variants with the minimal alternate allele count per variant < minAc or out of the range [minAc, maxAc]. |
| --seq-af                   | two forms are allowed: --seq-af minAF and --seq-af minAF-maxAF | Exclude variants with the minimal alternate allele frequency per variant < minAf or out of the range  [minAf, maxAf]. |
| --seq-an                   | --seq-an minAN                                               | Exclude variants with the minimum non-missing allele number per variant < AN. (default: 0) |
| --max-allele               | --max-allele [x], [x] an integer in the range of [2, 15]     | Exclude variants with alleles over --max-allele. (2-15, default: 15) |
| --no-qc                    | --no-qc                                                      | Disable all quality control methods.                         |

#### 1.3. Recommendation for parameters

- The genotypic quality information will be discarded after the extraction of genotype data,  and the quality control process is started by default (if quality information exist) because the low-quality sequencing data is unreliable. If the quality control is not required, please set the parameters as "--no-qc"；

- AMDO algorithm helps significantly improve compression (increase the compression ratio and compression speed) in most cases (-r true);

- GBC will not compress data by the empty compressor (-c EMPTY), which can be used as a baseline to compare the performance between different compressors;

- In parallel mode, the compression speed of the GBC depends on the IO speed of the disk, therefore, when running the GBC on a host with slow disk speeds, parallelism may not accelerate the compression;

- The maximum length of a single array is 2GB in Java, based on this, the corresponding relationship between sample size and block size is as follows:

  | Parameter | Variant number in a block | Sample size   | Parameter | Variant number in a block | Sample size    |
  | --------- | ------------------------- | ------------- | --------- | ------------------------- | -------------- |
  | -bs 7     | 16384                     | $\le 131071$  | -bs 3     | 1024                      | $\le 2097151$  |
  | -bs 6     | 8192                      | $\le 262143$  | -bs 2     | 512                       | $\le 4194303$  |
  | -bs 5     | 4096                      | $\le 524287$  | -bs 1     | 256                       | $\le 8388607$  |
  | -bs 4     | 2048                      | $\le 1048575$ | -bs 0     | 128                       | $\le 16777215$ |

#### 1.4. Use API in Java

```java
// Compress the assoc.hg19.vcf.gz file, set as phased and conduct quality control
IBuildTask task = new BuildTask("./example/assoc.hg19.vcf.gz").setPhased(true);
task.addAlleleFilter(new IAlleleFilter() {
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

// Compress the files of different populations in 1000GP3
for (String population: new String[]{"AFR", "AMR", "EAS", "EUR", "SAS"}){
    task = new BuildTask("./example/1000GP3/" + population);
    task.run();
}

// Compress the batchAll file, and set a criterion for quality control (gq >= 5, dp >= 30)
task = new BuildTask("./example/batch.all", "./example/batch.all.filterGQ5_DP30.gtb")
        .setGenotypeQualityControlDp(30)
        .setGenotypeQualityControlGq(5);
task.submit();
```

### 2. Refactor the GTB file - operational mode for "rebuild"

Refactor the GTB file with the following command:

```shell
GBC rebuild <inputFileName> [options]
```

- The "rebuild" operational mode will modify the GTB file permanently. These operations involve the process of re-decompressing and re-compressing the GTB file. Thus, this mode allows conducting the file sorting.
- For the operations that without any modification of the header information (such as delete or retain some specified nodes), we recommend using the operational mode "edit" because these operations do not require decompression, which can greatly improve the speed.

#### 2.1. Run the examples with the command line

```shell
# Rebuild the assoc.hg19.gtb file with 6 threads, and set the status of the genotype as phased, with a block size 4096
GBC rebuild ./example/assoc.hg19.gtb --phased true --threads 6 --blockSizeType 5 --output ./example/assoc.hg19.unphased.gtb
```

#### 2.2. List of parameters

**Parameters for compressor**

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --output<br />-o           | -o outputFileName                                            | Set the output file.                                         |
| --phased<br />-p           | -p true or -p false                                          | Set the status of the genotype: phased (true) or unphased (false) (default: true). |
| --threads<br />-t          | -t [x], [x] is an integer ≥ 1                                | Set the number of threads (default: 4).                      |
| --blockSizeType<br />-bs   | -bs [x], [x] is an integer in the range of [0, 7]            | Set the maximum size of each block (the true block size is  $2^{7+x}$​ ) (default: 4). |
| --reordering<br />-r       | -r true or -r false                                          | Use the AMDO algorithm to reorder the genotype arrays (default: true). |
| --windowSize<br />-ws      | -ws [x], [x] is an integer ≥ 2                               | Set the window size of the AMDO algorithm (Execute only if --reordering is true) (default: 24). |
| --compressor<br />-c       | -c [x], [x] is an integer in the range of [0, 3] or the corresponding name of compressor | Set the algorithm for compression (0: ZSTD, 1: LZMA, 2: EMPTY) , and the reserved 2 and 3 can be configured by other developers. |
| --level<br />-l            | -l [x], [x] is an integer ≤ 31 (default: -1, indicates the default value for each compressor) | Parameters for compression algorithm the larger number, the higher compression ratio with slower speed (ZSTD: 0~22, default: 16; LZMA: 0~9, default: 3). |
| --readyParas<br />-rp      | -rp gtbFileName                                              | Use the template parameters (-p, -l, -bs, -u) from the external file. |
| --yes<br />-y              | -y or --yes                                                  | Overwrite files without asking.                              |

**Select subjects**

| Parameters<img width=150/> | Parameter type/usage                                | Description                                                  |
| -------------------------- | --------------------------------------------------- | ------------------------------------------------------------ |
| --subject<br />-s          | --subject subject1,subject2,…or --subject @filename | Rebuild the *gtb for the specified subjects (--subject subject1,subject2,...). These subjects can be stored in a file with ',' delimited form, and passed in via "--subject @file". |

**Select variants**

Parameters "--retain" and "--delete" can not be used together under the command line mode (only one at a time), while there is no such restriction under the graphical interface mode (GUI editing mode) or with API tools.

| Parameters<img width=150/> | Parameter type/usage                                         | Description                    |
| -------------------------- | ------------------------------------------------------------ | ------------------------------ |
| --retain                   | two forms are allowed:  --retain chrom=1,2,3  --retain chrom=1,2,3-node=1,2,3 | Retain the specified GTBNodes. |
| --delete                   | two forms are allowed:  --delete chrom=1,2,3  --delete chrom=1,2,3-node=1,2,3 | Delete the specified GTBNodes. |

**Variants quality control**

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --seq-ac                   | two forms are allowed: --seq-ac minAC or --seq-ac minAC-maxAC | Exclude variants with the minimal alternate allele count per variant < minAc (--seq-ac minAc) or out of the range [minAc, maxAc] (--seq-ac minAc-maxAc). |
| --seq-af                   | two forms are allowed: --seq-af minAF or --seq-af minAF-maxAF | Exclude variants with the minimal alternate allele frequency per variant < minAf (--seq-af minAf) or out of the range [minAf, maxAf] (--seq-af minAf-maxAf). |
| --seq-an                   | --seq-an minAN                                               | Exclude variants with the minimum non-missing allele number per variant < AN. (default: 0) |
| --max-allele               | --max-allele [x], [x] is an integer in the range of [2, 15]  | Exclude variants with alleles over --max-allele. (2-15, default: 15) |

#### 2.3. Use API in Java

```java
// Rebuild the assoc.hg19.gtb file with 6 threads, and set the status of the genotype as unphased, with a block size 4096
IBuildTask task = new RebuildTask("./example/assoc.hg19.gtb", "./example/assoc.hg19.unphased.gtb")
        .setPhased(true)
        .setParallel(6)
        .setBlockSizeType(5);
task.submit();
```

### 3. Merge multiple GTB file -  operational mode for "merge"

Merge multiple GTB file with the following command:

```shell
GBC merge <GTBFileName1, GTBFileName2, ...> [options]
```

- These files must have non-overlapping sample names. When the sample names overlap, users can use `edit <GTBFileName> --reset-subject subject1,subject2,...` or  `edit <GTBFileName> --reset-subject @file` to reset the sample names;
- The variants of the input files must be in order. If the variants are out of order, users can use `rebuild <GTBFileName>` to sort the file.

#### 3.1. Run the examples with the command line

```shell
# Combine the population vcf files in 1000GP3
GBC merge ./example/1000GP3/AMR.gtb ./example/1000GP3/AFR.gtb \
./example/1000GP3/EAS.gtb ./example/1000GP3/EUR.gtb ./example/1000GP3/SAS.gtb \ 
--phased false --threads 6 --blockSizeType 5 \
--output ./example/1000GP3.gtb
```

#### 3.2. List of parameters 

**Parameters for compressor**

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| :------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --output<br />-o           | -o outputFileName                                            | Set the output file                                          |
| --phased<br />-p           | -p true or -p false                                          | Set the status of genotype: phased (true) or unphased (false). (default: false) |
| --threads<br />-t          | -t [x], [x] is an integer ≥ 1                                | Set the number of threads. (default: 4)                      |
| --blockSizeType<br />-bs   | -bs [x], [x] is an integer in the range of [0, 7]            | Set the maximum size of each block (the true block size is  $2^{7+x}$​ ) (default: 6). |
| --reordering<br />-r       | -r true or -r false                                          | Use the AMDO algorithm to reorder the genotype arrays. (default: true) |
| --windowSize<br />-ws      | -ws [x], [x] is an integer ≥ 2                               | Set the window size of the AMDO algorithm. (Execute only if -r is true, default: 24) |
| --compressor<br />-c       | -c [x], [x] is an integer in the range of [0, 3] or the corresponding name of compressor | Set the algorithm for compression (0: ZSTD, 1: LZMA, 2: EMPTY) , and the reserved 2 and 3 can be configured by other developers. |
| --level<br />-l            | -l [x], [x] is an integer ≤ 31 (default: -1, indicates the default value for each compressor) | Parameters for compression algorithm the larger number, the higher compression ratio with slower speed (ZSTD: 0~22, default: 16; LZMA: 0~9, default: 3). |
| --readyParas<br />-rp      | -rp gtbFileName                                              | Import the template parameters (-p, -bs, -c, -l) from an external file. |
| --yes<br />-y              | -y or --yes                                                  | Overwrite files without asking.                              |

**Variants quality control**

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --seq-ac                   | two forms are allowed: --seq-ac minAC or --seq-ac minAC-maxAC | Exclude variants with the minimal alternate allele count per variant < minAc (--seq-ac minAc) or out of the range [minAc, maxAc] (--seq-ac minAc-maxAc). |
| --seq-af                   | two forms are allowed: --seq-af minAF or --seq-af minAF-maxAF | Exclude variants with the minimal alternate allele frequency per variant < minAf (--seq-af minAf) or out of the range [minAf, maxAf] (--seq-af minAf-maxAf). |
| --seq-an                   | --seq-an minAN                                               | Exclude variants with the minimum non-missing allele number per variant < AN. (default: 0) |
| --max-allele               | --max-allele [x], [x] is an integer in the range of [2, 15]  | Exclude variants with alleles over --max-allele. (2-15, default: 15) |

#### 3.3. Use API in Java

```java
// Combine the population vcf files with 10 threads, and set the status of the genotype as unphased, with a block size  8192
IBuildTask task = new MergeTask(new String[]{"./example/1000GP3/AMR.gtb",
        "./example/1000GP3/EAS.gtb", "./example/1000GP3/EUR.gtb", 
        "./example/1000GP3/SAS.gtb", "./example/1000GP3/AFR.gtb", },
        "./example/1000GP3.gtb")
        .setPhased(false)
        .setParallel(10)
        .setBlockSizeType(6);
task.submit();
```

### 4. View the basic information of GTB file - operational mode for "show"

View the GTB file with the following command:

```shell
GBC show <inputFileName1, inputFileName2, ...> [options]
```

#### 4.1. Run the examples with the command line

```shell
# View the information of the compressed 1000GP3.phased.gtb file
GBC show ./example/1000GP3.phased.gtb

# View all the information of the compressed assoc.hg19.gtb file
GBC show ./example/assoc.hg19.gtb --full

# View the sample names of the compressed 1000GP3.phased.gtb file
GBC show ./example/1000GP3.phased.gtb --list-subject-only > ./example/1000GP3.phased.subjects.txt
```

#### 4.2. List of parameters 

| Parameters<img width=150/> | Parameter type/usage          | Description                                                  |
| -------------------------- | ----------------------------- | ------------------------------------------------------------ |
| --list-md5                 | --list-md5                    | Print the message-digest fingerprint (checksum) for file (which may take a long time to calculating for huge files). |
| --list-baseInfo            | --list-baseInfo               | Print the basic information of the GTB file (by default, only print the state of phased and random access enable). |
| --list-subject             | --list-subject                | Print subjects names of the GTB file.                        |
| --list-subject-only        | --list-subject-only           | Print subjects names of the GTB file only.                   |
| --list-tree                | --list-tree                   | Print information of the GTBTrees (chromosome only).         |
| --list-node                | --list-node                   | Print information of the GTBNodes.                           |
| --list-chromosome          | --list-chromosome 1,2,...,X,Y | Print information of the specified chromosome nodes (e.g., --list-chromosome 1,2,3). |
| --full, -f                 | --full or -f                  | Print all information of the GTB file (except md5).          |

#### 4.3. Use API in Java

```java
GTBManager manager = GTBRootCache.get("./example/assoc.hg19.gtb");
ManagerStringBuilder stringBuilder = manager.getManagerStringBuilder();
// Calculate the md5 for file
stringBuilder.calculateMd5(true);
// Print the basic information of the file
stringBuilder.listFileBaseInfo(true);
// Print information of the GTBTrees of chromosome
stringBuilder.listChromosomeInfo(true);
System.out.println(stringBuilder.build());
```

### 5. Extract data from the compressed GTB file - operational mode for "extract"

Extract the data of the GTB files with the following command:

```shell
GBC extract <inputFileName>  [options]
```

#### 5.1. Run the examples with the command line

```shell
# Decompress all the variant information of assoc.hg19.gtb file, and compress the information with BGZF format
GBC extract ./example/assoc.hg19.gtb --output ./example/assoc.hg19.simple.vcf.gz

# Extract the information of chr1:10177-1000000 from the 1000GP3.phased.gtb file
GBC extract ./example/1000GP3.phased.gtb --range 1:10177-1000000

# Extract the information of chr1-5 from the 1000GP3.phased.gtb file, split by the chromosome and save as bgz format
GBC extract ./example/1000GP3.phased.gtb --retain chrom=1,2,3,4,5 --o-bgz --split -o ./example/extract

# Extract the variant information according to the given file "query_1000GP3.txt", with each line contains the information of chromosome and position
GBC extract ./example/1000GP3.phased.gtb --ramdom ./example/query_1000GP3.txt

# Extract the variants whose allele counts is in the range of  1~20  in assoc.hg19.gtb file
GBC extract ./example/assoc.hg19.gtb --filterByAC 1-20
```

#### 5.2. List of parameters

**Output parameters**

| Parameters<img width=150/> | Parameter type/usage                             | Description                                                  |
| -------------------------- | ------------------------------------------------ | ------------------------------------------------------------ |
| --output<br />-o           | -o outputFileName                                | Set the output file (inputFileName.vcf by default, and inputFileName.vcf.gz by default with --o-bgz) |
| --o-bgz                    | --o-bgz                                          | Compress the output file by bgzip. (if the output file specified by -o is end with .gz, this command will be executed automatically) |
| --level<br />-l            | -l [x], [x] is an integer in the range of [0, 9] | Set the compression level. (Execute only if --o-bgz is passed in, default: 5) |
| --threads<br />-t          | -t [x], [x] is an integer ≥ 1                    | Set the number of threads. (default: 4)                      |
| --phased<br />-p           | -p true or -p false                              | Force-set the status of the genotype. (default: same as the GTB basic information) |
| --split                    | --split true or --split false                    | Set the format of the output file (split by the chromosome or not). |
| --hideGT<br />-hg          | -hg true or -hg false                            | Do not output the genotype pair.                             |
| --yes<br />-y              | -y or --yes                                      | Overwrite files without asking.                              |

**Sample selection**

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --subject<br />-s          | two forms are allowed: --subject subject1,subject2,…or --subject @filename | Extract the information of the specified subjects (--subject subject1,subject2,...). Subject name can be stored in a file with ',' delimited form, and pass in via `--subject @file`. |

**Variant extraction (all the variants by default)**

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --random                   | One line represents a variant: chrom,pos or chrom<\t>pos     | Extract the information by position. (An inputFile is needed here, with each line contains `chrom,position` or `chrom<\t>position`) |
| --range                    | three forms are allowed: --range chrom or --range chrom:start or --range chrom:start-end | Extract the information by position range. (format: --range chrom; --range chrom:start; --range chrom:start-end) |
| --node                     | two forms are allowed: --node chrom=1,2,3 or --node chrom=1,2,3-node=1,2,3 | Extract the information by nodeIndex. (e.g., --node chrom=1,2,3; --node chrom=1,2,3-node=1,2,3 chrom=5-node=5,6) |

**Variants quality control**

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --filterByAC<br />-fAC     | two forms are allowed: --filterByAC minAC or --filterByAC minAC-maxAC | Set the minimum alternate allele count (AC) or the AC range and extract the corresponding information. (--filter minAC; --filterByAC minAC-maxAC) |
| --filterByAF<br />-fAF     | two forms are allowed: --filterByAF minAF or --filterByAF minAF-maxAF | Set the minimum alternate allele frequency (AF) or the AF range and extract the corresponding information (--filter minAF; --filterByAF minAF-maxAF). |
| --filterByAN<br />-fAN     | --filterByAN minAN                                           | Set the minimum non-missing allele number (AN) and extract the corresponding information (--filter minAN). (default: 0) |

#### 5.3. Use API in Java

```java
// Decompress all the variant information of assoc.hg19.gtb file
ExtractTask task = new ExtractTask("./example/assoc.hg19.gtb");
task.decompressAll();

// Decompress all the variant information of assoc.hg19.gtb file, and compress the information with BGZF format
task = new ExtractTask("./example/assoc.hg19.gtb", "./example/assoc.hg19.simple.vcf.gz")
        .setCompressToBGZF();
task.decompressAll();

// Extract the information of chr1:10177-1000000 from the 1000GP3.phased.gtb file
task = new ExtractTask("./example/1000GP3.phased.gtb");
task.decompress(1, 10177, 1000000);

// Extract the information of chr3 from the 1000GP3.phased.gtb file
task = new ExtractTask("./example/1000GP3.phased.gtb");
task.decompress(3);

// Extract the variant information according to the given file "query_1000GP3.txt", with each line contains the information of chromosome and position
task = new ExtractTask("./example/1000GP3.phased.gtb");
task.decompressByPositionFromFile("query_1000GP3.txt", "\n", ",");

// Extract the variants whose allele counts is in the range of  1~20  in assoc.hg19.gtb file
task = new ExtractTask("./example/1000GP3.phased.gtb").filterByAC(1, 20);
task.decompressAll();
```

### 6. Edit the GTB file - operational mode for "edit"

Edit the GTB file with the following command:

```shell
GBC edit <inputFileName> [options]
```

Edit mode allows users to manipulate nodes without decompress the GTB files, it will be faster to delete, retain, add the node information and combine different vcf files. We recommend using the operational mode "edit" in the graphical interface mode, which is more intuitive.

#### 6.1. Run the examples with the command line

```shell
# Extract and retain the variant information in node1 and node2 separately
GBC edit ./example/assoc.hg19.gtb --output ./example/node1.gtb --retain chrom=1-node=1
GBC edit ./example/assoc.hg19.gtb --output ./example/node2.gtb --retain chrom=1-node=2

# Combine the node1.gtb and node2.gtb to node1_2_2.gtb file
GBC edit ./example/node1.gtb --output ./example/node1_2_2.gtb --merge ./example/node2.gtb

# Extract and retain the variant information in node1 and node2 together
GBC edit ./example/assoc.hg19.gtb --output ./example/node1_2_1.gtb --retain chrom=1-node=1,2

# Check the md5 for node1_2_1.gtb and node1_2_2.gtb
GBC md5 ./example/node1_2_1.gtb ./example/node1_2_2.gtb

# Split the 1000GP3.phased.gtb file according to the chromosome
GBC edit ./example/1000GP3.phased.gtb --split true --output ./example/1000GP3.phased
```

#### 6.2. List of parameters

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --output<br />-o           | -o outputFileName                                            | Set the output file. If not specified, the output file will **overwrite** the original file. |
| --retain                   | two forms are allowed: --retain chrom=1,2,3  --retain chrom=1,2,3-node=1,2,3 | Retain the specified GTBNodes (e.g., --retain chrom=1,2,3; --retain chrom=1,2,3-node=1,2,3 chrom=5-node=5,6). |
| --delete                   | two forms are allowed: --delete chrom=1,2,3  --delete chrom=1,2,3-node=1,2,3 | Delete the specified GTBNodes (e.g., --delete chrom=1,2,3; --delete chrom=1,2,3-node=1,2,3 chrom=5-node=5,6). |
| --concat                   | --merge gtbFileName1 gtbFileName1 …                          | Concatenate multiple VCF files. All source files must have the same subjects columns appearing in the same order with entirely different sites, and all files must have to be the same in parameters of the status (--phased). (e.g., --concat file1 file2 file3 ...) |
| --unique                   | --unique                                                     | Retain the unique GTBNodes. (default: false)                 |
| --reset-subject            | --reset-subject subject1,subject2,... or --reset-subject @file | Reset subject names (request that same subject number and no duplicated names) for gtb file directly (--reset-subject subject1,subject2,...). Subject names can be stored in a file with ',' delimited form, and pass in via `--reset-subject @file`. |
| --split, -s                | --split true or --split false                                | Set the format of the output file (split by the chromosome or not). |
| --yes<br />-y              | -y or --yes                                                  | Overwrite files without asking.                              |

Parameters "--retain", "--delete", "--merge" and "--unique" may not be used together under the command line mode (only one at a time), while there is no such restriction under the graphical interface mode (GUI editing mode) or with API tools.

#### 6.3. Use API in Java

```java
// Extract and retain the variant information in node1 and node2 separately
EditTask task = new EditTask("./example/assoc.hg19.gtb");
task.setOutputFileName("./example/node1.gtb").retain(1, new int[]{1}).submit();
task.setOutputFileName("./example/node2.gtb").retain(1, new int[]{2}).submit();

// Extract and retain the variant information in node1 and node2 together
task.setOutputFileName("./example/node1_2_1.gtb").retain(1, new int[]{1, 2}).submit();

// Combine the node1.gtb and node2.gtb to node1_2_2.gtb file
task = new EditTask("./example/node1.gtb")
        .concat("./example/node2.gtb")
        .setOutputFileName("./example/node1_2_2.gtb");
task.submit();

// Split the 1000GP3.phased.gtb file according to the chromosome
task = new EditTask("./example/1000GP3.phased.gtb")
        .split(true);
task.submit();
```

### 7. Calculate LD for GTB file —— operational mode for "ld"

Calculate LD for the **ordered GTB** file with the following command:

```shell
GBC ld <inputFileName> [options]
```

#### 7.1. Run the examples with the command line

```shell
# calculate the LD for 1000GP3 dataset
GBC ld ./example/1000GP3.phased.gtb --model --geno --window-bp 10000 --maf 0.1 --min-r2 0.2
GBC ld ./example/1000GP3.phased.gtb --model --hap
```

#### 7.2. List of parameters

**Output Options:** 

| Parameters<img width=150/> | Parameter type/usage                             | Description                                                  |
| -------------------------- | ------------------------------------------------ | ------------------------------------------------------------ |
| --output<br />-o           | -o outputFileName                                | Set the output file (inputFileName.ld by default, and inputFileName.ld.gz by default with --o-bgz) |
| --o-bgz                    | --o-bgz                                          | Compress the output file by bgzip. (if the output file specified by -o is end with .gz, this command will be executed automatically) |
| --level<br />-l            | -l [x], [x] is an integer in the range of [0, 9] | Set the compression level. (Execute only if --o-bgz is passed in, default: 5) |
| --threads<br />-t          | -t [x], [x] is an integer ≥ 1                    | Set the number of threads. (default: 4)                      |
| --yes<br />-y              | -y or --yes                                      | Overwrite files without asking.                              |

**LD Calculation Options:** 

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --model<br />-m            | Haplotype LD: hap, --hap, --hap--r2, --hap-ld<br />Genotype LD: geno, --geno, --geno--r2, --geno-ld<br /> | Calculate pairwise the linkage disequilibrium (--hap) or genotypic correlation (--geno). (default: phased GTB: `--hap`, unphased GTB: `--geno`) |
| --window-bp<br />-bp       | -bp [x], [x] is an integer ≥ 1                               | The maximum number of physical bases between the variants being calculated for LD. (default: 10000) |
| --min-r2                   | --min-r2 [x], [x] is a float number in the range [0, 1]      | Exclude pairs with R2 values less than --min-r2. (default: 0.2) |
| --maf                      | --maf [x], [x] is a float number in the range [0, 1]         | Exclude variants with the minor allele frequency (MAF) per variant < --maf. (default: 0.05) |

**Site Selection and Subject Selection:** 

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --range                    | three forms are allowed: --range chrom or --range chrom:start or --range chrom:start-end | Calculate the LD by specified position range.                |
| --subject<br />-s          | two forms are allowed: --subject subject1,subject2,…or --subject @filename | Calculate the LD of the specified subjects (--subject subject1,subject2,...). Subject name can be stored in a file with ',' delimited form, and pass in via `--subject @file`. |

#### 7.3. Use API in Java

```java
LDTask task = new LDTask("./example/1000GP3.phased.gtb");
task.setWindowSizeBp(10000)
        .setMaf(0.1)
        .setMinR2(0.2)
        .setLdModel(ILDModel.GENOTYPE_LD);
task.submit();
```

### 8. Auxiliary tool

#### 8.1. Construct contig file

```shell
GBC index inputFileName
```

When executing other commands, specify the corresponding contig file by --contig contigFIle. Otherwise, the input files will be processed according to the human genome by default.

#### 8.2. Parallel BGZIP compression algorithm

GBC integrates a pure Java version of the parallel BGZIP compression tool, using with the following command:

```shell
GBC bgzip inputFileName [options]
```

| Parameters<img width=150/> | Parameter type/usage                                         | Description                                                  |
| -------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --output<br />-o           | -o outputFileName, if not specified, it will be generated automaticly | Set the output file.                                         |
| --decompress<br />-d       | -d or --decompress                                           | Decompression.                                               |
| --cut<br />-c              | two forms are allowed:  --cut start or --cut start-end       | Cut the bgzip file by pointer range. (format: --cut start; --cut start-end) |
| --threads<br />-t          | -t [x], [x] is an integer ≥ 1                                | Set the number of threads. (default: 4)                      |
| --level<br />-l            | -l [x], [x] is an integer in the range of [0, 9]             | Compression level to use when compressing. The larger number, the higher compression ratio with the slower speed). (bgzip: 0~9, default: 5) |
| --yes, -y                  | -y or --yes                                                  | Overwrite files without asking.                              |

#### 8.3. MD5 verification tool

Verify the MD5 code of the file with the following command:

```shell
GBC md5 <inputFileName1, inputFileName2, ...> [--o-md5]
```

The optional parameter `--o-md5` helps export md5 file to the location of input file.

## IV. Command line interactive mode

The input parameters of command line interaction mode are the same as those of command line mode, but all the commands are in the same process, which will reduce the time spent on JVM startup and just-in-time compilation (JIT). 

When you type a command, you no longer need to specify `java -Xmx4g -Xms4g -jar gbc.jar`. The command line interaction mode has four additional parameters in addition to the parameters in command line mode:

| Parameters             | Description                                                  |
| ---------------------- | ------------------------------------------------------------ |
| exit, q, quit          | Exit program, exit the command line interaction mode.        |
| clear                  | Clearing the screen (actually printing out multiple blank lines). |
| reset                  | Clear the data buffer.                                       |
| Lines begin with a "#" | For annotation.                                              |

![interactive-mode](images/interactive-mode.png)

## V. The API documentation

API document will be supplementary by November.

### Unified IO framework —— FileStream

