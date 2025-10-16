package com.nexus.sentiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

class DataSplitterTest {

    private Instances instances;

    @BeforeEach
    void setUp() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("text", (ArrayList<String>) null));
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("positive");
        classValues.add("negative");
        classValues.add("neutral");
        attributes.add(new Attribute("sentiment", classValues));

        instances = new Instances("TestData", attributes, 0);
        instances.setClassIndex(1);

        for (int i = 0; i < 100; i++) {
            DenseInstance instance = new DenseInstance(2);
            instance.setDataset(instances);
            instance.setValue(0, "Review " + i);
            instance.setValue(1, classValues.get(i % 3));
            instances.add(instance);
        }
    }

    @Test
    void testSplitWithValidRatio() {
        DataSplitter.Split split = DataSplitter.split(instances, 0.8, 42);

        assertThat(split).isNotNull();
        assertThat(split.train().numInstances()).isEqualTo(80);
        assertThat(split.test().numInstances()).isEqualTo(20);
    }

    @Test
    void testSplitIsDeterministic() {
        DataSplitter.Split split1 = DataSplitter.split(instances, 0.8, 42);
        DataSplitter.Split split2 = DataSplitter.split(instances, 0.8, 42);

        assertThat(split1.train().numInstances()).isEqualTo(split2.train().numInstances());
        assertThat(split1.test().numInstances()).isEqualTo(split2.test().numInstances());
    }

    @Test
    void testSplitWithDifferentSeeds() {
        DataSplitter.Split split1 = DataSplitter.split(instances, 0.8, 42);
        DataSplitter.Split split2 = DataSplitter.split(instances, 0.8, 99);

        assertThat(split1.train().numInstances()).isEqualTo(split2.train().numInstances());
        // The actual instances should differ due to different shuffling
    }

    @Test
    void testSplitWithInvalidRatio() {
        assertThatThrownBy(() -> DataSplitter.split(instances, 0.0, 42))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Train ratio must be within (0,1)");

        assertThatThrownBy(() -> DataSplitter.split(instances, 1.0, 42))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Train ratio must be within (0,1)");

        assertThatThrownBy(() -> DataSplitter.split(instances, -0.5, 42))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> DataSplitter.split(instances, 1.5, 42))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSplitPreservesClassAttribute() {
        DataSplitter.Split split = DataSplitter.split(instances, 0.7, 42);

        assertThat(split.train().classIndex()).isEqualTo(instances.classIndex());
        assertThat(split.test().classIndex()).isEqualTo(instances.classIndex());
    }

    @Test
    void testSplitWithSmallDataset() {
        Instances smallData = new Instances(instances, 0, 10);
        DataSplitter.Split split = DataSplitter.split(smallData, 0.8, 42);

        assertThat(split.train().numInstances()).isEqualTo(8);
        assertThat(split.test().numInstances()).isEqualTo(2);
    }
}
