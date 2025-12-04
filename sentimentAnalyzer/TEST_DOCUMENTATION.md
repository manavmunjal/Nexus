# Sentiment Analyzer Test Suite

## Overview

Comprehensive unit tests for the multi-class sentiment analysis system using Java, Weka, and Naive Bayes classifier.

## Test Coverage

### 1. DatasetLoaderTest

Tests CSV dataset loading and validation:

- Valid dataset loading
- Missing class attribute handling
- Non-existent file handling
- Empty dataset handling

#### Equivalence Partitions

| Partition | Description | Tests Targeting Partition |
|-----------|-------------|--------------------------|
| **Valid dataset** | Dataset has both a class attribute and a text attribute, with at least one instance. | `testLoadIllFormattedDataset`, `testLoadMultiColumnHeader`, `testLoadNumericClass`, `testLoadClassAttributeString`, `testLoadTextAttributeNotString`, `testLoadReviewTextCleaned`, `testLoadReviewTextCleanedQuotesAndApostrophes` |
| **Empty dataset** | Dataset contains zero rows. | `testLoadEmptyFile`, `testLoadNoRows` |
| **Missing or incorrect attributes** | Required class or text columns are missing, or class/text attributes are the same. | `testLoadMissingClassAttribute`, `testLoadMissingTextAttribute`, `testLoadSingleColumnOnlyClass`, `testLoadReviewAttributeMissingThrows`, `testLoadTextAndClassSame` |
| **Text with special characters** | Text values contain quotes, apostrophes, commas, slashes, or other special characters. | `testLoadIllFormattedDataset`, `testLoadReviewTextCleaned`, `testLoadReviewTextCleanedQuotesAndApostrophes` |
| **Numeric or string class** | Class attribute is numeric or string and requires conversion to nominal. | `testLoadNumericClass`, `testLoadClassAttributeString` |
| **Boundary / missing values** | File does not exist, text column empty, or class column contains missing values. | `testLoadFileDoesNotExist`, `testLoadTextOnlyEmpty`, `testLoadMissingClassValues` |

---

### 2. DataSplitterTest

Tests train/test data splitting:

- Valid ratio splitting (80/20, 70/30, etc.)
- Deterministic splitting with seeds
- Invalid ratio handling
- Class attribute preservation
- Small dataset handling

#### Equivalence Partitions

| Partition | Description | Tests Targeting Partition |
|-----------|-------------|--------------------------|
| **Valid dataset, typical train ratio** | Dataset contains multiple instances, `trainRatio` strictly between 0 and 1, `seed >= 0`. | `testSplitWithValidRatio`, `testSplitIsDeterministic`, `testSplitPreservesClassAttribute`, `testTrainSizeEqualsNumInstancesAdjusted` |
| **Empty dataset** | Dataset contains zero instances. | `testSplitWithEmptyDataset` |
| **Train ratio out of bounds** | `trainRatio <= 0` or `trainRatio >= 1`. | `testSplitWithInvalidRatio`, `testSplitWithInvalidTrainRatio` |
| **Negative seed** | `seed < 0`. | `testSplitWithInvalidSeed` |
| **Small dataset** | Dataset contains very few instances; tests edge-case adjustments for train/test split. | `testSplitWithSmallDataset`, `testSplitWithTinyDataset` |
| **Single instance dataset** | Dataset has exactly one instance. | `testTrainSizeZeroAdjustedToOne`, `testSplitWithSingleInstance` |
| **Train size rounding to 0** | Rounding `trainSize` would yield 0; adjusted to 1. | `testTrainSizeZeroAdjustedToOne` |

---

### 3. SentimentModelTrainerTest

**Comprehensive NLP and text processing tests:**

- Trainer initialization
- Model training with valid data
- Missing text attribute validation
- Tokenization (mixed case, punctuation, special characters)
- Stop word removal
- Stemming
- TF-IDF weighting
- Multi-class prediction (positive, negative, neutral)
- Empty text handling
- Long text handling (100+ sentences)
- Special characters and Unicode
- Minimal training data edge cases
- Model persistence from file

#### Equivalence Partitions

