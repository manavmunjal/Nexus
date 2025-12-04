# Nexus
This is the official repository for the Advanced Software Engineering project of team Nexus comprising of Manav Munjal, Sreenivas Karthik Bandi, Song Li and Sindhu Krishnamurthy.

# Sentiment Analyzer - Multi-Class Sentiment Analysis with Weka and SVM

## Project Overview

A comprehensive Java-based sentiment analysis system using Weka's machine learning library and SVM classifier with TF-IDF feature extraction. The system performs multi-class sentiment classification (positive, negative, neutral) on review text data.

## Key Features

### 1. **NLP Text Processing**
- **TF-IDF Vectorization**: Converts review text into numerical features weighted by term importance
- **Tokenization**: Breaks text into individual words with proper handling of punctuation and special characters
- **Stop Word Removal**: Filters common words (the, a, is, etc.) using Rainbow stop word list
- **Stemming**: Reduces words to their root form (running→run) using Snowball stemmer
- **Case Normalization**: Converts all text to lowercase for consistency

### 2. **Machine Learning**
- **SVM Multinomial Classifier**: Probabilistic model optimized for text classification (experimantal)
- **FilteredClassifier**: Combines preprocessing (TF-IDF) with classification in a pipeline
- **Multi-class Support**: Handles positive, negative, and neutral sentiments
- **Probability Distributions**: Provides confidence scores for each sentiment class

### 3. **Statistical Analysis**
- **Sentiment Scores**: Maps categorical labels to numeric scores (-1.0 to 1.0)
- **Expected Score Calculation**: Weighted average based on probability distributions
- **Variance & Standard Deviation**: Measures sentiment dispersion
- **Skewness Analysis**: Detects bias in sentiment distributions
- **KL-Divergence**: Compares sentiment distributions between products/companies

### 4. **Comparison Capabilities**
- **Product Comparison**: Analyze sentiment distributions across different products
- **Company Comparison**: Compare sentiment between competing companies
- **Statistical Summary**: Comprehensive metrics for each group (product/company)
- **Distribution Smoothing**: Handles zero probabilities for robust KL-divergence

## Project Structure

```
sentimentAnalyzer/
├── src/
│   ├── main/
│   │   ├── java/com/nexus/sentiment/
│   │   │   ├── Main.java                      # Entry point
│   │   │   ├── DatasetLoader.java             # CSV data loading
│   │   │   ├── DataSplitter.java              # Train/test splitting
│   │   │   ├── SentimentModelTrainer.java     # TF-IDF + SVM training
│   │   │   ├── SentimentPredictor.java        # Generate predictions
│   │   │   ├── ScoreMapper.java               # Label→score mapping
│   │   │   ├── SentimentStatistics.java       # Statistical computations
│   │   │   ├── DistributionUtils.java         # KL-divergence, smoothing
│   │   │   ├── PredictionResult.java          # Prediction data class
│   │   │   └── ReportPrinter.java             # Console output formatting
│   │   └── resources/data/
│   │       └── sample_reviews.csv             # Sample dataset
│   └── test/
│       ├── java/com/nexus/sentiment/
│       │   ├── DatasetLoaderTest.java         # 4 tests
│       │   ├── DataSplitterTest.java          # 6 tests
│       │   ├── SentimentModelTrainerTest.java # 14 NLP-focused tests
│       │   ├── ScoreMapperTest.java           # 7 tests
│       │   ├── DistributionUtilsTest.java     # 11 tests
│       │   ├── SentimentStatisticsTest.java   # 11 tests
│       │   └── SentimentPredictorTest.java    # 12 tests
│       └── resources/data/
│           └── test_reviews.csv               # Test dataset
├── pom.xml                                    # Maven configuration
└── TEST_DOCUMENTATION.md                      # Detailed test documentation

## Technologies Used

- **Java 17**: Modern Java with records and text blocks
- **Weka 3.8.6**: Machine learning library for NLP and classification
- **Apache Commons Math3 3.6.1**: Statistical calculations (skewness, etc.)
- **JUnit Jupiter 5.10.1**: Testing framework
- **AssertJ 3.24.2**: Fluent assertion library
- **Maven**: Build and dependency management

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Installation

1. **Clone and navigate to project**:
```bash
cd sentimentAnalyzer
```

2. **Install dependencies**:
```bash
mvn dependency:resolve
```

3. **Compile the project**:
```bash
mvn clean compile
```

### Running the Application

**Option 1: Using Maven exec plugin**
```bash
mvn exec:java
```

**Option 2: With custom dataset**
```bash
mvn exec:java -Dexec.args="path/to/your/reviews.csv"
```

**Option 3: Build JAR and run**
```bash
mvn package
java -cp target/sentiment-analyzer-1.0.0.jar com.nexus.sentiment.Main
```

### Expected CSV Format

```csv
review_id,company,product,review_text,sentiment_label
1,Acme,AcmePhone,"Battery life is outstanding!",positive
2,Acme,AcmePhone,"Screen cracked easily.",negative
3,Acme,AcmeTablet,"Average device overall.",neutral
```

**Required Columns**:
- `review_text`: The text to analyze
- `sentiment_label`: Ground truth labels (positive/negative/neutral/somewhat positive/somewhat negative/very positive/very negative). If unconventional labels are used, they will be mapped to standard labels.

**Optional Columns** (for grouping):
- `review_id`: Unique identifier
- `company`: For company-level aggregation
- `product`: For product-level aggregation

## Sample Output

```
==== Evaluation Metrics ====

