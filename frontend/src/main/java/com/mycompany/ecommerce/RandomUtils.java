package com.mycompany.ecommerce;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.PrimitiveIterator;
import java.util.stream.DoubleStream;

public class RandomUtils {

    public static PrimitiveIterator.OfDouble positiveDoubleGaussianDistribution(double mean, double standardDerivation) {
        final NormalDistribution normalDistribution = new NormalDistribution(mean, standardDerivation);
        return DoubleStream.generate(() -> Math.max(normalDistribution.sample(), 0)).iterator();
    }
}