| Partition | Description | Tests Targeting Partition |
|-----------|-------------|--------------------------|
| **Valid training data** | Dataset contains sufficient instances (≥5), valid text and class attributes, labels correct. | `testTrainerInitialization`, `testTrainWithValidData`, `testNlpTokenization`, `testTfIdfWeighting`, `testSeenTrainedTextPrediction`, `testEmptyInstance`, `testLongInstance`, `testSingleWordInstance`, `testModelPersistenceFromFile` |
| **Insufficient instances** | Dataset contains fewer than 5 instances. | `testInsufficientTrainingInstances`, `testInsufficientInstancesAtBoundary` |
| **Invalid or missing class attribute** | Class attribute not set or missing. | `testClassAttributeNotSet` |
| **Invalid or missing text attribute** | Text attribute does not exist or has missing values. | `testTrainWithMissingTextAttribute`, `testTextAttributeMissingValue` |
| **Irregular training labels** | Labels not part of the class attribute’s allowed values. | `testIrregularTrainingLabels` |
| **Text edge cases** | Empty strings, whitespace-only, single words, long text, mixed punctuation, special characters, or emojis. | `testNlpTokenization`, `testEmptyInstance`, `testLongInstance`, `testSingleWordInstance` |
| **Valid model persistence** | Model is saved/loaded correctly from a file. | `testModelPersistenceFromFile` |
| **Untrained classifier state** | `getClassifier()` called before `train()` returns null. | `testGetClassifierBeforeTraining` |

---

### 4. ScoreMapperTest

Tests sentiment label to numeric score mapping:

- Standard sentiment labels
- Case-insensitive mapping
- Custom label handling
- Unknown label defaults
- Immutable score map
- Multiple label distribution
- Single label edge case

#### Equivalence Partitions

| Partition | Description | Tests Targeting Partition |
|-----------|-------------|--------------------------|
| **Standard labels** | Class attribute has "positive", "negative", "neutral". | `testStandardSentimentLabels` |
| **Mixed case labels** | Labels have mixed case. | `testCaseInsensitiveMapping` |
| **Custom labels** | Non-standard labels; map linearly from -1.0 to 1.0. | `testCustomLabels` |
| **Unknown/unrecognized labels** | Label not present; returns default 0.0. | `testUnknownLabelReturnsDefault` |
| **Single label edge case** | Class attribute contains only one label. | `testSingleLabelEdgeCase` |

---

### 5. DistributionUtilsTest

Tests statistical distribution utilities:

- Distribution smoothing
- Zero value smoothing
- KL divergence calculation
- Symmetric divergence
- Missing keys validation
- Proportions from counts conversion
- Sum-to-one validation
- Original data preservation

#### Equivalence Partitions

| Partition | Description | Tests Targeting Partition |
|-----------|-------------|--------------------------|
| **Standard distribution** | Normal probability distribution with positive values summing to 1.0. | `testSmooth`, `testSmoothPreservesOriginal`, `testKlDivergence`, `testSymmetricKlDivergence`, `testSymmetricKlDivergenceIdentical` |
| **Distribution with zeros** | Some categories have zero probability. | `testSmoothWithZeros`, `testSmoothAllZeroDistribution` |
| **Empty distribution** | No keys present. | `testSmoothEmptyDistribution`, `testProportionsFromCountsEmptyMap` |
| **Single-label distribution** | Only one category present. | `testSmoothSingleLabelDistribution`, `testProportionsFromCountsSingleLabel` |
| **Counts for proportions (normal)** | Map of counts where total sum > 0. | `testProportionsFromCounts`, `testProportionsFromCountsSumToOne` |
| **Counts with zero total** | Map of counts where all counts are zero. | `testProportionsFromCountsZeroTotal` |
| **KL divergence with differing keys** | Distributions with non-matching labels. | `testKlDivergenceMissingKeys`, `testSymmetricKlDivergenceMissingKeys` |
| **KL divergence with zeros** | Zero probabilities in KL divergence calculations. | `testKlDivergenceWithZeroInP`, `testKlDivergenceQHasZeroProbability` |
| **Smoothing edge-case epsilon** | Tests epsilon = 0 or epsilon < 0. | `testSmoothWithZeroEpsilon`, `testSmoothWithNegativeEpsilon` |

---

### 6. SentimentStatisticsTest

Tests aggregate statistics computation:

- Basic statistics (counts, proportions)
- Mean score calculation
- Variance and standard deviation
- Skewness calculation
- Empty predictions handling
- Single prediction edge case
- All-positive distribution
- Positive skewness detection
- Statistics immutability
- Multiple group comparison

#### Equivalence Partitions

