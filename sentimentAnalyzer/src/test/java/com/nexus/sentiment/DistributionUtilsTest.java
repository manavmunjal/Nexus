package com.nexus.sentiment;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class DistributionUtilsTest {

  @Test
  /** 
   * Test the smoothing function with a standard distribution.
   **/
  void testSmooth() {
      Map<String, Double> distribution = new HashMap<>();
      distribution.put("positive", 0.5);
      distribution.put("negative", 0.3);
      distribution.put("neutral", 0.2);
      distribution.put("useless", 0.0);

      Map<String, Double> smoothed = DistributionUtils.smooth(distribution, 0.01);

      assertThat(smoothed).hasSize(distribution.size());
      
      // Check that sum is still 1.0
      double sum = smoothed.values().stream().mapToDouble(Double::doubleValue).sum();
      assertThat(sum).isCloseTo(1.0, within(0.0001));
      
      // Check that all values are positive
      smoothed.values().forEach(val -> assertThat(val).isGreaterThan(0.0));
  }

  @Test
  /* Test smoothing when some categories have multiple zero counts. */
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
  /**
   * Test the KL divergence calculation for simple distributions.
   **/
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
  /** 
   * Test the KL divergence calculation for identical distributions.
   * Should be able to get the same answer.
   **/
  void testKlDivergenceIdenticalDistributions() {
      Map<String, Double> p = new HashMap<>();
      p.put("positive", 0.333);
      p.put("negative", 0.333);
      p.put("neutral", 0.334);

      double divergence = DistributionUtils.klDivergence(p, p);

      assertThat(divergence).isCloseTo(0.0, within(0.0001));
  }

  @Test
  /**
   * KL divergence should throw an exception if distributions have different keys.
   **/
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
  void testKlDivergenceWithZeroInP() {
    Map<String, Double> p = new HashMap<>();
    p.put("a", 0.0);
    p.put("b", 1.0);

    Map<String, Double> q = new HashMap<>();
    q.put("a", 0.5);
    q.put("b", 0.5);

    double div = DistributionUtils.klDivergence(p, q);

    assertThat(div).isGreaterThanOrEqualTo(0.0);
  }

  @Test
  void testKlDivergenceQHasZeroProbability() {
    Map<String, Double> p = new HashMap<>();
    p.put("label", 1.0);

    Map<String, Double> q = new HashMap<>();
    q.put("label", 0.0);

    double div = DistributionUtils.klDivergence(p, q);

    assertThat(div).isEqualTo(Double.POSITIVE_INFINITY);
  }

  @Test
  /**
   * Test the KL divergence calculation for symmetric distributions.
   **/
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
  /**
   * Symmetric KL divergence of identical distributions should be zero.
   **/
  void testSymmetricKlDivergenceIdentical() {
      Map<String, Double> p = new HashMap<>();
      p.put("positive", 0.5);
      p.put("negative", 0.3);
      p.put("neutral", 0.2);

      double divergence = DistributionUtils.symmetricKlDivergence(p, p);

      assertThat(divergence).isCloseTo(0.0, within(0.0001));
  }

  @Test
  void testSymmetricKlDivergenceMissingKeys() {
      Map<String, Double> p = new HashMap<>();
      p.put("x", 1.0);

      Map<String, Double> q = new HashMap<>();
      q.put("y", 1.0);

      assertThatThrownBy(() -> DistributionUtils.symmetricKlDivergence(p, q))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("identical labels");
  }

  @Test
  /**
   * Test the calculation of proportions from counts.
   **/
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
  /**
   * Test the calculation of proportions from counts.
   **/
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
  /**
   * Test that proportions sum to 1.0.
   **/
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
  void testProportionsFromCountsEmptyMap() {
      Map<String, Long> counts = new HashMap<>();

      Map<String, Double> proportions = DistributionUtils.proportionsFromCounts(counts);

      assertThat(proportions).isEmpty();
  }

  @Test
  void testProportionsFromCountsSingleLabel() {
      Map<String, Long> counts = new HashMap<>();
      counts.put("only", 42L);

      Map<String, Double> proportions = DistributionUtils.proportionsFromCounts(counts);

      assertThat(proportions.get("only")).isEqualTo(1.0);
  }

  @Test
  /**
   * Test that smoothing does not modify the original distribution.
   **/
  void testSmoothPreservesOriginal() {
      Map<String, Double> original = new HashMap<>();
      original.put("positive", 0.5);
      original.put("negative", 0.5);

      DistributionUtils.smooth(original, 0.1);

      // Verify original is unchanged
      assertThat(original.get("positive")).isEqualTo(0.5);
      assertThat(original.get("negative")).isEqualTo(0.5);
  }

  @Test
  void testSmoothEmptyDistribution() {
      Map<String, Double> distribution = new HashMap<>();
      Map<String, Double> smoothed = DistributionUtils.smooth(distribution, 0.1);
      assertThat(smoothed).isEmpty();
  }

  @Test
  void testSmoothAllZeroDistribution() {
    Map<String, Double> distribution = new HashMap<>();
    distribution.put("pos", 0.0);
    distribution.put("neg", 0.0);

    Map<String, Double> smoothed = DistributionUtils.smooth(distribution, 0.01);

    smoothed.values().forEach(v -> assertThat(v).isGreaterThan(0.0));
  }

  @Test
  void testSmoothWithZeroEpsilon() {
    Map<String, Double> distribution = new HashMap<>();
    distribution.put("positive", 0.5);
    distribution.put("negative", 0.5);

    Map<String, Double> smoothed = DistributionUtils.smooth(distribution, 0.0);

    assertThat(smoothed.get("positive")).isCloseTo(0.5, within(1e-9));
    assertThat(smoothed.get("negative")).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testSmoothWithNegativeEpsilon() {
    Map<String, Double> distribution = new HashMap<>();
    distribution.put("positive", 0.5);
    distribution.put("negative", 0.5);

    Map<String, Double> smoothed = DistributionUtils.smooth(distribution, -0.1);

    double sum = smoothed.values().stream().mapToDouble(Double::doubleValue).sum();
    assertThat(sum).isCloseTo(1.0, within(0.0001));
  }

  @Test
  void testSmoothSingleLabelDistribution() {
    Map<String, Double> distribution = new HashMap<>();
    distribution.put("only", 1.0);

    Map<String, Double> smoothed = DistributionUtils.smooth(distribution, 0.01);

    assertThat(smoothed.get("only")).isEqualTo(1.0);
  }
}
