FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache tzdata
ENV LANG=en_US.UTF-8
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
