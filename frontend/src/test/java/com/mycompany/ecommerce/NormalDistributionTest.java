package com.mycompany.ecommerce;

import org.junit.Test;

import java.util.PrimitiveIterator;

public class NormalDistributionTest {

    @Test
    public void testNormalDistribution() {
        PrimitiveIterator.OfDouble distribution = RandomUtils.positiveDoubleGaussianDistribution(1, 0.3);
        for (int i = 0; i < 50; i++) {
            System.out.println(distribution.nextDouble());
        }
    }
}
