package edu.sysu.pmglab.suranyi.gbc.core;

import edu.sysu.pmglab.suranyi.check.Assert;
import edu.sysu.pmglab.suranyi.check.Value;
import edu.sysu.pmglab.suranyi.easytools.ValueUtils;

/**
 * @author suranyi
 * @description 任务接口, 并行是 GBC 及相关研究的重点特色
 */

public interface ITask {
    /**
     * 最大并行线程数
     */
    int AVAILABLE_PROCESSORS = ValueUtils.max(1, Runtime.getRuntime().availableProcessors());
    int INIT_THREADS = Value.of(4, 1, AVAILABLE_PROCESSORS);
    int MIN_THREADS = 1;

    /**
     * 设置并行线程数
     * @param threads 并行线程数
     * @return 任务本类
     */
    default ITask setThreads(int threads) {
        return setParallel(threads);
    }

    /**
     * 设置并行线程数
     * @param threads 并行线程数
     * @return 任务本类
     */
    ITask setParallel(int threads);

    /**
     * 添加线程数
     * @param threads 要添加的并行线程数
     * @return 任务本类
     */
    default ITask addThreads(int threads) {
        Assert.that(threads >= 0);

        if (threads > 0) {
            return setThreads(getThreads() + threads);
        }

        return this;
    }

    /**
     * 减少线程数
     * @param threads 要减少的并行线程数
     * @return 任务本类
     */
    default ITask reduceThreads(int threads) {
        Assert.that(threads >= 0);

        if (threads > 0) {
            return setThreads(getThreads() - threads);
        }

        return this;
    }

    /**
     * 获取线程数
     * @return 线程数
     */
    int getThreads();

    /**
     * 设置输出文件名
     * @param outputFileName 输出文件名
     * @return 任务本类
     */
    ITask setOutputFileName(String outputFileName);

    /**
     * 获取输出文件名
     * @return 输出文件名
     */
    String getOutputFileName();

    /**
     * 自动生成输出文件名
     * @return 文件名
     */
    String autoGenerateOutputFileName();
}