| Partition | Description | Tests Targeting Partition |
|-----------|-------------|--------------------------|
| **Standard mixed predictions** | Predictions contain multiple labels with varying scores. | `testSummarizeBasicStatistics`, `testProportionsCalculation`, `testMeanScoreCalculation`, `testVarianceAndStdDevCalculation`, `testSkewnessCalculation` |
| **Empty prediction list** | No predictions provided. | `testEmptyPredictions` |
| **Single prediction** | Only one prediction provided. | `testSinglePrediction` |
| **All positive predictions** | All predictions have the positive label. | `testAllPositivePredictions` |
| **Right-skewed distribution** | Score distribution with a positive tail. | `testPositiveSkewness` |
| **Extreme score values** | Very large or very small scores. | `testExtremeScores` |
| **Missing labels in predictions** | Some class labels absent. | `testMissingLabels` |
| **Multiple independent groups** | Separate sets of predictions representing different groups. | `testMultipleGroups` |

---

### 7. SentimentPredictorTest

Tests prediction generation:

- Results generation
- All required fields populated
- Expected score range (-1.0 to 1.0)
- Distribution sums to 1.0
- Probability validity (0.0 to 1.0)
- Probability lookup by label
- Unknown label handling
- Minimal dataset handling (missing optional attributes)
- Predicted label correctness
- Classifier exception propagation

#### Equivalence Partitions

| Partition | Description | Tests Targeting Partition |
|-----------|-------------|--------------------------|
| **Standard complete dataset** | Predictions with all required attributes (review_text + class) and optional attributes present, multiple class labels. | `testPredictReturnsResults`, `testPredictionContainsAllFields`, `testExpectedScoreInRange`, `testDistributionSumsToOne`, `testDistributionProbabilitiesValid`, `testProbabilityForMethod`, `testPredictedLabelMatchesHighestProbability`, `testExpectedScoreCalculation` |
| **Unknown label in probability query** | Request probability for a label not in classValues. | `testProbabilityForUnknownLabel` |
| **Minimal dataset / missing optional attributes** | Instances missing optional fields like review_id, company, or product. | `testMissingAttributes` |
| **Instances with missing class label** | Class attribute is not set. | `testActualLabelUnknownWhenClassMissing` |
| **Empty dataset** | No instances in the dataset. | `testPredictOnEmptyDatasetReturnsEmptyList` |
| **Classifier throws exception** | Classifier fails during prediction. | `testPredictPropagatesClassifierException` |

### 8. SentimentServiceTest

Tests the high-level sentiment service functionality:

- Text scoring
- Model saving and loading
- Model readiness and training
- Dataset path resolution
- Exception handling and edge cases

---

#### Equivalence Partitions

| Partition | Description | Tests Targeting Partition |
|-----------|-------------|--------------------------|
| **Valid text scoring** | Input text is non-null, non-empty, non-whitespace, classifier returns meaningful distribution. | `testScoreFromText_highConfidencePositiveReturnsFive`, `testScoreFromText_highConfidenceNegativeReturnsZero` |
| **Invalid text input** | Text is null, empty string, or whitespace-only. | `testScoreFromText_nullTextCausesException`, `testScoreFromText_emptyStringThrowsException`, `testScoreFromText_whitespaceStringThrowsException` |
| **Model persistence – successful save** | All model components are present, SerializationHelper writes successfully. | `saveModel_createsFiles` |
| **Model persistence – failure** | SerializationHelper throws exception during save. | `saveModel_writeFails_throwsRuntimeException`, `saveModel_partialFailure_throwsRuntimeException` |
| **Model loading – missing files** | Model files not present in directory. | `loadModel_missingFiles_throwsRuntimeException` |
| **Model loading – success** | Model files exist and are valid. | `loadModel_success_loadsRealFiles` |
| **Model loading – corrupted files** | Model files exist but contain incorrect object types or are corrupted. | `loadModel_corruptedFiles_throwsRuntimeException` |
| **Readiness check – untrained** | Classifier, trained header, or score mapper missing. | `ensureReady_throwsIfNotTrained` |
| **Readiness check – trained** | Classifier, trained header, and score mapper all set. | `ensureReady_passesIfTrained` |
| **isTrained – false** | One or more fields missing. | `isTrainedShouldReturnFalseWhenAllFieldsNull`, `isTrainedShouldReturnFalseWhenOnlyClassifierSet`, `isTrainedShouldReturnFalseWhenClassifierAndMapperSet` |
| **isTrained – true** | All required fields (classifier, score mapper, trained header) are non-null. | `isTrainedShouldReturnTrueWhenAllFieldsSet` |
| **Training with defaults** | `trainModel` called with null dataset path, text, or class attribute. | `trainModel_withNullParameters_usesDefaults` |
| **Training with custom parameters** | `trainModel` called with user-specified dataset path, class, or text attributes. | `trainModel_withCustomParameters_callsLoaderWithProvidedValues` |
| **Training failures – loader** | DatasetLoader throws IOException. | `trainModel_whenLoaderThrows_wrappedInRuntimeException` |
| **Training failures – trainer** | SentimentModelTrainer throws exception during training. | `trainModel_whenTrainerThrows_wrappedInRuntimeException` |
| **Dataset resolution – existing file** | Dataset path points to existing file. | `resolveDatasetPath_existingFile_returnsPath` |
| **Dataset resolution – classpath resource** | Dataset path refers to classpath resource; copied to temp file. | `resolveDatasetPath_inClasspath_returnsTempCopy` |
| **Dataset resolution – missing** | Dataset path not found on filesystem or classpath. | `resolveDatasetPath_missing_throws` |

