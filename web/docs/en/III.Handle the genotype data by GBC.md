The four interface modes of the GBC provide the following operational modes, and at most one operation mode is specified at each run time, as shown following:

| Operational Mode | Command                     | Illustration                                                 |
| ---------------- | --------------------------- | ------------------------------------------------------------ |
| build            | build <input(s)\> [options] | Compress and build *.gtb for vcf/vcf.gz files.               |
| rebuild          | rebuild <input\> [options]  | Rebuild *.gtb with new parameters.                           |
| extract          | extract <input\> [options]  | Retrieve variant from *.gtb file.                            |
| edit             | edit <input\> [options]     | Edit the GTB file directly.                                  |
| merge            | merge <input(s)\> [options] | Merge multiple GTB files (with non-overlapping subject sets) into a single GTB file. |
| show             | show <input(s)\> [options]  | Display summary of the GTB File.                             |
| index            | index <input\> [options]    | Index contig file for specified VCF file or reset contig file for specified GTB file. |
| ld               | ld <input\> [options]       | Calculate pairwise the linkage disequilibrium or genotypic correlation. |
| bgzip            | bgzip <mode\> [options]     | Use parallel bgzip to compress a single file.                |
| md5              | md5 <input(s)\> [--o-md5]   | Calculate a message-digest fingerprint (checksum) for file.  |

### 1. Build the compressed GTB file - operational mode for "build"

Compress the genomic VCF file with the following command:

```shell
GBC build <input(s)> [options]
```

