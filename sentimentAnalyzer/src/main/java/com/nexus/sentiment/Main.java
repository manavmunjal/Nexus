package com.nexus.sentiment;

import com.nexus.sentiment.SentimentStatistics.GroupStatistics;

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
import java.util.function.Function;

/**
 * Main class for sentiment analysis application.
 */
public final class Main {

    /**
     * Default dataset path.
     */
    private static final String DEFAULT_DATASET =
            "src/main/resources/data/sample_reviews.csv";

    /**
     * Default text attribute name.
     */
    private static final String DEFAULT_TEXT_ATTR = "review_text";

    /**
     * Default class attribute name.
     */
    private static final String DEFAULT_CLASS_ATTR = "sentiment_label";

    /**
     * Default train ratio for splitting dataset.
     */
    private static final double DEFAULT_TRAIN_RATIO = 0.8;

    /**
     * Default random seed.
     */
    private static final long DEFAULT_SEED = 42L;

    /**
     * Default epsilon for smoothing in KL divergence.
     */
    private static final double DEFAULT_EPSILON = 1e-6;

    /**
     * Default limit for sample predictions printed.
     */
    private static final int DEFAULT_SAMPLE_LIMIT = 10;

    private Main() {
        // Prevent instantiation
    }

    /**
     * Entry point for the application.
     *
     * @param args Command line arguments.
     * @throws Exception If an error occurs during execution.
     */
    public static void main(final String[] args) throws Exception {
        final Config cfg = parseArgs(args);
        if (cfg.isShowHelp()) {
            printHelp();
            return;
        }
        runAnalysis(cfg);
    }

    /**
     * Runs the sentiment analysis pipeline.
     *
     * @param cfg Configuration options.
     * @throws Exception If an error occurs during processing.
     */
    public static void runAnalysis(final Config cfg) throws Exception {
        if (cfg.isShowHelp()) {
            printHelp();
            return;
        }

        final Path datasetFilePath = Paths.get(cfg.getDatasetPath());
        if (!Files.exists(datasetFilePath)) {
            throw new IllegalArgumentException(
                    "Dataset not found: "
                            + datasetFilePath.toAbsolutePath());
        }

        Instances data = DatasetLoader.load(
                datasetFilePath,
                cfg.getClassAttribute());

        printAttributes(data);

        System.out.println("\nConverting to 3-class labels...");
        data = SentimentLabelConverter.convertTo3Class(
                data,
                cfg.getClassAttribute());

        final Attribute classAttribute = data.attribute(
                cfg.getClassAttribute());
        if (classAttribute == null) {
            throw new IllegalArgumentException(
                    "Class attribute not found: "
                            + cfg.getClassAttribute());
        }
        data.setClass(classAttribute);

        final String[] classValues = extractClassValues(classAttribute);

        final ScoreMapper scoreMapper =
                ScoreMapper.fromAttribute(classAttribute);

        final DataSplitter.Split split =
                DataSplitter.split(data, cfg.getTrainRatio(), cfg.getSeed());

        final SentimentModelTrainer trainer = new SentimentModelTrainer();
        final FilteredClassifier classifier =
                trainer.train(split.train(), cfg.getTextAttribute());

        final Evaluation eval = new Evaluation(split.train());
        eval.evaluateModel(classifier, split.test());

        final List<PredictionResult> predictions = SentimentPredictor.predict(
                classifier,
                split.test(),
                scoreMapper);

        ReportPrinter.printEvaluation(eval, classValues);

        ReportPrinter.printPredictions(
                predictions,
                classValues,
                Math.min(cfg.getSampleLimit(), predictions.size())
        );

        final Map<String, List<PredictionResult>> byProduct = groupBy(
                predictions,
                PredictionResult::product,
                "UNKNOWN_PRODUCT");
        final Map<String, List<PredictionResult>> byCompany = groupBy(
                predictions,
                PredictionResult::company,
                "UNKNOWN_COMPANY");

        final Map<String, GroupStatistics> productSummaries =
                ReportPrinter.printGroupSummaries(
                        "Product Summaries",
                        byProduct,
                        classValues);

        final Map<String, GroupStatistics> companySummaries =
                ReportPrinter.printGroupSummaries(
                        "Company Summaries",
                        byCompany,
                        classValues);

        ReportPrinter.printKlDivergence(
                productSummaries,
                cfg.getEpsilon());

        ReportPrinter.printKlDivergence(
                companySummaries,
                cfg.getEpsilon());
    }

