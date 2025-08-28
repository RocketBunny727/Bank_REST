FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/bank_db
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=123456
ENV ENCRYPT_KEY=wWvhTBPCjgYyAxWZj2SPTvXqw8eu2S37oss0ESyNAIUPeNW1K9gLMwzyJhXrzNip3Ilh00A4bq8TZcciHFzodQ
ENV JWT_SECRET=QEIZXTVAsW2+gw630r+0syqB3csEEUk/fdBY624U51eIpWXAXGenivNJCtPFNlpbMCDO+vPDlJ3X/TrFTH+Msg
ENV JWT_EXPIRATION=3600000

ENTRYPOINT ["java", "-jar", "app.jar"]