- GBC help compress the file in compliance with [VCF  Specification](https://samtools.github.io/hts-specs/VCFv4.2.pdf), and all GBC operations are based on the assumption that the file format is compliant with this specification;
- The inputFileName can be a single .vcf file or .vcf.gz file, and it can also be the path of the folder containing all the files to be compressed. When a folder path is given, the GBC will help filter out all .vcf or .vcf.gz files in this folder (and its sub-folders) for compression.
- The GBC currently only supports compression of the human genome, for chromosome, GBC only supports 1-22, X, Y and 1-22, X, Y with the "chr" prefix (such as chr1, chrY); For other species, please use command  `GBC index <inputFileName>`  to build the contig file first, and users should add the command `--contig <contigFile>` when compressing.
- When using GBC to combine and compress multiple files,  the files are required to have the same samples (can be disordered). If the sample of a file is a subset of other files, it can also be compressed correctly, and the missing genotypes will be replaced by .|.;

#### 1.1. Run the examples with the command line

```shell
# Compress the assoc.hg19.vcf.gz file
GBC build ./example/assoc.hg19.vcf.gz -p

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

**Compressor Options:**

| Parameters           | Parameter type/usage | Description                                              |
| :----------------------- | ---------------------------- | ------------------------------------------------------------ |
| --contig                 | --contig <file\>           | Specify the corresponding contig file.     |
| --output<br />-o         | -o <file\>                 | Set the output file.                           |
| --threads<br />-t        | -t <int, ≥1\>               | Set the number of threads. (default: 4)  |
| --phased<br />-p         | -p                           | Set the status of genotype to phased.  |
| --blockSizeType<br />-bs | -bs <int, 0~7\>            | Set the maximum size=$2^{7+x}$ of each block. (default: -1) |
| --no-reordering<br />-nr | -nr                          | Disable the Approximate Minimum Discrepancy Ordering (AMDO) algorithm. |
| --windowSize<br />-ws    | -ws <1~131072\>            | Set the window size of the AMDO algorithm. (default: 24) |
| --compressor<br />-c     | -c [0/1]<br />-c [ZSTD/LZMA] | Set the basic compressor for compressing processed data. (default: ZSTD) |
| --level<br />-l          | -l <int\>                  | Compression level to use when basic compressor works. (ZSTD: 0~22, default: 16; LZMA: 0~9, default: 3) |
| --readyParas<br />-rp    | -rp <file\>                | Import the template parameters (-p, -bs, -c, -l) from an external GTB file. |
| --yes<br />-y            | -y                           | Overwrite output file without asking. |

**Quality Control Options:**

| Parameters   | Parameter type/usage                                         | Description                                                  |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --gty-gq     | --gty-gq <int, ≥0\>                                          | Exclude genotypes with the minimal genotyping quality (Phred Quality Score) per genotype < gq. (default: 20) |
| --gty-dp     | --gty-dp <int, ≥0\>                                          | Exclude genotypes with the minimal read depth per genotype < dp. (default: 4) |
| --seq-qual   | --seq-qual <int, ≥0\>                                        | Exclude variants with the minimal overall sequencing quality score (Phred Quality Score) per variant < qual. (default: 30) |
| --seq-dp     | --seq-dp <int, ≥0\>                                          | Exclude variants with the minimal overall sequencing read depth per variant < dp. (default: 0) |
| --seq-mq     | --seq-mq <int, ≥0\>                                          | Exclude variants with the minimal overall mapping quality score (Mapping Quality Score) per variant < mq. (default: 20) |
| --seq-ac     | --seq-ac minAC-<br />--seq-ac -maxAC<br />--seq-ac minAC-maxAC | Exclude variants with the alternate allele count (AC) per variant out of the range [minAc, maxAc]. |
| --seq-af     | --seq-af minAF-<br />--seq-af -maxAF<br />--seq-ac minAF-maxAF | Exclude variants with the alternate allele frequency (AF) per variant out of the range [minAf, maxAf]. |
| --seq-an     | --seq-an minAN-<br />--seq-an -maxAN<br />--seq-an minAN-maxAN | Exclude variants with the non-missing allele number (AN) per variant out of the range [minAn, maxAn]. |
| --max-allele | --max-allele <int, 2~15\>                                    | Exclude variants with alleles over --max-allele.             |
| --no-qc      | --no-qc                                                      | Disable all quality control methods.                         |

#### 1.3. Recommendation for parameters

- The genotypic quality information will be discarded after the extraction of genotype data,  and the quality control process is started by default (if quality information exist) because the low-quality sequencing data is unreliable. If the quality control is not required, please set the parameters as "--no-qc";
- AMDO algorithm helps significantly improve compression (increase the compression ratio and compression speed) in most cases (-r true);
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

// Compress the files of different populations in 1000GP3
for (String population: new String[]{"AFR", "AMR", "EAS", "EUR", "SAS"}){
    task = new BuildTask("./example/1000GP3/" + population);
    task.submit();
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
GBC rebuild <input> [options]
```

- The "rebuild" operational mode will modify the GTB file permanently. These operations involve the process of re-decompressing and re-compressing the GTB file. Thus, this mode allows conducting the file sorting.
- For the operations that without any modification of the header information (such as delete or retain some specified nodes), we recommend using the operational mode "edit" because these operations do not require decompression, which can greatly improve the speed.

#### 2.1. Run the examples with the command line

```shell
# Rebuild the assoc.hg19.gtb file with 6 threads, and set the status of the genotype as phased, with a block size 4096
GBC rebuild ./example/assoc.hg19.gtb --phased --threads 6 --blockSizeType 5 --output ./example/assoc.hg19.unphased.gtb
```

#### 2.2. List of parameters

**Compressor Options:**

| Parameters           | Parameter type/usage | Description                                              |
| :----------------------- | ---------------------------- | ------------------------------------------------------------ |
| --contig                 | --contig <file\>           | Specify the corresponding contig file.     |
| --output<br />-o         | -o <file\>                 | Set the output file. |
| --threads<br />-t        | -t <int, ≥1\>               | Set the number of threads. (default: 4) |
| --phased<br />-p         | -p                           | Set the status of genotype to phased.  |
| --blockSizeType<br />-bs | -bs <int, 0~7\>            | Set the maximum size=$2^{7+x}$ of each block. (default: -1) |
| --no-reordering<br />-nr | -nr                          | Disable the Approximate Minimum Discrepancy Ordering (AMDO) algorithm. |
| --windowSize<br />-ws    | -ws <int, 1~131072\>       | Set the window size of the AMDO algorithm. (default: 24) |
| --compressor<br />-c     | -c [0/1]<br />-c [ZSTD/LZMA] | Set the basic compressor for compressing processed data. (default: ZSTD) |
| --level<br />-l          | -l <int\>                  | Compression level to use when basic compressor works. (ZSTD: 0~22, default: 16; LZMA: 0~9, default: 3) |
| --readyParas<br />-rp    | -rp <file\>                | Import the template parameters (-p, -bs, -c, -l) from an external GTB file. |
| --yes<br />-y            | -y                           | Overwrite output file without asking. |

**Normalize Variants Options:**

| Parameters     | Parameter type/usage | Description                                                  |
| -------------- | -------------------- | ------------------------------------------------------------ |
| --multiallelic | --multiallelic       | Join multiple biallelic variants with the same coordinate for a multiallelic variant. |
| --biallelic    | --biallelic          | Split multiallelic variants into multiple biallelic variants. |

**Alignment Coordinate Options:**

| Parameters            | Parameter type/usage                    | Description                                                  |
| --------------------- | -------------------------------------- | ------------------------------------------------------------ |
| --align               | --align <file\>                      | Filter coordinates and adjust reference base pairs |
| --check-allele        | --check-allele                         | Correct for potential complementary strand errors based on allele labels (A and C, T and G; only biallelic variants are supported). InputFiles will be resorted according the samples number of each GTB File. |
| --p-value             | --p-value <float, 0.000001~0.5\>       | Correct allele labels of rare variants (minor allele frequency < --maf) with the p-value of chi^2 test >= --p-value.   (default: 0.05) |
| --freq-gap            | --freq-gap <float, 0.000001~0.5\>      | Correct allele labels of rare variants (minor allele frequency < --maf) with the allele frequency gap >= --freq-gap. |
| --no-ld               | --no-ld                                | By default, correct allele labels of common variants (minor allele frequency >= --maf) using the ld pattern in different files. Disable this function with option '--no-ld'. |
| --min-r               | --min-r <float, 0.5~1.0\>              | Exclude pairs with genotypic LD correlation less than --min-r. (default: 0.8) |
| --flip-scan-threshold | --flip-scan-threshold <float, 0.5~1.0\> | Variants with flipped ld patterns (strong correlation coefficients of opposite signs) that >= threshold ratio will be corrected. (default: 0.8) |
| --maf                 | --maf <float, 0~1\>                    | For common variants (minor allele frequency >= --maf) use LD to identify inconsistent allele labels. (default: 0.05) |
| --window-bp<br />-bp  | -bp <int, ≥1\>                        | The maximum number of physical bases between the variants being calculated for LD. (default: 10000) |
| --method<br />-m      | -m [union/intersection]                | Method for handing coordinates in different files (union or intersection). (default: intersection) |

**Subset Selection Options:**

| Parameters        | Parameter type/usage                                         | Description                                                  |
| ----------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --subject<br />-s | --subject <string\>,<string\>,…<br />--subject @<file\>      | Retain the genotypes for the specified subjects. Subject name can be stored in a file with ',' delimited form, and pass in via '-s @file'. |
| --retain          | --retain chrom=<string\>,<string\>,...<br />--retain chrom=<string\>,<string\>,...;node=<int\>,<int\>,... | Retain the specified GTBNodes.                               |
| --delete          | --delete chrom=<string\>,<string\>,...<br />--delete chrom=<string\>,<string\>,...;node=<int\>,<int\>,... | Delete the specified GTBNodes.                               |
| --range<br />-r   | --range <chrom\><br />--range <chrom\>:<start\>-<br />--range <chrom\>:-<end\><br />--range <chrom\>:<start\>-<end\> | Retain the variants by range of coordinates.                 |
| --random          | --random <file\>                                             | Retain the variants by specified coordinates. (An inputFile is needed here, with each line contains 'chrom,position' or 'chrom<\t> position'. |

**Quality Control Options:**

| Parameters   | Parameter type/usage                                         | Description                                                  |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --seq-ac     | --seq-ac <minAc\>-<br />--seq-ac -<maxAc\><br />--seq-ac <minAc\>-<maxAc\> | Exclude variants with the alternate allele count (AC) per variant out of the range [minAc, maxAc]. |
| --seq-af     | --seq-af <minAf\>-<br />--seq-af -<maxAf\><br />--seq-af <minAf\>-<maxAf\> | Exclude variants with the alternate allele frequency (AF) per variant out of the range [minAf, maxAf]. |
| --seq-an     | --seq-an<minAn\>-<br />--seq-an -<maxAn\><br />--seq-an <minAn\>-<maxAn\> | Exclude variants with the non-missing allele number (AN) per variant out of the range [minAn, maxAn]. |
| --max-allele | --max-allele <int, 2~15\>                                    | Exclude variants with alleles over --max-allele.             |

#### 2.3. Use API in Java

```java
// Rebuild the assoc.hg19.gtb file with 6 threads, and set the status of the genotype as unphased, with a block size 4096
RebuildTask task = new RebuildTask("./example/assoc.hg19.gtb", "./example/assoc.hg19.unphased.gtb");
task.setPhased(true)
        .setParallel(6)
        .setBlockSizeType(5)
        .filterByAC(1, 1965);

task.rebuildAll();
```

### 3. Extract data from the compressed GTB file - operational mode for "extract"

Extract the data of the GTB files with the following command:

```shell
GBC extract <input> [options]
```

#### 3.1. Run the examples with the command line

```shell
# Decompress all the variant information of assoc.hg19.gtb file, and compress the information with BGZF format
GBC extract ./example/assoc.hg19.gtb --output ./example/assoc.hg19.simple.vcf.gz

# Extract the information of chr1:10177-1000000 from the 1000GP3.phased.gtb file
GBC extract ./example/1000GP3.phased.gtb --range 1:10177-1000000

# Extract the information of chr1-5 from the 1000GP3.phased.gtb file, split by the chromosome and save as bgz format
GBC extract ./example/1000GP3.phased.gtb --node chrom=1,2,3,4,5 --o-text

# Extract the variant information according to the given file "query_1000GP3.txt", with each line contains the information of chromosome and position
GBC extract ./example/1000GP3.phased.gtb --ramdom ./example/query_1000GP3.txt

# Extract the variants whose allele counts is in the range of  1~20  in assoc.hg19.gtb file
GBC extract ./example/assoc.hg19.gtb --seq-ac 1-20
```

#### 3.2. List of parameters

**Output parameters**

| Parameters        | Parameter type/usage | Description                                                  |
| ----------------- | ----------------- | ------------------------------------------------------------ |
| --contig          | --contig <file\> | Specify the corresponding contig file.     |
| --output<br />-o  | -o <file\>      | Set the output file.                           |
| --o-text          | --o-text          | Output VCF file in text format. (this command will be executed automatically if '--o-bgz' is not passed in and the output file specified by '-o' is not end with '.gz') |
| --o-bgz           | --o-bgz           | Output VCF file in bgz format. (this command will be executed automatically if '--o-text' is not passed in and the output file specified by '-o' is end with '.gz') |
| --level<br />-l   | -l <int, 0~9\>  | Set the compression level. (Execute only if --o-bgz is passed in) (default: 5) |
| --threads<br />-t | -t <int, ≥1\>    | Set the number of threads. (default: 4)  |
| --phased<br />-p  | -p [true/false]   | Force-set the status of the genotype. (same as the GTB basic information by default) |
| --hideGT<br />-hg | -hg               | Do not output the sample genotypes (only CHROM, POS, REF, ALT, AC, AN, AF). |
| --yes<br />-y     | -y                | Overwrite files without asking. |

**Subset Selection Options:**

| Parameters        | Parameter type/usage                                         | Description                                                  |
| ----------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --subject<br />-s | --subject <string\>,<string\>,…<br />--subject @<file\>      | Extract the information of the specified subjects. Subject name can be stored in a file with ',' delimited form, and pass in via '-s @file'. |
| --range<br />-r   | --range <chrom\><br />--range <chrom\>:<start\>-<br />--range <chrom\>:-<end\><br />--range <chrom\>:<start\>-<end\> | Extract the information by position range.                   |
| --random          | --random <file\>                                             | Extract the information by position. (An inputFile is needed here, with each line contains 'chrom,position' or 'chrom<\t> position'. |
| --node            | --node chrom=<string\>,<string\>,...<br />--node chrom=<string\>,<string\>,...;node=<int\>,<int\>,... | Extract the information by nodeIndex.                        |

**Filter Options:**

| Parameters | Parameter type/usage                                         | Description                                                  |
| ---------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --seq-ac   | --seq-ac <minAc\>-<br />--seq-ac -<maxAc\><br />--seq-ac <minAc\>-<maxAc\> | Exclude variants with the alternate allele count (AC) per variant out of the range [minAc, maxAc]. |
| --seq-af   | --seq-af <minAf\>-<br />--seq-af -<maxAf\><br />--seq-af <minAf\>-<maxAf\> | Exclude variants with the alternate allele frequency (AF) per variant out of the range [minAf, maxAf]. |
| --seq-an   | --seq-an<minAn\>-<br />--seq-an -<maxAn\><br />--seq-an <minAn\>-<maxAn\> | Exclude variants with the non-missing allele number (AN) per variant out of the range [minAn, maxAn]. |

#### 3.3. Use API in Java

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
task.decompressByRange(1, 10177, 1000000);

// Extract the information of chr3 from the 1000GP3.phased.gtb file
task = new ExtractTask("./example/1000GP3.phased.gtb");
task.decompressByChromosome(3);

// Extract the variant information according to the given file "query_1000GP3.txt", with each line contains the information of chromosome and position
task = new ExtractTask("./example/1000GP3.phased.gtb");
task.decompressByPositionFromFile("query_1000GP3.txt", "\n", ",");

// Extract the variants whose allele counts is in the range of  1~20  in assoc.hg19.gtb file
task = new ExtractTask("./example/1000GP3.phased.gtb").filterByAC(1, 20);
task.decompressAll();
```

### 4. Edit the GTB file - operational mode for "edit"

Edit the GTB file with the following command:

```shell
GBC edit <input> [options]
```

Edit mode allows users to manipulate nodes without decompress the GTB files, it will be faster to delete, retain, add the node information and combine different vcf files. We recommend using the operational mode "edit" in the graphical interface mode, which is more intuitive.

#### 4.1. Run the examples with the command line

```shell
# Extract and retain the variant information in node1 and node2 separately
GBC edit ./example/assoc.hg19.gtb --output ./example/node1.gtb --retain "chrom=1;node=1"
GBC edit ./example/assoc.hg19.gtb --output ./example/node2.gtb --retain "chrom=1;node=2"

# Combine the node1.gtb and node2.gtb to node1_2_2.gtb file
GBC edit ./example/node1.gtb --output ./example/node1_2_2.gtb --concat ./example/node2.gtb

# Extract and retain the variant information in node1 and node2 together
GBC edit ./example/assoc.hg19.gtb --output ./example/node1_2_1.gtb --retain "chrom=1;node=1,2"

# Check the md5 for node1_2_1.gtb and node1_2_2.gtb
GBC md5 ./example/node1_2_1.gtb ./example/node1_2_2.gtb

# Split the 1000GP3.phased.gtb file according to the chromosome
GBC edit ./example/1000GP3.phased.gtb --split --output ./example/1000GP3.phased
```

#### 4.2. List of parameters

| Parameters       | Parameter type/usage                                         | Description                                                  |
| ---------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --contig         | --contig <file\>                                             | Specify the corresponding contig file.                       |
| --output<br />-o | -o <file\>                                                   | Set the output file.                                         |
| --unique         | --unique                                                     | Retain the unique GTBNodes.                                  |
| --delete         | --delete chrom=<string\>,<string\>,...<br />--delete chrom=<string\>,<string\>,...;node=<int\>,<int\>,... | Delete the specified GTBNodes.                               |
| --retain         | --retain chrom=<string\>,<string\>,...<br />--retain chrom=<string\>,<string\>,...;node=<int\>,<int\>,... | Retain the specified GTBNodes.                               |
| --concat         | --concat <file\> <file\> ...                                 | Concatenate multiple VCF files. All source files must have the same subjects columns appearing in the same order with entirely different sites, and all files must have to be the same in parameters of the status. |
| --reset-subject  | --subject <string\>,<string\>,…<br />--subject @<file\>      | Reset subject names (request that same subject number and no duplicated names) for gtb file directly. Subject names can be stored in a file with ',' delimited form, and pass in via '--reset-subject @file'. |
| --split, -s      | --split                                                      | Set the format of the output file (split by the chromosome or not). |
| --yes<br />-y    | -y                                                           | Overwrite output file without asking.                        |

Parameters "--retain", "--delete", "--merge" and "--unique" may not be used together under the command line mode (only one at a time), while there is no such restriction under the graphical interface mode (GUI editing mode) or with API tools.

#### 4.3. Use API in Java

```java
// Extract and retain the variant information in node1 and node2 separately
EditTask task = new EditTask("./example/assoc.hg19.gtb");
task.setOutputFileName("./example/node1.gtb").retain(1, new int[]{1}).submit();
task.setOutputFileName("./example/node2.gtb").retain(1, new int[]{2}).submit();

// Extract and retain the variant information in node1 and node2 together
task.setOutputFileName("./example/node1_2_1.gtb").retain(1, new int[]{1, 2}).submit();

// Concatenate the node1.gtb and node2.gtb to node1_2_2.gtb file
task = new EditTask("./example/node1.gtb")
        .concat("./example/node2.gtb")
        .setOutputFileName("./example/node1_2_2.gtb");
task.submit();

// Split the 1000GP3.phased.gtb file according to the chromosome
task = new EditTask("./example/1000GP3.phased.gtb")
        .split(true);
task.submit();
```

### 5. Merge multiple GTB file -  operational mode for "merge"

Merge multiple GTB file with the following command:

```shell
GBC merge <input(s)> [options]
```

- These files must have non-overlapping sample names. When the sample names overlap, users can use `edit <input> --reset-subject subject1,subject2,...` or  `edit <input> --reset-subject @file` to reset the sample names;
- The variants of the input files must be in order. If the variants are out of order, users can use `rebuild <input>` to sort the file.

- When multiple files are merged ($\ge 3$), the order of merging is determined by the number of samples as the minimum heap of weights.

#### 5.1. Run the examples with the command line

```shell
# Merge the population vcf files in 1000GP3
GBC merge ./example/1000GP3/AMR.gtb ./example/1000GP3/AFR.gtb \
./example/1000GP3/EAS.gtb ./example/1000GP3/EUR.gtb ./example/1000GP3/SAS.gtb \ 
--phased --threads 6 --blockSizeType 5 \
--output ./example/1000GP3.gtb
```

#### 5.2. List of parameters 

**Compressor Options:**

| Parameters               | Parameter type/usage         | Description                                                  |
| :----------------------- | ---------------------------- | ------------------------------------------------------------ |
| --contig                 | --contig <file\>           | Specify the corresponding contig file.     |
| --method<br />-m         | -m [union/intersection]      | Method for handing coordinates in different files (union or intersection), the missing genotype is replaced by '.'. (default: intersection) |
| --biallelic              | --biallelic                  | Split multiallelic variants into multiple biallelic variants. |
| --output<br />-o         | -o <file\>                 | Set the output file. |
| --threads<br />-t        | -t <int, ≥1\>               | Set the number of threads. (default: 4)  |
| --phased<br />-p         | -p                           | Set the status of genotype to phased.  |
| --blockSizeType<br />-bs | -bs <int, 0~7\>            | Set the maximum size=$2^{7+x}$ of each block. (default: -1) |
| --no-reordering<br />-nr | -nr                          | Disable the Approximate Minimum Discrepancy Ordering (AMDO) algorithm. |
| --windowSize<br />-ws    | -ws <int, 1~131072\>       | Set the window size of the AMDO algorithm. (default: 24) |
| --compressor<br />-c     | -c [0/1]<br />-c [ZSTD/LZMA] | Set the basic compressor for compressing processed data. (default: ZSTD) |
| --level<br />-l          | -l <int\>                  | Compression level to use when basic compressor works. (ZSTD: 0~22, default: 16; LZMA: 0~9, default: 3) |
| --readyParas<br />-rp    | -rp <file\>                | Import the template parameters (-p, -bs, -c, -l) from an external GTB file. |
| --yes<br />-y            | -y                           | Overwrite output file without asking. |

**Identify Inconsistent Allele Labels Options:**

| Parameters            | Parameter type/usage                    | Description                                                  |
| --------------------- | -------------------------------------- | ------------------------------------------------------------ |
| --check-allele        | --check-allele                         | Correct for potential complementary strand errors based on allele labels (A and C, T and G; only biallelic variants are supported). InputFiles will be resorted according the samples number of each GTB File. |
| --p-value             | --p-value <float, 0.000001~0.5\>       | Correct allele labels of rare variants (minor allele frequency < --maf) with the p-value of chi^2 test >= --p-value.   (default: 0.05) |
| --freq-gap            | --freq-gap <float, 0.000001~0.5\>      | Correct allele labels of rare variants (minor allele frequency < --maf) with the allele frequency gap >= --freq-gap. |
| --no-ld               | --no-ld                                | By default, correct allele labels of common variants (minor allele frequency >= --maf) using the ld pattern in different files. Disable this function with option '--no-ld'. |
| --min-r               | --min-r <float, 0.5~1.0\>              | Exclude pairs with genotypic LD correlation less than --min-r. (default: 0.8) |
| --flip-scan-threshold | --flip-scan-threshold <float, 0.5~1.0\> | Variants with flipped ld patterns (strong correlation coefficients of opposite signs) that >= threshold ratio will be corrected. (default: 0.8) |
| --maf                 | --maf <float, 0~1\>                    | For common variants (minor allele frequency >= --maf) use LD to identify inconsistent allele labels. (default: 0.05) |
| --window-bp<br />-bp  | -bp <int, ≥1\>                        | The maximum number of physical bases between the variants being calculated for LD. (default: 10000) |

**Variant Selection Options:**

| Parameters   | Parameter type/usage                                         | Description                                                  |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --seq-ac     | --seq-ac <minAc\>-<br />--seq-ac -<maxAc\><br />--seq-ac <minAc\>-<maxAc\> | Exclude variants with the alternate allele count (AC) per variant out of the range [minAc, maxAc]. |
| --seq-af     | --seq-af <minAf\>-<br />--seq-af -<maxAf\><br />--seq-af <minAf\>-<maxAf\> | Exclude variants with the alternate allele frequency (AF) per variant out of the range [minAf, maxAf]. |
| --seq-an     | --seq-an<minAn\>-<br />--seq-an -<maxAn\><br />--seq-an <minAn\>-<maxAn\> | Exclude variants with the non-missing allele number (AN) per variant out of the range [minAn, maxAn]. |
| --max-allele | --max-allele <int, 2~15\>                                    | Exclude variants with alleles over --max-allele.             |

#### 5.3. Use API in Java

```java
// merge the population vcf files with 10 threads, and set the status of the genotype as unphased, with a block size 8192
MergeTask task = new MergeTask(new String[]{"./example/1000GP3/AMR.gtb",
        "./example/1000GP3/EAS.gtb", "./example/1000GP3/EUR.gtb",
        "./example/1000GP3/SAS.gtb", "./example/1000GP3/AFR.gtb"},
        "./example/1000GP3.gtb");
task.setPhased(false)
        .setParallel(10)
        .setBlockSizeType(6);
task.submit();
```

### 6 View the basic information of GTB file - operational mode for "show"

View the GTB file with the following command:

```shell
GBC show <input> [options]
```

#### 6.1. Run the examples with the command line

```shell
# View the information of the compressed 1000GP3.phased.gtb file
GBC show ./example/1000GP3.phased.gtb

# View all the information of the compressed assoc.hg19.gtb file
GBC show ./example/assoc.hg19.gtb --full

# View the sample names of the compressed 1000GP3.phased.gtb file
GBC show ./example/1000GP3.phased.gtb --list-subject-only > ./example/1000GP3.phased.subjects.txt

# View the sites of the compressed 1000GP3.phased.gtb file
GBC show ./example/1000GP3.phased.gtb --list-site
```

#### 6.2. List of parameters

**Common Options:**

| Parameters          | Parameter type/usage                        | Description                                    |
| ------------------- | ------------------------------------------- | ---------------------------------------------- |
| --contig            | --contig <file\>                            | Specify the corresponding contig file.         |
| --assign-chromosome | --assign-chromosome <string\>,<string\>,... | Print information of the specified chromosome. |

**Summary View Options:**

| Parameters      | Parameter type/usage | Description                                                  |
| --------------- | -------------------- | ------------------------------------------------------------ |
| --list-md5      | --list-md5           | Print the message-digest fingerprint (checksum) for file (which may take a long time to calculating for huge files). |
| --list-baseInfo | --list-baseInfo      | Print the basic information of the GTB file (by default, only print the state of phased and random access enable). |
| --list-subject  | --list-subject       | Print subjects names of the GTB file.                        |
| --list-tree     | --list-tree          | Print information of the GTBTrees (chromosome only by default). |
| --list-node     | --list-node          | Print information of the GTBNodes.                           |
| --full, -f      | --full               | Print all abstract information of the GTB file (i.e., --list-baseInfo, --list-subject, --list-node). |

**GTB View Options:**

| Parameters           | Parameter type/usage | Description                                                  |
| -------------------- | -------------------- | ------------------------------------------------------------ |
| --list-subject-only  | --list-subject-only  | Print subjects names of the GTB file only.                   |
| --list-position-only | --list-position-only | Print coordinates (i.e., CHROM,POSITION) of the GTB file only. |
| --list-site          | --list-site          | Print coordinates, alleles and INFOs (i.e., CHROM,POSITION,REF,ALT,INFO) of the GTB file. |
| --list-gt            | --list-gt            | Print genotype frequency of the GTB file (used with '--list-site'). |

#### 6.3. Use API in Java

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

### 7. Build or convert contig files to custom chromosome labels - operational mode for "index"

Use the following commands for chromosome tag management (construction or conversion) of VCF files.

```shell
GBC index <input (VCF or GTB)> [options]
```

#### 7.1. Run the examples with the command line

```shell
# build contig for other species (http://dog10kgenomes.org/download.html)
GBC index ./example/Canis_familiaris.V85.vcf.gz --deep-scan

# reset contig for gtb file
GBC index ./example/assoc.hg19.gtb -to ./example/Canis_familiaris.V85.contig
```

#### 7.2. List of parameters

| Parameters               | Parameter type/usage | Description                                                  |
| ------------------------ | -------------------- | ------------------------------------------------------------ |
| --deep-scan              | --deep-scan          | Scan all sites in the file to build the contig file.         |
| --output<br />-o         | -o <file\>           | Set the output file.                                         |
| --from-contig<br />-from | -from <file\>        | Specify the corresponding contig file.                       |
| --to-contig<br />-to     | -to <file\>          | Reset contig (chromosome marker in each gtb block header) for gtb file directly. |
| --yes<br />-y            | -y                   | Overwrite output file without asking.                        |

#### 7.3. Use API in Java

```java
// load contig
ChromosomeInfo.load("./example/origin.contig");

// reset contig for a gtb file
GTBManager manager = GTBRootCache.get("./example/assoc.hg19.gtb");
manager.resetContig("./example/new.contig");
manager.toFile("./example/assoc.hg19.newcotig.gtb");
```

### 8. Calculate LD for GTB file —— operational mode for "ld"

Calculate LD for the **ordered GTB** file with the following command:

```shell
GBC ld <input> [options]
```

#### 8.1. Run the examples with the command line

```shell
# calculate the LD for 1000GP3 dataset
GBC ld ./example/1000GP3.phased.gtb --model --geno --window-bp 10000 --maf 0.1 --min-r2 0.2
GBC ld ./example/1000GP3.phased.gtb --model --hap
```

#### 8.2. List of parameters

**Output Options:**

| Parameters        | Parameter type/usage | Description                                          |
| ----------------- | --------------- | ------------------------------------------------------------ |
| --output<br />-o  | -o <file\>    | Set the output file. |
| --o-text          | --o-text        | Output LD file in text format. (this command will be executed automatically if '--o-bgz' is not passed in and the output file specified by '-o' is not end with '.gz') |
| --o-bgz           | --o-bgz         | Output LD file in bgz format. (this command will be executed automatically if '--o-text' is not passed in and the output file specified by '-o' is end with '.gz') |
| --level<br />-l   | -l <int, 0~9\> | Set the compression level. (Execute only if --o-bgz is passed in) (default: 5) |
| --threads<br />-t | -t <int, ≥1\>  | Set the number of threads. (default: 4)  |
| --yes<br />-y     | -y              | Overwrite output file without asking. |

**LD Calculation Options:**

| Parameters           | Parameter type/usage   | Description                                                  |
| -------------------- | ---------------------- | ------------------------------------------------------------ |
| --contig             | --contig <file\>     | Specify the corresponding contig file.     |
| --model<br />-m      | -m <string\>         | Calculate pairwise the linkage disequilibrium (hap, --hap, --hap-ld, --hap-r2) or genotypic correlation (geno, --geno, --geno-ld, --geno-r2). |
| --window-bp<br />-bp | -bp <int, ≥1\>        | The maximum number of physical bases between the variants being calculated for LD. (default: 10000) |
| --window-kb<br />-kb | -kb <int, ≥1\>        | The maximum number of physical bases between the variants being calculated for LD (1kb=1000bp). (default: 10000) |
| --min-r2             | --min-r2 <float, 0~1\> | Exclude pairs with R2 values less than --min-r2. (default: 0.2) |
| --maf                | --maf <float, 0~1\>   | Exclude variants with the minor allele frequency (MAF) per variant <maf. (default: 0.05) |

**Subset Selection Options:**

| Parameters        | Parameter type/usage                                         | Description                                                  |
| ----------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| --subject<br />-s | --subject <string\>,<string\>,…<br />--subject @<file\>      | Calculate the LD for the specified subjects. Subject name can be stored in a file with ',' delimited form, and pass in via '-s @file'. |
| --range<br />-r   | --range <chrom\><br />--range <chrom\>:<start\>-<br />--range <chrom\>:-<end\><br />--range <chrom\>:<start\>-<end\> | Calculate the LD by specified position range.                |

#### 8.3. Use API in Java

```java
LDTask task = new LDTask("./example/1000GP3.phased.gtb");
task.setWindowSizeBp(10000)
        .setMaf(0.1)
        .setMinR2(0.2)
        .setLdModel(ILDModel.GENOTYPE_LD);
task.submit();
```

### 9. Auxiliary tool

#### 9.1. Parallel BGZIP compression algorithm

GBC integrates a pure Java version of the parallel BGZIP compression tool, using with the following command:

```shell
GBC bgzip <mode> [options]
```

| Mode       | Description                                                  |
| ---------- | ------------------------------------------------------------ |
| compress   | Compression using parallel-bgzip (supported by CLM algorithm). |
| convert    | Convert *.gz format to *.bgz format.                         |
| decompress | Decompression.                                               |
| extract    | Cut the bgzip file by pointer range (decompressed file).     |
| concat     | Concatenate multiple files.                                  |
| md5        | Calculate a message-digest fingerprint (checksum) for decompressed file. |

#### 9.2. MD5 verification tool

Verify the MD5 code of the file with the following command:

```shell
GBC md5 <input(s)> [--o-md5]
```

The optional parameter `--o-md5` helps export md5 file to the location of input file.
