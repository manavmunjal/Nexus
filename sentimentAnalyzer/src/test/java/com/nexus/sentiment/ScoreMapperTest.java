package com.nexus.sentiment;

import org.junit.jupiter.api.Test;
import weka.core.Attribute;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

class ScoreMapperTest {

    @Test
    /**
     * Test standard sentiment labels mapping.
     */
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
    /** 
     * Test case insensitive mapping.
     */
    void testCaseInsensitiveMapping() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("PosiTive");
        classValues.add("NegAtive");
        classValues.add("NeutraL");
        Attribute classAttribute = new Attribute("sentiment", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);

        assertThat(mapper.scoreFor("PositivE")).isEqualTo(1.0);
        assertThat(mapper.scoreFor("NegatiVe")).isEqualTo(-1.0);
        assertThat(mapper.scoreFor("NeUtral")).isEqualTo(0.0);
    }

    @Test
    /**
     * Default labels for sentiments are very positive, somewhat positive, neutral,
     * somewhat negative, very negative. If custom labels are provided, they would be 
     * ranked mapped from -1.0 to 1.0 in equal intervals. 
     * Note: the custom labels should be added in the order of sentiment from negative
     * to positive. The score mapper DOES NOT sort the added custom labels.
     */
    void testCustomLabels() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("happy");
        classValues.add("sad");
        Attribute classAttribute = new Attribute("emotion", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);

        // Custom labels should be mapped to a range
        assertThat(mapper.scoreFor("happy")).isBetween(-1.0, 1.0);
        assertThat(mapper.scoreFor("sad")).isBetween(-1.0, 1.0);
        assertThat(mapper.scoreFor("happy")).isLessThan(mapper.scoreFor("sad"));
    }

    @Test
    /** 
     * Test any unrecognized string input argument for scoreMapper methods, such as unknown label.
     * Unknown labels should return a default score of 0.0.
     **/
    void testUnknownLabelReturnsDefault() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("positive");
        classValues.add("negative");
        Attribute classAttribute = new Attribute("sentiment", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);

        assertThat(mapper.scoreFor("unknown")).isEqualTo(0.0);
    }

    @Test
    /**
     * Test that allScores method returns an immutable copy of the score mapping.
     * This ensures that the external code can not modify the model inferenced scores.
     */
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
    /**
     * Test edge case where there is only one label in the class attribute.
     */
    void testSingleLabelEdgeCase() {
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("positive");
        Attribute classAttribute = new Attribute("sentiment", classValues);

        ScoreMapper mapper = ScoreMapper.fromAttribute(classAttribute);

        assertThat(mapper.scoreFor("positive")).isEqualTo(1.0);
        assertThat(mapper.allScores()).hasSize(1);
    }
}
