  - ## II. Environment configuration and software installation

    ### 1. Download GBC

    | Type                        | File                                              |
    | --------------------------- | ------------------------------------------------- |
    | Package                     | [gbc-1.1.jar](../download/gbc-1.1.jar)            |
    | Source codes                | [gbc-source.zip](../download/gbc-source.zip)      |
    | Example                     | [Instance](../download/example.zip)               |
    | Docker Images (Docker File) | [Dockerfile](../download/linux_x64_86.dockerfile) |

    ### 2. System requirements

    GBC is developed based on Oracle JDK 8. It is available on any computer device that supports or is compatible with Oracle JDK 8. Users are required to download and install the [Oracle JDK](https://www.oracle.com/cn/java/technologies/javase-downloads.html) or [Open JDK](https://openjdk.java.net/install/) firstly.

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
    
    - API tools: Use by importing the gbc.jar package.
