FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Version que expone GET /version; van al final para no invalidar la cache del build de Maven
ARG GIT_COMMIT=
ARG BUILD_DATE=
ENV APP_VERSION_COMMIT=${GIT_COMMIT} \
    APP_VERSION_FECHA=${BUILD_DATE}
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
