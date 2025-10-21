package com.nexus.sentiment;

import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.Instances;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class Main {
    private static final String DEFAULT_DATASET = "src/main/resources/data/sample_reviews.csv";
    private static final String DEFAULT_TEXT_ATTR = "review_text";
    private static final String DEFAULT_CLASS_ATTR = "sentiment_label";

    private Main() {}

    public static void main(String[] args) throws Exception {
        Config cfg = parseArgs(args);
        if (cfg.showHelp) {
            printHelp();
            return;
        }

        runAnalysis(cfg);
    }

    public static void runAnalysis(Config cfg) throws Exception {
        if (cfg.showHelp) {
            printHelp();
            return;
        }

        Path datasetPath = Paths.get(cfg.datasetPath);
        if (!Files.exists(datasetPath)) {
            throw new IllegalArgumentException("Dataset not found: " + datasetPath.toAbsolutePath());
        }

        Instances data = DatasetLoader.load(datasetPath, cfg.classAttribute);

        printAttributes(data);

        System.out.println("\nConverting to 3-class labels...");
        data = SentimentLabelConverter.convertTo3Class(data, cfg.classAttribute);

        Attribute classAttribute = data.attribute(cfg.classAttribute);
        if (classAttribute == null) {
            throw new IllegalArgumentException("Class attribute not found: " + cfg.classAttribute);
        }
        data.setClass(classAttribute);

        String[] classValues = extractClassValues(classAttribute);

        ScoreMapper scoreMapper = ScoreMapper.fromAttribute(classAttribute);

        DataSplitter.Split split = DataSplitter.split(data, cfg.trainRatio, cfg.seed);

        SentimentModelTrainer trainer = new SentimentModelTrainer();
        FilteredClassifier classifier = trainer.train(split.train(), cfg.textAttribute);

        Evaluation eval = new Evaluation(split.train());
        eval.evaluateModel(classifier, split.test());

        List<PredictionResult> predictions = SentimentPredictor.predict(classifier, split.test(), scoreMapper);

        ReportPrinter.printEvaluation(eval, classValues);
        ReportPrinter.printPredictions(predictions, classValues, Math.min(cfg.sampleLimit, predictions.size()));

        Map<String, List<PredictionResult>> byProduct = groupBy(predictions, PredictionResult::product, "UNKNOWN_PRODUCT");
        Map<String, List<PredictionResult>> byCompany = groupBy(predictions, PredictionResult::company, "UNKNOWN_COMPANY");

        Map<String, SentimentStatistics.GroupStatistics> productSummaries = ReportPrinter.printGroupSummaries("Product Summaries", byProduct, classValues);
        Map<String, SentimentStatistics.GroupStatistics> companySummaries = ReportPrinter.printGroupSummaries("Company Summaries", byCompany, classValues);

        ReportPrinter.printKlDivergence(productSummaries, cfg.epsilon);
        ReportPrinter.printKlDivergence(companySummaries, cfg.epsilon);
    }

    static Map<String, List<PredictionResult>> groupBy(
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

    static void printAttributes(Instances data) {
        System.out.println("Attributes after loading:");
        for (int i = 0; i < data.numAttributes(); i++) {
            System.out.printf(" %d: %s (%s)%n", i, data.attribute(i).name(), data.attribute(i).type());
        }
    }

    static String[] extractClassValues(Attribute classAttribute) {
        String[] classValues = new String[classAttribute.numValues()];
        for (int i = 0; i < classAttribute.numValues(); i++) {
            classValues[i] = classAttribute.value(i);
        }
        return classValues;
    }

    static void printHelp() {
        System.out.println("Sentiment Analyzer - Options:\n" +
                " --dataset=<path>     Path to CSV dataset (default: " + DEFAULT_DATASET + ")\n" +
                " --text-attr=<name>   Name of the text attribute (default: " + DEFAULT_TEXT_ATTR + ")\n" +
                " --class-attr=<name>  Name of the class attribute (default: " + DEFAULT_CLASS_ATTR + ")\n" +
                " --train-ratio=<0-1>  Train split ratio (default: 0.8)\n" +
                " --seed=<long>        Random seed (default: 42)\n" +
                " --epsilon=<double>   Smoothing epsilon for KL (default: 1e-6)\n" +
                " --limit=<int>        Number of sample predictions to print (default: 10)\n" +
                " --help               Show this help message\n"
        );
    }

    public static Config parseArgs(String[] args) {
        Config cfg = new Config();
        for (String arg : args) {
            if (arg == null) continue;
            if (arg.equals("--help") || arg.equals("-h")) {
                cfg.showHelp = true;
            } else if (arg.startsWith("--dataset=")) {
                cfg.datasetPath = arg.substring("--dataset=".length());
            } else if (arg.startsWith("--text-attr=")) {
                cfg.textAttribute = arg.substring("--text-attr=".length());
            } else if (arg.startsWith("--class-attr=")) {
                cfg.classAttribute = arg.substring("--class-attr=".length());
            } else if (arg.startsWith("--train-ratio=")) {
                try {
                    cfg.trainRatio = Double.parseDouble(arg.substring("--train-ratio=".length()));
                } catch (Exception ignored) {}
            } else if (arg.startsWith("--seed=")) {
                try {
                    cfg.seed = Long.parseLong(arg.substring("--seed=".length()));
                } catch (Exception ignored) {}
            } else if (arg.startsWith("--epsilon=")) {
                try {
                    cfg.epsilon = Double.parseDouble(arg.substring("--epsilon=".length()));
                } catch (Exception ignored) {}
            } else if (arg.startsWith("--limit=")) {
                try {
                    cfg.sampleLimit = Integer.parseInt(arg.substring("--limit=".length()));
                } catch (Exception ignored) {}
            } else {
                if (cfg.datasetPath.equals(DEFAULT_DATASET)) {
                    cfg.datasetPath = arg;
                }
            }
        }
        return cfg;
    }

    static final class Config {
        String datasetPath = DEFAULT_DATASET;
        String textAttribute = DEFAULT_TEXT_ATTR;
        String classAttribute = DEFAULT_CLASS_ATTR;
        double trainRatio = 0.8;
        long seed = 42L;
        double epsilon = 1e-6;
        int sampleLimit = 10;
        boolean showHelp = false;
    }
}
