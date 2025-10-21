package com.nexus.sentiment;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instances;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainMockitoTest {

    private Main.Config config;

    @BeforeEach
    void setup() {
        // Initialize default config before each test
        config = new Main.Config();
        config.datasetPath = "dummy/path.csv";
        config.textAttribute = "review_text";
        config.classAttribute = "sentiment_label";
        config.trainRatio = 0.8;
        config.seed = 42L;
        config.epsilon = 1e-6;
        config.sampleLimit = 5;
        config.showHelp = false;
    }

    @Test
    void testRunAnalysis_fileNotExists_throws() {
        // Mock Files.exists() to simulate that dataset file does NOT exist
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(Path.of(config.datasetPath))).thenReturn(false);

            // Expect exception with error message
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                Main.runAnalysis(config);
            });
            assertTrue(ex.getMessage().contains("Dataset not found"));
        }
    }

    @Test
    void testRunAnalysis_classAttributeNull_throws() throws Exception {
        // Test case where the dataset loads but class attribute is missing (null)
        try (MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<DatasetLoader> datasetLoaderMock = mockStatic(DatasetLoader.class);
             MockedStatic<SentimentLabelConverter> converterMock = mockStatic(SentimentLabelConverter.class)) {

            // Simulate that dataset file exists
            filesMock.when(() -> Files.exists(Path.of(config.datasetPath))).thenReturn(true);

            // Mock Instances object returned by DatasetLoader.load()
            Instances mockedInstances = mock(Instances.class);

            // Simulate that class attribute is missing by returning null
            when(mockedInstances.attribute(config.classAttribute)).thenReturn(null);

            // Mock DatasetLoader.load() to return our mocked Instances
            datasetLoaderMock.when(() -> DatasetLoader.load(any(Path.class), anyString())).thenReturn(mockedInstances);

            // Mock SentimentLabelConverter.convertTo3Class() to return mocked Instances as is
            converterMock.when(() -> SentimentLabelConverter.convertTo3Class(any(), anyString())).thenReturn(mockedInstances);

            // Expect exception with error message
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                Main.runAnalysis(config);
            });
            assertTrue(ex.getMessage().contains("Class attribute not found"));
        }
    }

    @Test
    void testParseArgs_setsAllFieldsCorrectly() {
        // Provide args
        String[] args = {
                "--dataset=path/to/data.csv",
                "--text-attr=my_text",
                "--class-attr=my_class",
                "--train-ratio=0.75",
                "--seed=12345",
                "--epsilon=0.0001",
                "--limit=7"
        };
        // Parse args into Config object
        Main.Config parsedConfig = Main.parseArgs(args);

        // Check that each field in Config matches expected value from args
        assertEquals("path/to/data.csv", parsedConfig.datasetPath);
        assertEquals("my_text", parsedConfig.textAttribute);
        assertEquals("my_class", parsedConfig.classAttribute);
        assertEquals(0.75, parsedConfig.trainRatio);
        assertEquals(12345L, parsedConfig.seed);
        assertEquals(0.0001, parsedConfig.epsilon);
        assertEquals(7, parsedConfig.sampleLimit);
        assertFalse(parsedConfig.showHelp); // No help flag present, so should be false
    }

    @Test
    void testParseArgs_helpFlagSetsShowHelp() {
        // Test that the --help flag sets showHelp to true
        String[] args = {"--help"};
        Main.Config parsedConfig = Main.parseArgs(args);
        assertTrue(parsedConfig.showHelp);

        // Also test short flag -h
        parsedConfig = Main.parseArgs(new String[]{"-h"});
        assertTrue(parsedConfig.showHelp);
    }

    @Test
    void testGroupBy_groupsCorrectly() {
        // Create mock PredictionResult objects with product keys
        PredictionResult pr1 = mock(PredictionResult.class);
        when(pr1.product()).thenReturn("prodA");

        PredictionResult pr2 = mock(PredictionResult.class);
        when(pr2.product()).thenReturn("prodB");

        PredictionResult pr3 = mock(PredictionResult.class);
        when(pr3.product()).thenReturn("prodA");

        List<PredictionResult> list = List.of(pr1, pr2, pr3);

        // Group by product with fallback "UNKNOWN"
        Map<String, List<PredictionResult>> grouped = Main.groupBy(list, PredictionResult::product, "UNKNOWN");

        // Assert grouping is correct - two keys and correct counts per key
        assertEquals(2, grouped.size());
        assertTrue(grouped.containsKey("prodA"));
        assertTrue(grouped.containsKey("prodB"));
        assertEquals(2, grouped.get("prodA").size());
        assertEquals(1, grouped.get("prodB").size());
    }

    @Test
    void testGroupBy_usesFallbackWhenKeyIsNullOrBlank() {
        // Mock PredictionResults with null, empty, or whitespace keys to test fallback usage
        PredictionResult pr1 = mock(PredictionResult.class);
        when(pr1.product()).thenReturn(null);

        PredictionResult pr2 = mock(PredictionResult.class);
        when(pr2.product()).thenReturn("");

        PredictionResult pr3 = mock(PredictionResult.class);
        when(pr3.product()).thenReturn("  ");

        List<PredictionResult> list = List.of(pr1, pr2, pr3);

        // Group by product with fallback key "FALLBACK"
        Map<String, List<PredictionResult>> grouped = Main.groupBy(list, PredictionResult::product, "FALLBACK");

        // All should be grouped under fallback key because keys are null or blank
        assertEquals(1, grouped.size());
        assertTrue(grouped.containsKey("FALLBACK"));
        assertEquals(3, grouped.get("FALLBACK").size());
    }

    @Test
    void testRunAnalysis_showHelp_onlyPrintsHelp() throws Exception {
        // When showHelp is true, runAnalysis should just print help and exit without error
        Main.Config helpConfig = new Main.Config();
        helpConfig.showHelp = true;

        // No exception expected
        Main.runAnalysis(helpConfig);
    }
}
