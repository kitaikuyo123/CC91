FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache tzdata
ENV LANG=en_US.UTF-8
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
