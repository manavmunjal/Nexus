package com.nexus.sentiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Comprehensive tests for SentimentLabelConverter.
 * Tests conversion from 5-class to 3-class sentiment labels.
 */
class SentimentLabelConverterTest {

  private Instances fiveClassData;
  private Instances threeClassData;

  @BeforeEach
  void setUp() {
      // Setup 5-class dataset
      ArrayList<Attribute> attributes = new ArrayList<>();
      attributes.add(new Attribute("review_text", (ArrayList<String>) null));

      List<String> fiveClassValues = new ArrayList<>();
      fiveClassValues.add("very negative");
      fiveClassValues.add("somewhat negative");
      fiveClassValues.add("neutral");
      fiveClassValues.add("somewhat positive");
      fiveClassValues.add("very positive");
      attributes.add(new Attribute("sentiment", fiveClassValues));

      fiveClassData = new Instances("5ClassSentiment", attributes, 0);
      fiveClassData.setClassIndex(1);

      // Setup 3-class dataset
      ArrayList<Attribute> threeClassAttr = new ArrayList<>();
      threeClassAttr.add(new Attribute("review_text", (ArrayList<String>) null));

      List<String> threeClassValues = new ArrayList<>();
      threeClassValues.add("negative");
      threeClassValues.add("neutral");
      threeClassValues.add("positive");
      threeClassAttr.add(new Attribute("sentiment", threeClassValues));

      threeClassData = new Instances("3ClassSentiment", threeClassAttr, 0);
      threeClassData.setClassIndex(1);
  }

  @Test
  /**
   * Test conversion from 5-class to 3-class sentiment labels.
   * @throws Exception
   */
  void testConvertFrom5ClassTo3Class() throws Exception {
      // Add 5-class instances
      addInstance(fiveClassData, "Very bad experience!", "very negative");
      addInstance(fiveClassData, "Bad product quality", "somewhat negative");
      addInstance(fiveClassData, "It's okay", "neutral");
      addInstance(fiveClassData, "Good product", "somewhat positive");
      addInstance(fiveClassData, "Excellent product!", "very positive");

      Instances converted = SentimentLabelConverter.convertTo3Class(fiveClassData, "sentiment");

      assertThat(converted).isNotNull();
      assertThat(converted.numInstances()).isEqualTo(5);
      assertThat(converted.classAttribute().numValues()).isEqualTo(3);
      assertThat(converted.classAttribute().value(0)).isEqualTo("negative");
      assertThat(converted.classAttribute().value(1)).isEqualTo("neutral");
      assertThat(converted.classAttribute().value(2)).isEqualTo("positive");
  }

  @Test
  /**
   * Test conversion from 3-class to 3-class sentiment labels.
   * Should not need change anything.
   * @throws Exception
   */
  void testConvertFrom3Class() throws Exception {
      // Add 3-class instances
      addInstance(threeClassData, "Bad experience", "negative");
      addInstance(threeClassData, "It's okay", "neutral");
      addInstance(threeClassData, "Great product", "positive");

      Instances converted = SentimentLabelConverter.convertTo3Class(threeClassData, "sentiment");

      assertThat(converted).isNotNull();
      assertThat(converted.numInstances()).isEqualTo(3);
      assertThat(converted.classAttribute().numValues()).isEqualTo(3);
      assertThat(converted.classAttribute().value(0)).isEqualTo("negative");
      assertThat(converted.classAttribute().value(1)).isEqualTo("neutral");
      assertThat(converted.classAttribute().value(2)).isEqualTo("positive");
  }

  @Test
  void testIllFormattedClasses() throws Exception {
      // Add instances with unrecognized labels
      assertThatThrownBy(() -> addInstance(fiveClassData, "Bad experience", "extremely negative"))
              .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  /**
   * Test conversion when class attribute is not set.
   * Should throw IllegalArgumentException.
   * @throws Exception
   */
  void testNonSetClassAttribute() {
      Instances data = new Instances(fiveClassData);
      data.setClassIndex(-1);  // Unset class attribute

      assertThatThrownBy(() -> SentimentLabelConverter.convertTo3Class(data, "sentiment"))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Class attribute not set");
  }


  private void addInstance(Instances data, String text, String label) throws Exception {
      DenseInstance instance = new DenseInstance(2);
      instance.setDataset(data);
      instance.setValue(0, text);
      // Check if the label is a valid value in the sentiment class attribute
      Attribute sentimentAttr = data.attribute("sentiment");
      if (sentimentAttr == null || sentimentAttr.indexOfValue(label) == -1) {
          throw new IllegalArgumentException("Unrecognized label: " + label);
      }
      instance.setValue(1, label);
      data.add(instance);
  }

  @Test
  void testEmptyDataset() throws Exception {
      Instances emptyData = new Instances(fiveClassData, 0);  // No instances, just attributes

      assertThatThrownBy(() -> SentimentLabelConverter.convertTo3Class(emptyData, "sentiment"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("No instances available to convert");
  }

  @Test
  void testSingleInstanceDataset() throws Exception {
      Instances singleInstanceData = new Instances(fiveClassData, 0);
      Instance inst = new DenseInstance(singleInstanceData.numAttributes());
      inst.setDataset(singleInstanceData);
      inst.setValue(singleInstanceData.classAttribute(), singleInstanceData.classAttribute().value(0));
      singleInstanceData.add(inst);

      // Should not throw, should convert to 3-class
      Instances converted = SentimentLabelConverter.convertTo3Class(singleInstanceData, "sentiment");
      assertNotNull(converted);
      assertEquals(1, converted.numInstances());
  }

  @Test
  void testEmptyClassAttribute() throws Exception {
      // Create a class attribute with no valid values
      List<String> emptyClassValues = new ArrayList<>();
      Attribute emptyClassAttr = new Attribute("sentiment", emptyClassValues);

      ArrayList<Attribute> attributes = new ArrayList<>();
      attributes.add(emptyClassAttr);

      Instances emptyClassData = new Instances("EmptyClassData", attributes, 1);
      emptyClassData.setClassIndex(0);  // Set the class index to the empty class attribute

      Instance instance = new DenseInstance(emptyClassData.numAttributes());
      instance.setDataset(emptyClassData); // link instance to dataset
      emptyClassData.add(instance);

      assertThatThrownBy(() -> SentimentLabelConverter.convertTo3Class(emptyClassData, "sentiment"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("No valid labels provided");
}

  @Test
  void testUnexpectedClassValues() throws Exception {
      // Create class values with unrecognized labels
      List<String> classValues = new ArrayList<>();
      classValues.add("unknown positive");
      classValues.add("unknown negative");
    
      // Create a custom class attribute with the unrecognized values
      Attribute customClassAttr = new Attribute("sentiment", classValues);
      ArrayList<Attribute> attributes = new ArrayList<>();
      attributes.add(customClassAttr);
    
      Instances customData = new Instances("CustomData", attributes, 1);
      customData.setClassIndex(0);
    
      Instance instance = new DenseInstance(1);
      instance.setValue(customClassAttr, "unknown positive");  // Set unrecognized label
      customData.add(instance);

      // Run the test expecting an IllegalArgumentException with the correct message
      assertThatThrownBy(() -> SentimentLabelConverter.convertTo3Class(customData, "sentiment"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unrecognized sentiment label");
  }
}