### 9. MainTest

Tests the **Main** class for sentiment analysis CLI — not related to or used by API endpoints. Focuses on correct handling of inputs, argument parsing, dataset loading, and internal grouping logic.

#### Equivalence Partitions

| Input Partition | Description | Tests Targeting Partition |
|-----------------|-------------|--------------------------|
| **Arguments – valid** | All CLI arguments are present and correctly parsed (dataset path, text attribute, class attribute, train ratio, seed, epsilon, sample limit). | `parseArgsSetsAllFieldsCorrectly` |
| **Arguments – help flag** | `--help` or `-h` sets `showHelp=true`. | `parseArgsHelpFlagSetsShowHelp`, `runAnalysisShowHelpOnlyPrintsHelp` |
| **Dataset path – missing file** | Path points to a non-existent file; should throw `IllegalArgumentException`. | `runAnalysisFileNotExistsThrows` |
| **Class attribute – missing** | Attribute not found in dataset; should throw exception. | `runAnalysisClassAttributeNullThrows` |
| **Sample limit – zero/negative** | Zero or negative values; should not crash, may print no predictions. | `runAnalysisWithZeroOrNegativeSampleLimitPrintsNoPredictions` |
| **Sample limit – exceeding predictions** | Sample limit is larger than the number of predictions; should print only available predictions. | `runAnalysisWithSampleLimitExceedingPredictionCountPrintsAll` |
| **PredictionResult key – valid** | Non-null, non-empty, non-blank string used for grouping. | `groupByGroupsCorrectly` |
| **PredictionResult key – null/empty/blank** | Null, empty, or whitespace string triggers fallback key. | `groupByUsesFallbackWhenKeyIsNullOrBlank` |

## Running Tests

### Run all tests:

```bash
mvn test
```

### Run specific test class:

```bash
mvn test -Dtest=SentimentModelTrainerTest
```

### Run with verbose output:

```bash
mvn test -X
```

### Run tests and generate coverage report:

```bash
mvn clean test
```

## NLP Testing Highlights

The test suite includes extensive NLP validation:

