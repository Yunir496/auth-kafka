# ---- build ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app


COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml


COPY . .


RUN mvn -q -Dmaven.test.skip=true -f auth-service/pom.xml package

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar /app/app.jar
ENV JAVA_OPTS=""
CMD ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
