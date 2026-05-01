FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY mvnw mvnw.cmd ./
COPY .mvn/wrapper/ .mvn/wrapper/
COPY pom.xml .
COPY src src
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8880
ENTRYPOINT ["java", "-jar", "app.jar"]