# Sentiment Analyzer - Multi-Class Sentiment Analysis Product Review Service

## 📋 Service Overview

A Java-based sentiment analysis service using Weka's machine learning library. The system performs multi-class sentiment classification on review text data. This service that provides review analysis based on the reviews. It further supports sentiment and statistical analysis on reviews for a product, and provides these results to a client (i.e. a company) or a customer (a shopper considering buying the product). 

Clients, such as product-based websites, can use our service to predict their potential customers’ future shopping decisions based on the past reviews, or use the data generated to compare their promoted similar products with their competitors. Optionally, the service may also take into account images attached in reviews and compare them with the company-provided image of the product in order to report whether the portrayal of the product is accurate. This review analysis can be provided as an analytics dashboard, which displays user sentiment towards the product.

This README outlines the part of the service functionality that is closely related to the sentiment analysis. Please refer to other parts of the Nexus project for details on the full service, including the database storage, API usages, etc.

# Nexus — Sentiment & Review Analytics (Java / Weka)

## Part 1 — Team

- Team Name: Nexus
- GitHub Repo: https://github.com/manavmunjal/Nexus

Team members:

- Song Li — GitHub: `SongTonyLi`
- Manav Munjal — GitHub: `manavmunjal`
- Sindhu Krishnamurthy — GitHub: `sk4699`
- Sreenivas Karthik Bandi — GitHub: `sreeni-kar`

## Part 2 — Project Vision & Service Description

We are building a review analytics service that performs NLP-driven sentiment and statistical analysis on product reviews. The service ingests review text (and optionally review images), vectorizes the reviews (TF-IDF) and runs a multi-class Naive Bayes classifier to assign sentiment labels and numeric sentiment scores.

Core capabilities:

- Assign sentiment labels and compute an expected numeric sentiment score per review
- Aggregate results by product and company and compare distributions (proportions of positive/neutral/negative)
- Compute statistical metrics per group: mean, variance, standard deviation, skewness
- Compare distributions between products/companies using symmetric KL-divergence (with smoothing)
- Optionally compare review images against company-provided product images (future/optional)

Deliverables for clients:

- Programmatic API and/or analytics dashboard that shows sentiment distributions, statistical summaries, and similarity comparisons between competing products
- Per-review outputs (label, probability distribution over classes, expected sentiment score)

Typical workflow:

1. Ingest labeled or unlabeled reviews (CSV / database / scraped)
2. Preprocess and vectorize text (TF-IDF)
3. Train or load a Naive Bayes model
4. Produce per-review predictions and aggregate statistics
5. Offer visualizations and numeric comparisons for decision-making

## Part 3 — Potential Clients & Use Cases

1. Company Analytics Tools — e.g., product teams at brands (Nike, Samsung) can import sentiment summaries to inform marketing and product decisions.
2. Consumer Review Aggregators — third-party platforms or shoppers: scrape reviews (Amazon, Walmart, eBay) and provide an aggregated sentiment score to help shoppers decide.
3. Public Dashboard (optional) — a hosted analytics dashboard that surfaces trending sentiment, comparative KL-divergence, and product summaries.

## Part 4 — Technologies / Tools (end-to-end)

Primary stack used for this repository and the planned service:

- Java 17 — core language
- Weka — TF-IDF, Naive Bayes pipeline
- Spring Boot — backend services (suggested / planned)
- MongoDB — data storage (suggested)
- Maven — build and dependency management
- JUnit (JUnit Jupiter) — unit testing
- Mockito — mocking in tests (planned)
- JaCoCo — coverage
- CheckStyle / PMD — static analysis
- Postman — API testing
- GitHub Actions — CI workflows
- Trello — project management

## Project layout (summary)

The repository under `sentimentAnalyzer/` contains a small Weka-based pipeline and tests. Key files include:

- `src/main/java/com/nexus/sentiment/` — core Java classes (see code for details)
- `src/main/resources/data/sample_reviews.csv` — small sample dataset
- `src/test/java/com/nexus/sentiment/` — unit tests (NLP-heavy tests included)
- `pom.xml` — Maven configuration and dependencies

## Getting started — quick commands

Prerequisites: Java 17+, Maven

Install dependencies and compile:

```bash
cd sentimentAnalyzer
mvn dependency:resolve
mvn clean compile
```

Run the application (uses `src/main/resources/data/sample_reviews.csv` by default):

```bash
mvn exec:java
```

Run with a custom CSV file (first arg):

```bash
mvn exec:java -Dexec.args="path/to/your/reviews.csv"
```

Build a runnable jar:

```bash
mvn package
java -cp target/sentiment-analyzer-1.0.0.jar com.nexus.sentiment.Main
```

CSV format expected (example):

```csv
review_id,company,product,review_text,sentiment_label
1,Acme,AcmePhone,"Battery life is outstanding!",positive
2,Acme,AcmePhone,"Screen cracked easily.",negative
3,Acme,AcmeTablet,"Average device overall.",neutral
```

## Tests

Run full test suite:

```bash
mvn test
```

Run an individual test class:

```bash
mvn test -Dtest=SentimentModelTrainerTest
```
```bash
mvn test -Dtest=SentimentModelTrainerTest
```

Notes about tests:

- The repository includes a comprehensive NLP-focused test class (`SentimentModelTrainerTest`) that stresses tokenization, TF-IDF, stemming and edge cases (empty/long text, unicode, punctuation).
- For best results in NLP model tests, ensure training/test data is representative and not extremely small; Weka classifiers require appropriate attribute types and sufficient examples.