# ---- build ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY mailer-native/pom.xml mailer-native/pom.xml
COPY . .

RUN mvn -q -Dmaven.test.skip=true -f mailer-native/pom.xml package

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/mailer-native/target/mailer-native-0.0.1-SNAPSHOT.jar /app/app.jar
CMD ["java","-jar","/app/app.jar"]
