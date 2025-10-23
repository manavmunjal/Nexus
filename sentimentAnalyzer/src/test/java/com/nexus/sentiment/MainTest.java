package com.nexus.sentiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import weka.core.Instances;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainTest {

    private Main.Config config;

    @BeforeEach
    void setup() {
        // Initialize default config before each test
        config = new Main.Config();
        config.setDatasetPath("dummy/path.csv");
        config.setTextAttribute("review_text");
        config.setClassAttribute("sentiment_label");
        config.setTrainRatio(0.8);
        config.setSeed(42L);
        config.setEpsilon(1e-6);
        config.setSampleLimit(5);
        config.setShowHelp(false);
    }

    @Test
    void runAnalysisFileNotExistsThrows() {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(Path.of(config.getDatasetPath()))).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                Main.runAnalysis(config);
            });
            assertTrue(ex.getMessage().contains("Dataset not found"));
        }
    }

    @Test
    void runAnalysisClassAttributeNullThrows() throws Exception {
        try (MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<DatasetLoader> datasetLoaderMock = mockStatic(DatasetLoader.class);
             MockedStatic<SentimentLabelConverter> converterMock = mockStatic(SentimentLabelConverter.class)) {

            filesMock.when(() -> Files.exists(Path.of(config.getDatasetPath()))).thenReturn(true);

            Instances mockedInstances = mock(Instances.class);
            when(mockedInstances.attribute(config.getClassAttribute())).thenReturn(null);

            datasetLoaderMock.when(() -> DatasetLoader.load(any(Path.class), anyString())).thenReturn(mockedInstances);
            converterMock.when(() -> SentimentLabelConverter.convertTo3Class(any(), anyString())).thenReturn(mockedInstances);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                Main.runAnalysis(config);
            });
            assertTrue(ex.getMessage().contains("Class attribute not found"));
        }
    }

    @Test
    void parseArgsSetsAllFieldsCorrectly() {
        String[] args = {
                "--dataset=path/to/data.csv",
                "--text-attr=my_text",
                "--class-attr=my_class",
                "--train-ratio=0.75",
                "--seed=12345",
                "--epsilon=0.0001",
                "--limit=7"
        };
        Main.Config parsedConfig = Main.parseArgs(args);

        assertEquals("path/to/data.csv", parsedConfig.getDatasetPath());
        assertEquals("my_text", parsedConfig.getTextAttribute());
        assertEquals("my_class", parsedConfig.getClassAttribute());
        assertEquals(0.75, parsedConfig.getTrainRatio());
        assertEquals(12345L, parsedConfig.getSeed());
        assertEquals(0.0001, parsedConfig.getEpsilon());
        assertEquals(7, parsedConfig.getSampleLimit());
        assertFalse(parsedConfig.isShowHelp());
    }

    @Test
    void parseArgsHelpFlagSetsShowHelp() {
        Main.Config parsedConfig = Main.parseArgs(new String[]{"--help"});
        assertTrue(parsedConfig.isShowHelp());

        parsedConfig = Main.parseArgs(new String[]{"-h"});
        assertTrue(parsedConfig.isShowHelp());
    }

    @Test
    void groupByGroupsCorrectly() {
        PredictionResult pr1 = mock(PredictionResult.class);
        when(pr1.product()).thenReturn("prodA");

        PredictionResult pr2 = mock(PredictionResult.class);
        when(pr2.product()).thenReturn("prodB");

        PredictionResult pr3 = mock(PredictionResult.class);
        when(pr3.product()).thenReturn("prodA");

        List<PredictionResult> list = List.of(pr1, pr2, pr3);

        Map<String, List<PredictionResult>> grouped = Main.groupBy(list, PredictionResult::product, "UNKNOWN");

        assertEquals(2, grouped.size());
        assertTrue(grouped.containsKey("prodA"));
        assertTrue(grouped.containsKey("prodB"));
        assertEquals(2, grouped.get("prodA").size());
        assertEquals(1, grouped.get("prodB").size());
    }

    @Test
    void groupByUsesFallbackWhenKeyIsNullOrBlank() {
        PredictionResult pr1 = mock(PredictionResult.class);
        when(pr1.product()).thenReturn(null);

        PredictionResult pr2 = mock(PredictionResult.class);
        when(pr2.product()).thenReturn("");

        PredictionResult pr3 = mock(PredictionResult.class);
        when(pr3.product()).thenReturn("  ");

        List<PredictionResult> list = List.of(pr1, pr2, pr3);

        Map<String, List<PredictionResult>> grouped = Main.groupBy(list, PredictionResult::product, "FALLBACK");

        assertEquals(1, grouped.size());
        assertTrue(grouped.containsKey("FALLBACK"));
        assertEquals(3, grouped.get("FALLBACK").size());
    }

    @Test
    void runAnalysisShowHelpOnlyPrintsHelp() throws Exception {
        Main.Config helpConfig = new Main.Config();
        helpConfig.setShowHelp(true);

        Main.runAnalysis(helpConfig);  // No exception expected
    }
}
