# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY domain ./domain
COPY runtime ./runtime
COPY server ./server

RUN mvn -pl server -am package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# netcat-openbsd is used by the Docker health check (nc -z localhost 5555)
RUN apt-get update && \
    apt-get install -y --no-install-recommends netcat-openbsd && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/server/target/*.jar /app/app.jar

EXPOSE 5555

CMD ["java", "-jar", "/app/app.jar"]