FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY gateway/pom.xml gateway/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
RUN mvn dependency:go-offline || true

COPY . .
RUN mvn package -pl order-service -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
