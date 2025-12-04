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

### 2. DataSplitterTest

Tests train/test data splitting:

- Valid ratio splitting (80/20, 70/30, etc.)
- Deterministic splitting with seeds
- Invalid ratio handling
- Class attribute preservation
- Small dataset handling

### 3. SentimentModelTrainerTest (Heavy NLP Testing)

**Comprehensive NLP and text processing tests:**

- Trainer initialization
- Model training with valid data
- Missing text attribute validation
- **Tokenization testing** (mixed case, punctuation, special characters)
- **Stop word removal** (the, a, is, etc.)
- **Stemming** (running→run, breaks→break)
- **TF-IDF weighting** (unique vs common word importance)
- **Multi-class prediction** (positive, negative, neutral)
- Empty text handling
- Long text handling (100+ sentences)
- Special characters and Unicode (★, ☹, café, etc.)
- Minimal training data edge cases
- Model persistence from file

### 4. ScoreMapperTest

Tests sentiment label to numeric score mapping:

- Standard sentiment labels (positive=1.0, negative=-1.0, neutral=0.0)
- Case-insensitive mapping
- Custom label handling
- Unknown label defaults
- Immutable score map
- Multiple label distribution
- Single label edge case

### 5. DistributionUtilsTest

Tests statistical distribution utilities:

- Distribution smoothing
- Zero value smoothing
- KL divergence calculation
- Identical distribution divergence (should be ~0)
- Missing keys validation
- Symmetric KL divergence
- Proportions from counts conversion
- Zero total handling
- Sum-to-one validation
- Original data preservation

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

### 7. SentimentPredictorTest

Tests prediction generation:

- Results generation
- All fields populated
- Expected score range (-1.0 to 1.0)
- Distribution sums to 1.0
- Probability validity (0.0 to 1.0)
- Probability lookup by label
- Unknown label handling
- Formatted probability output
- Debug summary generation
- Missing attributes handling
- Predicted label matches highest probability
- Expected score calculation verification

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

