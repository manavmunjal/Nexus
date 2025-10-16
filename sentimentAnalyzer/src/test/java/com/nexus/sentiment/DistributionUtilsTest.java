package com.nexus.sentiment;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class DistributionUtilsTest {

    @Test
    void testSmooth() {
        Map<String, Double> distribution = new HashMap<>();
        distribution.put("positive", 0.5);
        distribution.put("negative", 0.3);
        distribution.put("neutral", 0.2);

        Map<String, Double> smoothed = DistributionUtils.smooth(distribution, 0.1);

        assertThat(smoothed).hasSize(3);
        
        // Check that sum is still 1.0
        double sum = smoothed.values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(sum).isCloseTo(1.0, within(0.0001));
        
        // Check that all values are positive
        smoothed.values().forEach(val -> assertThat(val).isGreaterThan(0.0));
    }

    @Test
    void testSmoothWithZeros() {
        Map<String, Double> distribution = new HashMap<>();
        distribution.put("positive", 0.0);
        distribution.put("negative", 0.0);
        distribution.put("neutral", 1.0);

        Map<String, Double> smoothed = DistributionUtils.smooth(distribution, 0.01);

        // After smoothing, no zeros should exist
        smoothed.values().forEach(val -> assertThat(val).isGreaterThan(0.0));
        
        double sum = smoothed.values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(sum).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void testKlDivergence() {
        Map<String, Double> p = new HashMap<>();
        p.put("positive", 0.5);
        p.put("negative", 0.3);
        p.put("neutral", 0.2);

        Map<String, Double> q = new HashMap<>();
        q.put("positive", 0.4);
        q.put("negative", 0.4);
        q.put("neutral", 0.2);

        double divergence = DistributionUtils.klDivergence(p, q);

        assertThat(divergence).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void testKlDivergenceIdenticalDistributions() {
        Map<String, Double> p = new HashMap<>();
        p.put("positive", 0.333);
        p.put("negative", 0.333);
        p.put("neutral", 0.334);

        double divergence = DistributionUtils.klDivergence(p, p);

        assertThat(divergence).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void testKlDivergenceMissingKeys() {
        Map<String, Double> p = new HashMap<>();
        p.put("positive", 0.5);
        p.put("negative", 0.5);

        Map<String, Double> q = new HashMap<>();
        q.put("positive", 0.5);
        q.put("neutral", 0.5);

        assertThatThrownBy(() -> DistributionUtils.klDivergence(p, q))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Distributions must cover identical labels");
    }

    @Test
    void testSymmetricKlDivergence() {
        Map<String, Double> p = new HashMap<>();
        p.put("positive", 0.6);
        p.put("negative", 0.3);
        p.put("neutral", 0.1);

        Map<String, Double> q = new HashMap<>();
        q.put("positive", 0.2);
        q.put("negative", 0.5);
        q.put("neutral", 0.3);

        double symDivergence = DistributionUtils.symmetricKlDivergence(p, q);
        double reverseSymDivergence = DistributionUtils.symmetricKlDivergence(q, p);

        assertThat(symDivergence).isEqualTo(reverseSymDivergence);
        assertThat(symDivergence).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void testSymmetricKlDivergenceIdentical() {
        Map<String, Double> p = new HashMap<>();
        p.put("positive", 0.5);
        p.put("negative", 0.3);
        p.put("neutral", 0.2);

        double divergence = DistributionUtils.symmetricKlDivergence(p, p);

        assertThat(divergence).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void testProportionsFromCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("positive", 50L);
        counts.put("negative", 30L);
        counts.put("neutral", 20L);

        Map<String, Double> proportions = DistributionUtils.proportionsFromCounts(counts);

        assertThat(proportions).hasSize(3);
        assertThat(proportions.get("positive")).isCloseTo(0.5, within(0.0001));
        assertThat(proportions.get("negative")).isCloseTo(0.3, within(0.0001));
        assertThat(proportions.get("neutral")).isCloseTo(0.2, within(0.0001));
    }

    @Test
    void testProportionsFromCountsZeroTotal() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("positive", 0L);
        counts.put("negative", 0L);
        counts.put("neutral", 0L);

        Map<String, Double> proportions = DistributionUtils.proportionsFromCounts(counts);

        assertThat(proportions).hasSize(3);
        proportions.values().forEach(val -> assertThat(val).isEqualTo(0.0));
    }

    @Test
    void testProportionsFromCountsSumToOne() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("positive", 100L);
        counts.put("negative", 150L);
        counts.put("neutral", 250L);

        Map<String, Double> proportions = DistributionUtils.proportionsFromCounts(counts);

        double sum = proportions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(sum).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void testSmoothPreservesOriginal() {
        Map<String, Double> original = new HashMap<>();
        original.put("positive", 0.5);
        original.put("negative", 0.5);

        DistributionUtils.smooth(original, 0.1);

        // Verify original is unchanged
        assertThat(original.get("positive")).isEqualTo(0.5);
        assertThat(original.get("negative")).isEqualTo(0.5);
    }
}