    /**
     * Groups prediction results by a classifier function.
     *
     * @param predictions List of predictions.
     * @param classifier Function to classify predictions.
     * @param fallback Fallback key if classification returns null or blank.
     * @return Map grouping predictions by key.
     */
    static Map<String, List<PredictionResult>> groupBy(
            final List<PredictionResult> predictions,
            final Function<PredictionResult, String> classifier,
            final String fallback) {
        final Map<String, List<PredictionResult>> grouped = new HashMap<>();
        for (final PredictionResult result : predictions) {
            String key = classifier.apply(result);
            if (key == null || key.isBlank()) {
                key = fallback;
            }
            grouped
                    .computeIfAbsent(key, ignored -> new ArrayList<>())
                    .add(result);
        }
        return grouped;
    }

    /**
     * Prints attribute details from dataset.
     *
     * @param data Instances dataset.
     */
    static void printAttributes(final Instances data) {
        System.out.println("Attributes after loading:");
        for (int i = 0; i < data.numAttributes(); i++) {
            System.out.printf(
                    " %d: %s (%s)%n",
                    i,
                    data.attribute(i).name(),
                    data.attribute(i).type());
        }
    }

    /**
     * Extracts class value names from class attribute.
     *
     * @param classAttribute Class attribute.
     * @return Array of class value names.
     */
    static String[] extractClassValues(final Attribute classAttribute) {
        final String[] classValues = new String[classAttribute.numValues()];
        for (int i = 0; i < classAttribute.numValues(); i++) {
            classValues[i] = classAttribute.value(i);
        }
        return classValues;
    }

    /**
     * Prints help information for command-line usage.
     */
    static void printHelp() {
        System.out.println(
                "Sentiment Analyzer - Options:\n"
                        + " --dataset=<path>   Path to CSV dataset\n"
                        + "                   (default: "
                        + DEFAULT_DATASET + ")\n"
                        + " --text-attr=<name> Name of text attribute\n"
                        + "                   (default: "
                        + DEFAULT_TEXT_ATTR + ")\n"
                        + " --class-attr=<name> Class attribute name\n"
                        + "                   (default: "
                        + DEFAULT_CLASS_ATTR + ")\n"
                        + " --train-ratio=<0-1> Train split ratio\n"
                        + "                   (default: "
                        + DEFAULT_TRAIN_RATIO + ")\n"
                        + " --seed=<long>     Random seed\n"
                        + "                   (default: "
                        + DEFAULT_SEED + ")\n"
                        + " --epsilon=<double> Smoothing epsilon\n"
                        + "                   (default: "
                        + DEFAULT_EPSILON + ")\n"
                        + " --limit=<int>     Sample predictions to print\n"
                        + "                   (default: "
                        + DEFAULT_SAMPLE_LIMIT + ")\n"
                        + " --help            Show help message\n"
        );
    }


