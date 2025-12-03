# Deploying the Sentiment Analyzer to Google Cloud Platform

This document outlines the steps to deploy the Sentiment Analyzer application to Google Cloud Run.

## 1. Initial Setup and Dockerization

The first step is to containerize the application using Docker.

### Create a `Dockerfile`

A `Dockerfile` is created to define the build and runtime environment for the application. Already existed.

```dockerfile
# Use a Maven image to build the application
FROM maven:3.8.5-openjdk-17 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the application code
COPY src ./src

# Build the application
RUN mvn clean install

# Use a smaller OpenJDK image to run the application
FROM eclipse-temurin:17-jre-focal

# Set the working directory
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/sentiment-analyzer-1.0.0.jar .

# Expose the port the application runs on (default for Spring Boot is 8080)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "sentiment-analyzer-1.0.0.jar"]
```

## 2. Build and Push the Docker Image

### Build the Image

Build the Docker image using the `docker build` command. Replace `[PROJECT-ID]` with your Google Cloud project ID. Remember to run this command in the directory where the `Dockerfile` is located, and your docker desktop is running.
```bash
docker build -t gcr.io/nexus-479916/sentiment-analyzer:latest .
```

**Initial Build Failure:**

An initial attempt to build the image failed because the `openjdk:17-slim` base image could not be found.

```bash
ERROR: failed to build: failed to solve: openjdk:17-slim: failed to resolve source metadata for docker.io/library/openjdk:17-slim: docker.io/library/openjdk:17-slim: not found
```

The `Dockerfile` was updated to use `eclipse-temurin:17-jre-focal` as the base image.

**Successful Build:**

```bash
$ docker build -t gcr.io/nexus-479916/sentiment-analyzer:latest .
[+] Building 149.0s (15/15) FINISHED
 => => naming to gcr.io/nexus-479916/sentiment-analyzer:latest
```

### Authenticate Docker

To push the image to Google Container Registry (GCR), you need to authenticate Docker with your Google Cloud credentials.

```bash
gcloud auth configure-docker
```

### Push the Image to GCR

Push the built image to GCR.

```bash
$ docker push gcr.io/nexus-479916/sentiment-analyzer:latest
The push refers to repository [gcr.io/nexus-479916/sentiment-analyzer]
latest: digest: sha256:7c2dd23306e52629b8bc6afff38f608f7eaff19999407be563f239f6f0c0553e size: 856
```

## 3. Deploy to Google Cloud Run

### Initial Deployment Failure

The first deployment attempt failed due to an architecture incompatibility.

```bash
$ gcloud run deploy sentiment-analyzer-service --image gcr.io/nexus-479916/sentiment-analyzer:latest --platform managed --region us-central1 --allow-unauthenticated --project nexus-479916
Deployment failed
ERROR: (gcloud.run.deploy) Cloud Run does not support image 'gcr.io/nexus-479916/sentiment-analyzer:latest': Container manifest type 'application/vnd.oci.image.index.v1+json' must support amd64/linux.
```

### Rebuild for `linux/amd64`

The Docker image was rebuilt with the `--platform linux/amd64` flag to ensure compatibility with Cloud Run.

```bash
$ docker build --platform linux/amd64 -t gcr.io/nexus-479916/sentiment-analyzer:latest .
[+] Building 181.0s (17/17) FINISHED
 => => naming to gcr.io/nexus-479916/sentiment-analyzer:latest
```

The new image was then pushed to GCR.

### Successful Deployment

The application was successfully deployed to Cloud Run.

```bash
$ gcloud run deploy sentiment-analyzer-service --image gcr.io/nexus-479916/sentiment-analyzer:latest --platform managed --region us-central1 --allow-unauthenticated --project nexus-479916
Done.
Service [sentiment-analyzer-service] revision [sentiment-analyzer-service-00002-jhr] has been deployed and is serving 100 percent of traffic.
Service URL: https://sentiment-analyzer-service-321275563168.us-central1.run.app
```

## 4. Handle Database Credentials

After deployment, the application threw a database authentication error.

```bash
Database error while fetching company: Exception authenticating MongoCredential{mechanism=SCRAM-SHA-1, userName='sb5181_db_user', source='admin', password=, mechanismProperties=}
```

The MongoDB password was stored in Google Secret Manager and made available to the Cloud Run service as an environment variable.

### Store a MongoDB Password
Replace the placeholder `....` with the actual MongoDB password.
```bash
$ echo -n "...." | gcloud secrets create mongodb-password --data-file=- --project=nexus-479916
Created version [1] of the secret [mongodb-password].
```

### Grant Cloud Run Access to the Secret
This step allows the Cloud Run service to access the secret, and the external users from internet to access the service.
```bash
$ gcloud secrets add-iam-policy-binding mongodb-password --member="serviceAccount:321275563168-compute@developer.gserviceaccount.com" --role="roles/secretmanager.secretAccessor" --project=nexus-479916
Updated IAM policy for secret [mongodb-password].
```

### Redeploy with the Secret

The Cloud Run service was redeployed with the secret mounted as the `MONGODB_PASSWORD` environment variable.

```bash
$ gcloud run deploy sentiment-analyzer-service --image gcr.io/nexus-479916/sentiment-analyzer:latest --platform managed --region us-central1 --allow-unauthenticated --update-secrets=MONGODB_PASSWORD=mongodb-password:latest --project nexus-479916
Done.
Service [sentiment-analyzer-service] revision [sentiment-analyzer-service-00003-knt] has been deployed and is serving 100 percent of traffic.
Service URL: https://sentiment-analyzer-service-321275563168.us-central1.run.app
```

## 5. Access the Service

The deployed service can be accessed on the Google Cloud Console via the [URL](https://console.cloud.google.com/run/detail/us-central1/sentiment-analyzer-service/revisions?project=nexus-479916)
