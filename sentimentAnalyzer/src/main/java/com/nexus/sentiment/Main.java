package com.nexus.sentiment;

import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instances;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Main {
    private static final String DEFAULT_DATASET = "src/main/resources/data/sample_reviews.csv";

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Path datasetPath = args.length > 0 ? Paths.get(args[0]) : Paths.get(DEFAULT_DATASET);
        if (!Files.exists(datasetPath)) {
            throw new IllegalArgumentException("Dataset not found: " + datasetPath.toAbsolutePath());
        }

        Instances data = DatasetLoader.load(datasetPath, "sentiment_label");
        Attribute classAttribute = data.classAttribute();
        String[] classValues = new String[classAttribute.numValues()];
        for (int i = 0; i < classAttribute.numValues(); i++) {
            classValues[i] = classAttribute.value(i);
        }

        ScoreMapper scoreMapper = ScoreMapper.fromAttribute(classAttribute);

    DataSplitter.Split split = DataSplitter.split(data, 0.8, 42);
    SentimentModelTrainer trainer = new SentimentModelTrainer("review_text");
        FilteredClassifier classifier = trainer.train(split.train());

        Evaluation evaluation = new Evaluation(split.train());
        evaluation.evaluateModel(classifier, split.test());

        List<PredictionResult> predictions = SentimentPredictor.predict(classifier, split.test(), scoreMapper);

        ReportPrinter.printEvaluation(evaluation, classValues);
        ReportPrinter.printPredictions(predictions, classValues, Math.min(10, predictions.size()));

        Map<String, List<PredictionResult>> byProduct = groupBy(predictions, PredictionResult::product, "UNKNOWN_PRODUCT");
        Map<String, List<PredictionResult>> byCompany = groupBy(predictions, PredictionResult::company, "UNKNOWN_COMPANY");

        Map<String, SentimentStatistics.GroupStatistics> productSummaries = ReportPrinter.printGroupSummaries(
                "Product Summaries",
                byProduct,
                classValues
        );

        Map<String, SentimentStatistics.GroupStatistics> companySummaries = ReportPrinter.printGroupSummaries(
                "Company Summaries",
                byCompany,
                classValues
        );

        ReportPrinter.printKlDivergence(productSummaries, 1e-6);
        ReportPrinter.printKlDivergence(companySummaries, 1e-6);
    }

    private static Map<String, List<PredictionResult>> groupBy(
            List<PredictionResult> predictions,
            java.util.function.Function<PredictionResult, String> classifier,
            String fallback
    ) {
        Map<String, List<PredictionResult>> grouped = new HashMap<>();
        for (PredictionResult result : predictions) {
            String key = classifier.apply(result);
            if (key == null || key.isBlank()) {
                key = fallback;
            }
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(result);
        }
        return grouped;
    }
}