Accuracy: 85.23%
Class 'positive' -> Precision: 0.867, Recall: 0.842, F1: 0.854
Class 'negative' -> Precision: 0.823, Recall: 0.858, F1: 0.840
Class 'neutral' -> Precision: 0.876, Recall: 0.857, F1: 0.866
Weighted AUC: 0.912

==== Product Summaries ====

AcmePhone -> total=45, meanScore=0.234, variance=0.856, stdDev=0.925, skewness=0.123
    Distribution: negative=0.244, neutral=0.333, positive=0.422
AcmeTablet -> total=38, meanScore=-0.156, variance=0.723, stdDev=0.850, skewness=-0.234
    Distribution: negative=0.421, neutral=0.316, positive=0.263

==== Symmetric KL Divergence ====

AcmePhone vs AcmeTablet -> KL=0.0856
```

## Running Tests

### Run all tests:
```bash
mvn test
```

### Run specific test class:
```bash
mvn test -Dtest=SentimentModelTrainerTest
```

### Run with coverage:
```bash
mvn clean test
```

### All in one: Tests:
You can verify the training process and the statistics generated by our service, using the
default test files provided in the `src/test/resources/data/` folder.
```bash
mvn clean compile exec:java -Dexec.args="--dataset=src/main/resources/data/augmented_cleaned_data.csv --text-attr=review_text --class-attr=sentiment_label --train-ratio=0.8 --seed=42 --limit=5"
```

## Test Suite

### Test Coverage: **250 Unit Tests**

### Heavy NLP Testing in `SentimentModelTrainerTest`:

1. **Tokenization Tests**
    - Mixed case handling (GREAT → great)
    - Punctuation (!!!, ???, ...)
    - Numbers (5 stars, 100%)
    - Special characters (@, #, &)

2. **Text Preprocessing**
    - Stop word removal
    - Stemming (running→run, breaks→break)
    - Empty text handling
    - Long text (100+ sentences)
    - Unicode characters (★, café, ☹)

3. **TF-IDF Features**
    - Rare word weighting
    - Common word downweighting
    - Document frequency impact

4. **Classification**
    - Multi-class predictions
    - Probability distributions
    - Confidence scores

## Use Cases

1. **Product Review Analysis**: Analyze customer sentiment for products
2. **Brand Monitoring**: Compare sentiment across competing brands
3. **Customer Feedback**: Identify areas of strength and weakness
4. **Trend Analysis**: Track sentiment changes over time
5. **Quality Assurance**: Flag overwhelmingly negative reviews for investigation

## Core Classes Explained

### `SentimentModelTrainer`
- Configures TF-IDF vectorization (5000 words, stemming, stop words)
- Trains SVM classifier
- Wraps in FilteredClassifier for pipeline execution

### `SentimentPredictor`
- Generates predictions for test instances
- Computes probability distributions
- Calculates expected sentiment scores

### `SentimentStatistics`
- Computes mean, variance, std dev, skewness
- Aggregates by product/company
- Provides label counts and proportions

### `DistributionUtils`
- KL-divergence for distribution comparison
- Laplace smoothing for zero probabilities
- Converts counts to proportions

## Customization

### Adjust TF-IDF Parameters
In `SentimentModelTrainer.java`:
```java
vectorizer.setWordsToKeep(10000);      // Increase vocabulary size
vectorizer.setMinTermFreq(2);          // Minimum term frequency
vectorizer.setNormalizeDocLength(true); // Document length normalization
```

### Change Stemmer
```java
vectorizer.setStemmer(new LovinsStemmer()); // Alternative stemmer
```

### Modify Score Mapping
In `ScoreMapper.java`, adjust the defaults:
```java
Map<String, Double> defaults = Map.of(
    "very_negative", -1.0,
    "negative", -0.5,
    "neutral", 0.0,
    "positive", 0.5,
    "very_positive", 1.0
);
```

## Third-Party Client Development

This section provides instructions for third-party developers who want to interact with the sentiment analysis service.

### API Endpoints

The service exposes the following RESTful endpoints for sentiment analysis.

#### 1. Get Sentiment Score

Analyzes the sentiment of a given text and returns a numerical score.

- **URL:** `/api/sentiment/score`
- **Method:** `GET`
- **Query Parameters:**
    - `text` (required): The string of text you want to analyze.

- **Success Response (200 OK):**
    - **Content-Type:** `application/json`
    - **Body:** A `double` value representing the sentiment score. A more positive value indicates more positive sentiment.

- **Example Request (using cURL):**
  ```bash
  curl -X GET "http://localhost:8080/api/sentiment/score?text=This%20is%20a%20great%20product!"
  ```

- **Error Responses:**
    - `400 Bad Request`: If the `text` parameter is missing or invalid.
    - `500 Internal Server Error`: If an unexpected error occurs during analysis.

#### 2. Train the Model

Triggers the training or retraining of the sentiment analysis model. This is an advanced feature and should be used with caution.

- **URL:** `/api/sentiment/train`
- **Method:** `POST`
- **Query Parameters (all optional):**
    - `datasetPath`: The file path to a CSV dataset for training. If not provided, a default dataset will be used.
    - `classAttr`: The name of the attribute in the CSV that contains the sentiment label (e.g., 'sentiment_label').
    - `textAttr`: The name of the attribute in the CSV that contains the text to be analyzed (e.g., 'review_text').

- **Success Response (200 OK):**
    - **Content-Type:** `text/plain`
    - **Body:** A confirmation message, e.g., "Model trained successfully".

- **Example Request (using cURL with optional parameters):**
  ```bash
  curl -X POST "http://localhost:8080/api/sentiment/train?datasetPath=path/to/your/data.csv&classAttr=sentiment&textAttr=review"
  ```

- **Example Request (using cURL with defaults):**
  ```bash
  curl -X POST "http://localhost:8080/api/sentiment/train"
  ```

- **Error Responses:**
    - `400 Bad Request`: If the provided parameters are invalid.
    - `500 Internal Server Error`: If an error occurs during the training process.

## Style Checking

## Testing Requirements
```bash
cd sentimentAnalyzer
mvn clean test jacoco:report  
```
To see the index.html report, open:
`target/site/jacoco/index.html`.

Rightnow, our test coverage is 69% in total with 86 unit tests.
![Jacoco Report](static/img/jacoco.png)

## External Documentation
We did not use any external third-party codes.

## Style Checking
Run the following command to check code style:
```bash
cd sentimentAnalyzer
mvn checkstyle:check
```
![Checkstyle Report](static/img/checkstyle.png)

## AI Usage
1. We used Claude Code on Copilot to determine the hyper-parameter range for the
   `gamma` in the SVM implementations.
   Prompt: what is an ideal range of gamma for SVM classifier in weka library for 3-class text classification?

2. We used mobile version of ChatGPT to discuss the potential limitations of SVM classifier
   for text classification tasks.

Prompt: What are the limitations of SVM classifier for text classification tasks?

3. We used Copilot to discuss the SVM stemmer implementation in Weka library.
   Prompt: How to use SVM stemmer in Weka library for text classification tasks?

## Notes

- **Minimum Training Data**: At least 20-30 examples per class recommended
- **Text Quality**: Clean, grammatical text performs better
- **Class Balance**: Balanced datasets improve accuracy
- **Vocabulary Size**: Larger datasets support larger vocabularies

## Contributing

To add new features:
1. Add functionality to appropriate class
2. Write comprehensive unit tests
3. Update documentation
4. Run full test suite


## Authors
- Development Team: Nexus Project Contributors - Manav, Sreenivas, Sindhu, Song
  We used the [Trello](https://trello.com/b/GtJUzHHj/nexus) to keep track of our tasks and progress.

---

# REST API Endpoints (Spring Boot)

The project now includes a full REST API for sentiment analysis and review management, built with Spring Boot and MongoDB.

## Endpoints

- `GET    /api`                      — Welcome message and endpoint list
- `GET    /api/sentiment/score?text=...` — Get sentiment score (0–5) for input text (Yet to be implemented)
- `POST   /api/users`                — Create a user
- `GET    /api/users`                — List all users
- `POST   /api/companies`            — Create a company
- `POST   /api/products`             — Create a product
- `GET    /api/products`             — List all products
- `POST   /api/products/{id}/reviews` — Post a review to a product
- `GET    /api/products/{id}/reviews` — Get all reviews for a product
- `PUT    /api/products/{id}/reviews/{reviewId}` — Update a review

## Example Usage (with Postman)

- Create a user:
    - POST `http://localhost:8080/api/users`
    - Body (JSON):
      ```json
      { "username": "manav", "email": "mm6840@columbia.edu" }
      ```