    /**
     * Parses command-line arguments into a Config object.
     *
     * @param args Command-line arguments.
     * @return Config object with parsed values.
     */
    public static Config parseArgs(final String[] args) {
        final Config cfg = new Config();
        for (final String arg : args) {
            if (arg == null) {
                continue;
            }
            if ("--help".equals(arg) || "-h".equals(arg)) {
                cfg.setShowHelp(true);
            } else if (arg.startsWith("--dataset=")) {
                cfg.setDatasetPath(arg.substring("--dataset=".length()));
            } else if (arg.startsWith("--text-attr=")) {
                cfg.setTextAttribute(arg.substring("--text-attr=".length()));
            } else if (arg.startsWith("--class-attr=")) {
                cfg.setClassAttribute(arg.substring("--class-attr=".length()));
            } else if (arg.startsWith("--train-ratio=")) {
                try {
                    cfg.setTrainRatio(
                            Double.parseDouble(
                                    arg.substring("--train-ratio=".length())
                            )
                    );
                } catch (final Exception ignored) {
                    // ignore invalid double
                }
            } else if (arg.startsWith("--seed=")) {
                try {
                    cfg.setSeed(
                            Long.parseLong(
                                    arg.substring("--seed=".length())
                            )
                    );
                } catch (final Exception ignored) {
                    // ignore invalid long
                }
            } else if (arg.startsWith("--epsilon=")) {
                try {
                    cfg.setEpsilon(
                            Double.parseDouble(
                                    arg.substring("--epsilon=".length())
                            )
                    );
                } catch (final Exception ignored) {
                    // ignore invalid double
                }
            } else if (arg.startsWith("--limit=")) {
                try {
                    cfg.setSampleLimit(
                            Integer.parseInt(
                                    arg.substring("--limit=".length())
                            )
                    );
                } catch (final Exception ignored) {
                    // ignore invalid int
                }
            } else {
                if (DEFAULT_DATASET.equals(cfg.getDatasetPath())) {
                    cfg.setDatasetPath(arg);
                }
            }
        }
        return cfg;
    }

    /**
     * Configuration class for application settings.
     */
    static final class Config {

        /**
         * Path to the dataset file.
         */
        private String datasetPath = DEFAULT_DATASET;

        /**
         * Name of the text attribute.
         */
        private String textAttribute = DEFAULT_TEXT_ATTR;

        /**
         * Name of the class attribute.
         */
        private String classAttribute = DEFAULT_CLASS_ATTR;

        /**
         * Ratio for training set split.
         */
        private double trainRatio = DEFAULT_TRAIN_RATIO;

        /**
         * Random seed value.
         */
        private long seed = DEFAULT_SEED;

        /**
         * Epsilon smoothing parameter for KL divergence.
         */
        private double epsilon = DEFAULT_EPSILON;

        /**
         * Limit on number of sample predictions printed.
         */
        private int sampleLimit = DEFAULT_SAMPLE_LIMIT;

        /**
         * Flag to indicate if help should be shown.
         */
        private boolean showHelp = false;

        public String getDatasetPath() {
            return datasetPath;
        }

        public void setDatasetPath(final String datasetPathParam) {
            this.datasetPath = datasetPathParam;
        }

        public String getTextAttribute() {
            return textAttribute;
        }

        public void setTextAttribute(final String textAttributeParam) {
            this.textAttribute = textAttributeParam;
        }

        public String getClassAttribute() {
            return classAttribute;
        }

        public void setClassAttribute(final String classAttributeParam) {
            this.classAttribute = classAttributeParam;
        }

        public double getTrainRatio() {
            return trainRatio;
        }

        public void setTrainRatio(final double trainRatioParam) {
            this.trainRatio = trainRatioParam;
        }

        public long getSeed() {
            return seed;
        }

        public void setSeed(final long seedParam) {
            this.seed = seedParam;
        }

        public double getEpsilon() {
            return epsilon;
        }

        public void setEpsilon(final double epsilonParam) {
            this.epsilon = epsilonParam;
        }

        public int getSampleLimit() {
            return sampleLimit;
        }

        public void setSampleLimit(final int sampleLimitParam) {
            this.sampleLimit = sampleLimitParam;
        }

        public boolean isShowHelp() {
            return showHelp;
        }

        public void setShowHelp(final boolean showHelpParam) {
            this.showHelp = showHelpParam;
        }
    }
}
