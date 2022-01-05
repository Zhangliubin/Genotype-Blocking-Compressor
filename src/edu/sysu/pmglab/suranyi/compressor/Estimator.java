package edu.sysu.pmglab.suranyi.compressor;

import edu.sysu.pmglab.suranyi.container.Pair;
import edu.sysu.pmglab.suranyi.container.Range;
import edu.sysu.pmglab.suranyi.container.VolumeByteStream;
import edu.sysu.pmglab.suranyi.check.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author suranyi
 * @description 压缩边界估计器
 */

public class Estimator {
    final VolumeByteStream outputCache = new VolumeByteStream(256 * 1024 * 1024);
    final Random random = new Random();
    final ICompressor compressor;
    final ArrayList<Pair<Number, Number>> data = new ArrayList<>(1024);

    public Estimator(int compressorIndex) {
        this(compressorIndex, ICompressor.getDefaultCompressionLevel(compressorIndex));
    }

    public Estimator(int compressorIndex, int compressionLevel) {
        this(ICompressor.getInstance(compressorIndex, compressionLevel));
    }

    public Estimator(String compressorName) {
        this(ICompressor.getCompressorIndex(compressorName));
    }

    public Estimator(String compressorName, int compressionLevel) {
        this(ICompressor.getCompressorIndex(compressorName), compressionLevel);
    }

    public Estimator(ICompressor compressor) {
        this.compressor = compressor;
    }

    /**
     * 设置随机种子
     * @param seed 随机种子 (用于复现结果)
     */
    public void setRandomSeed(long seed) {
        this.random.setSeed(seed);
    }

    /**
     * 添加自定义规模的训练数据集
     * @param range 范围编码器
     * @throws IOException IO 异常
     */
    public <T extends Number & Comparable<? super T>> void addTrainingSets(Range<T> range) throws IOException {
        synchronized (this.compressor) {
            VolumeByteStream input = generateRandomData(range.end.intValue());
            while (range.hasNext()) {
                T currentSize = range.getNext();
                outputCache.reset();
                int size = this.compressor.compress(input.getCache(), 0, currentSize.intValue(), outputCache);
                this.data.add(new Pair<>(currentSize, size));
            }
        }
    }

    /**
     * 使用网格搜索进行最优化
     * @param parameters 范围编码器
     * @return 最优模型结果
     */
    @SafeVarargs
    public final <T extends Number & Comparable<? super T>> Model<T> optimize(Range<T>... parameters) {
        Assert.that(parameters.length >= 2);

        Model<T> optimizeValue = null;
        double loss = Double.MAX_VALUE;

        while (parameters[0].hasNext()) {
            T currentAlpha = parameters[0].getNext();
            parameters[1].reset();
            while (parameters[1].hasNext()) {
                Model<T> model = new Model<>(currentAlpha, parameters[1].getNext());
                double modelLoss = lossFunction(model);
                if (modelLoss < loss) {
                    optimizeValue = model;
                    loss = modelLoss;
                }
            }
        }

        return optimizeValue;
    }

    public Model<Double> optimize() {
        return optimize(new Range<>(1.00, 1.05, 0.0001), new Range<>(512.0, 8192.0, 512.0));
    }

    public <T extends Number & Comparable<? super T>> double lossFunction(Model<T> model) {
        double loss = 0;
        for (Pair<Number, Number> pair : this.data) {
            loss += (model.fit(pair.key.intValue()) < pair.value.doubleValue() ? 1 : 0);
        }
        return loss + model.parameters[0].doubleValue() + (model.parameters[1].doubleValue() / (1024 * 1024 * 10));
    }

    public VolumeByteStream generateRandomData(int length) {
        VolumeByteStream vbs = new VolumeByteStream(length);
        synchronized (random) {
            random.nextBytes(vbs.getCache());
        }
        vbs.reset(length);
        return vbs;
    }

    public static class Model<T extends Number> {
        private final T[] parameters;

        @SafeVarargs
        Model(T... parameters) {
            this.parameters = parameters;
        }

        public double fit(int length) {
            Assert.that(length >= 0 && length <= Integer.MAX_VALUE - 2);

            return (Math.max(parameters[0].doubleValue() * length, parameters[1].doubleValue()));
        }

        @Override
        public String toString() {
            return String.format("Model: %.6f * l + %d", parameters[0].floatValue(), parameters[1].intValue());
        }
    }

    public static void main(String[] args) throws IOException {
        // 创建估计器。待估计的压缩器需要继承 ICompressor，并在 ICompressor 中注册方法。
        for (int i = 0; i < 23; i++) {
            Estimator estimator = new Estimator("ZSTD", i);
            // 设置随机种子
            estimator.setRandomSeed(0);
            // 小规模数据集 (1 byte 递增)
            estimator.addTrainingSets(new Range<>(0, 8192, 1));
            // 中规模数据集 (1 KB 递增)
            estimator.addTrainingSets(new Range<>(8192, 1024 * 1024, 1024));
            // 大规模数据集 (1 MB 递增)
            estimator.addTrainingSets(new Range<>(1024 * 1024, 64 * 1024 * 1024, 1024 * 1024));
            // 执行最优化
            Model<Double> optimize = estimator.optimize();
            // 打印结果 (optimize 中可以传入自定义的参数范围)
            System.out.println(estimator.compressor.getClass() + " level " + i + "\n - Estimate " + optimize + ", loss = " + estimator.lossFunction(optimize));
        }

        for (int i = 0; i < 10; i++) {
            Estimator estimator = new Estimator("LZMA", i);
            // 设置随机种子
            estimator.setRandomSeed(0);
            // 小规模数据集 (1 byte 递增)
            estimator.addTrainingSets(new Range<>(0, 8192, 1));
            // 中规模数据集 (1 KB 递增)
            estimator.addTrainingSets(new Range<>(8192, 1024 * 1024, 1024));
            // 大规模数据集 (1 MB 递增)
            estimator.addTrainingSets(new Range<>(1024 * 1024, 64 * 1024 * 1024, 1024 * 1024));
            // 执行最优化
            Model<Double> optimize = estimator.optimize();
            // 打印结果 (optimize 中可以传入自定义的参数范围)
            System.out.println(estimator.compressor.getClass() + " level " + i + "\n - Estimate " + optimize + ", loss = " + estimator.lossFunction(optimize));
        }
    }
}