- Get all products:
    - GET `http://localhost:8080/api/products`
- Get sentiment score:
    - GET `http://localhost:8080/api/sentiment/score?text=This%20product%20is%20great` (To do)

---

# MongoDB Integration

- Uses Spring Data MongoDB for persistence.
- Connection string is configured in `src/main/resources/application.yaml`:
  ```yaml
  spring:
    data:
      mongodb:
        uri: mongodb+srv://sb5181_db_user:YOUR_PASSWORD@cluster0.85xlubi.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0
        database: sentimentDB
  server:
    port: 8080
  ```
- You can set your password directly in the YAML or use environment variables for security.

---

# Controller Unit Testing

- All controllers and services have unit tests using JUnit 5 and Mockito.
- Example test classes:
    - `UserControllerTest`
    - `ProductControllerTest`
    - `CompanyControllerTest`
    - `SentimentControllerTest`
    - `SentimentServiceTest`
    - `IndexControllerTest`
- Run all tests:
  ```powershell
  mvn test
  ```
- Tests cover:
    - Success and error cases
    - Mocked repository/service dependencies
    - Boundary conditions and input validation

  ---

  ### Create a regular user
  ```bash
  curl -X POST http://localhost:8080/api/auth/users \
    -H "Content-Type: application/json" \
    -d '{"userId": "user123"}'
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

  ### Get sentiment score for text (any exisiting authenticated user)
  ```bash
  curl -X GET "http://localhost:8080/api/sentiment/score?text=I%20love%20this%20product" \
    -H "X-User-Id: user123"
  ```

  Negative Review Example (frome ture Amazon comment):
  ```bash
  curl -X GET "http://localhost:8080/api/sentiment/score?text=I%20deeply%20regret%20purchasing%20this%20keyboard.%20It%E2%80%99s%20like%20the%20keyboard%20has%20a%20mind%20of%20its%20own%20or%20someone%20has%20hacked%20it%20and%20begins%20typing%20strange%20stuff.%20Seriously,%20really%20strange.%20At%20first%20it%20was%20wonderful%20but%20then%20all%20things%20went%20wrong.%20There%20have%20been%20several%20times%20when%20I%20was%20typing%20something%20and%20all%20was%20well,%20then%20out%20of%20nowhere%20the%20typing%20began%20to%20be%20questionable.%20For%20example,%20I%20would%20press%20the%20T%20key%20and%20a%20symbol%20or%20a%20number%20would%20appear%20instead%20of%20the%20letter%20T.%20And%20it%20would%20happen%20very%20randomly.%20One%20minute%20the%20intended%20letters%20you%20are%20typing%20are%20showing%20up%20and%20then%20less%20than%20a%20second%20later%20you%E2%80%99re%20typing%20some%20strange%20stuff.%20Sadly,%20I%20will%20be%20getting%20rid%20of%20this%20defective%20keyboard%20and%20will%20have%20to%20buy%20something%20different.%20It%E2%80%99s%20frustrating%20because%20it%20didn%E2%80%99t%20start%20happening%20until%20after%20the%2030%20day%20returned%20window.%20I%20DO%20NOT%20RECOMMEND%20PURCHASING%20THIS%20KEYBOARD%20AND%20MOUSE%20COMBO.%20Look%20elsewhere%20and%20save%20yourself%20money,%20the%20hassle%20and%20frustration.%20Ugh." \
    -H "X-User-Id: user123"
  ```

  Positive Review Example (from ture Amazon comment):
  ```bash
  curl -X GET "http://localhost:8080/api/sentiment/score?text=Works%20well,%20they%20were%20a%20bit%20thick%20for%20a%20wallet%20but%20ok%20for%20a%20key%20chain%20and%20I%20use%20them%20with%20a%20Belkin%20holder%20where%20needed.%20The%20install%20was%20perfect%20and%20quick!%20Comes%20up%20great%20when%20I%20leave%20them%20behind%20from%20the%20phone." \
    -H "X-User-Id: user123"
  ```

  Neutral Review Example:
  ```bash
  curl -X GET "http://localhost:8080/api/sentiment/score?
  text=The%20Xbox%20showed%20up%20quickly%20and%20securely%20packaged.%20It%20has%20some%20cosmetic%20damage,%20and%20was%20a%20little%20dirty,%20but%20completely%20functional.%20It%20arrived%20reset%20and%20functional%20which%20is%20about%20all%20I%27m%20concerned%20with%20when%20buying%20used%20electronics." \
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


# Sentiment Analyzer Unit Testing

- All controllers and services have unit tests using JUnit 5 and Mockito.
- Example test classes:
    - `DatasetLoaderTest`
    - `DataSplitterTest`
    - `SentimentModelTrainerTest`
    - `ScoreMapperTest`
    - `DistributionUtilsTest`
    - `SentimentStatisticsTest`
      ...
- Run individual unit test:
  ```bash
  mvn test -Dtest=DatasetLoaderTest#testLoadIllFormattedDataset
  ```
- Tests cover:
    - Edge cases for text input for the sentiment analyzer
    - Data loader and splitter so that ill-formatted csv file can be handled properly
    - Distribution tests for the mathematical details of the sentiment analysis.
- Sentiment analysis tests to validate the accuracy and performance of the model.


---
