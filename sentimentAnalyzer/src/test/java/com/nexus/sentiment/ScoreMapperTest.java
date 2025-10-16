package com.nexus.sentiment;

import org.junit.jupiter.api.Test;
import weka.core.Attribute;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

class ScoreMapperTest {

    @Test
    void testStandardSentimentLabels() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("positive");
        classValues.add("negative");
        classValues.add("neutral");
        Attribute classAttribute = new Attribute("sentiment", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);

        assertThat(mapper.scoreFor("positive")).isEqualTo(1.0);
        assertThat(mapper.scoreFor("negative")).isEqualTo(-1.0);
        assertThat(mapper.scoreFor("neutral")).isEqualTo(0.0);
    }

    @Test
    void testCaseInsensitiveMapping() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("Positive");
        classValues.add("Negative");
        classValues.add("Neutral");
        Attribute classAttribute = new Attribute("sentiment", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);

        assertThat(mapper.scoreFor("Positive")).isEqualTo(1.0);
        assertThat(mapper.scoreFor("Negative")).isEqualTo(-1.0);
        assertThat(mapper.scoreFor("Neutral")).isEqualTo(0.0);
    }

    @Test
    void testCustomLabels() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("happy");
        classValues.add("sad");
        Attribute classAttribute = new Attribute("emotion", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);

        // Custom labels should be mapped to a range
        assertThat(mapper.scoreFor("happy")).isBetween(-1.0, 1.0);
        assertThat(mapper.scoreFor("sad")).isBetween(-1.0, 1.0);
    }

    @Test
    void testUnknownLabelReturnsDefault() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("positive");
        classValues.add("negative");
        Attribute classAttribute = new Attribute("sentiment", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);

        assertThat(mapper.scoreFor("unknown")).isEqualTo(0.0);
    }

    @Test
    void testAllScoresReturnsImmutableCopy() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("positive");
        classValues.add("negative");
        classValues.add("neutral");
        Attribute classAttribute = new Attribute("sentiment", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);
        var scores = mapper.allScores();

        assertThat(scores).hasSize(3);
        assertThat(scores).containsKeys("positive", "negative", "neutral");
        
        // Verify immutability
        assertThatThrownBy(() -> scores.put("new", 0.5))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testMultipleLabelsDifferentScores() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("very_positive");
        classValues.add("positive");
        classValues.add("neutral");
        classValues.add("negative");
        classValues.add("very_negative");
        Attribute classAttribute = new Attribute("sentiment", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);

        var allScores = mapper.allScores();
        assertThat(allScores).hasSize(5);
        
        // Check that scores are distributed across the range
        double minScore = allScores.values().stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxScore = allScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        
        assertThat(minScore).isLessThan(maxScore);
    }

    @Test
    void testSingleLabelEdgeCase() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("positive");
        Attribute classAttribute = new Attribute("sentiment", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);

        assertThat(mapper.scoreFor("positive")).isEqualTo(1.0);
        assertThat(mapper.allScores()).hasSize(1);
    }
}
