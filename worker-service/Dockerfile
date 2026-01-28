# Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jdk
# Using JDK because we need tools potentially or to support Semgrep/Python environment better?
# Actually Semgrep needs Python. Alpine can be tricky for Python/Pip sometimes due to musl/glibc.
# Steps say: use eclipse-temurin:21-jdk and install git python3 python3-pip

RUN apt-get update && apt-get install -y git python3 python3-pip && rm -rf /var/lib/apt/lists/*
# Install Semgrep
RUN pip3 install semgrep --break-system-packages

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create temp directory
RUN mkdir -p /tmp/devsecwatch

ENTRYPOINT ["java", "-jar", "app.jar"]