1. **Text Preprocessing**

   - Case normalization (GREAT → great)
   - Punctuation handling (!!! ??? ...)
   - Special character handling (@, #, &, etc.)

2. **Tokenization**

   - Word boundary detection
   - Hyphenated words (top-notch)
   - Numbers in text (5 stars)

3. **Stop Word Removal**

   - Common words filtered (the, a, is, are, etc.)
   - Content words preserved (excellent, terrible)

4. **Stemming**

   - Verb forms (running, runs, ran → run)
   - Plural forms (products → product)
   - Past tense (broke, broken → break)

5. **TF-IDF Weighting**

   - Rare discriminative words weighted higher
   - Common neutral words weighted lower
   - Document frequency impact

6. **Multi-class Classification**
   - Positive sentiment detection
   - Negative sentiment detection
   - Neutral sentiment detection
   - Probability distributions

## Integration Tests

The suite includes integration tests that verify the interaction between controllers, services, and repositories.

### 1. Review Workflow Integration
**File:** `ReviewWorkflowIntegrationTest.java`
- **Scope:** Verifies the end-to-end flow of posting a review via `ProductController`.
- **Scenarios:**
    - Successful review posting (201 Created).
    - Handling non-existent products (404 Not Found).
    - Verifies JSON response structure and content.

### 2. Controller Component Tests
**Files:** `RepositoryIntegrationTest.java`, `DatabaseIntegrationTest.java`
- **Scope:** Tests individual controller endpoints and their interaction with the service layer.
- **Key Tests:**
    - **CompanyController:** Create company, Get average rating, Error handling (500 on DB failure).
    - **ProductController:** Create product, Get reviews, Post reviews.
    - **UserController:** Create user, Get user details.
- **Methodology:** Uses `@WebMvcTest` with `MockMvc` to simulate HTTP requests and `Mockito` to mock repository responses.

## Input Partitions

Unit tests utilize specific input partitions to ensure robust coverage:

### 1. Text Content Partitions
- **Length:** Empty string, Single word, Short sentence, Long paragraph (100+ sentences).
- **Characters:** Alphanumeric, Special characters (@, #, $).
- **Formatting:** Mixed case (GrEaT), All caps, All lowercase.
- **Punctuation:** Heavy punctuation (!!!), No punctuation, Mixed.

### 2. Dataset Attribute Partitions
- **Completeness:** All attributes present, Missing optional attributes (ID, Company, Product), Missing mandatory attributes (Text, Class).
- **Validity:** Valid CSV format, Malformed CSV, Empty file, Non-existent file.

### 3. Class Label Partitions
- **Standard:** "positive", "negative", "neutral".
- **Custom:** User-defined labels (e.g., "joy", "anger").
- **Unknown:** Labels not present in training data (should return 0 probability).
- **Distribution:** Balanced classes, Skewed distribution (e.g., 90% positive), Single class only.

### 4. Numerical Input Partitions
- **Split Ratios:** Valid (0.0 < r < 1.0), Boundary (0.0, 1.0), Invalid (< 0.0, > 1.0).
- **Probabilities:** Valid (0.0 to 1.0), Sum to 1.0.
- **Scores:** Standard range (-1.0 to 1.0).

## Test Data

Test datasets are located in:

- `src/test/resources/data/test_reviews.csv` - Sample test data

## Dependencies

- JUnit Jupiter 5.10.1 - Testing framework
- AssertJ 3.24.2 - Fluent assertions
- Weka 3.8.6 - ML library
- Apache Commons Math3 3.6.1 - Statistical calculations

## Continuous Integration

Tests are designed to be deterministic and can be integrated into CI/CD pipelines:

- All randomized operations use fixed seeds
- No external dependencies required
- Fast execution (<1 minute for full suite)


## Start the Service

### Set MongoDB password environment variable first
```bash
export MONGODB_PASSWORD=<your-mongodb-password>
```

### Start the service
```bash
mvn spring-boot:run
```

Create Users

### Create a regular user
```bash
curl -X POST http://localhost:8080/api/auth/users \
   -H "Content-Type: application/json" \
   -d '{"userId": "user123"}'
```

### Attempt to create a user with a missing userId

```bash
curl -X POST http://localhost:8080/api/auth/users \
   -H "Content-Type: application/json" \
   -d ''
```

#### Expected Response:

```json
{
  "error": "User ID is required. Please provide a valid user ID in the request body."
}
```

### Create the ADMIN user (required for training)
```bash
curl -X POST http://localhost:8080/api/auth/users \
   -H "Content-Type: application/json" \
   -d '{"userId": "ADMIN"}'
```

Train the Model (ADMIN only)

### Train with default dataset
```bash
curl -X POST "http://localhost:8080/api/sentiment/train" \
   -H "X-User-Id: ADMIN"
```

### Train with custom dataset
```bash
curl -X POST
"http://localhost:8080/api/sentiment/train?datasetPath=/path/to/data.csv&classAttr=sentiment&textAttr=text" \
   -H "X-User-Id: ADMIN"
```

Get Sentiment Score

### Get sentiment score for text (any authenticated user)
```bash
curl -X GET "http://localhost:8080/api/sentiment/score?text=I%20love%20this%20product" \
   -H "X-User-Id: user123"
```

### Example with negative text
```bash
curl -X GET "http://localhost:8080/api/sentiment/score?text=This%20is%20terrible" \
   -H "X-User-Id: user123"
```

Other Useful Commands

### List all users (ADMIN only)
```bash
curl -X GET http://localhost:8080/api/auth/users \
   -H "X-User-Id: ADMIN"
```

### Validate a user exists (ADMIN only)
```bash
curl -X GET http://localhost:8080/api/auth/users/user123/validate \
   -H "X-User-Id: ADMIN"
```

### Delete a user (ADMIN only)
```bash
curl -X DELETE http://localhost:8080/api/auth/users/user123 \
   -H "X-User-Id: ADMIN"
```

