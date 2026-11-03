# Dev: roda com spring-boot:run (o código é montado via volume, devtools reinicia ao recompilar)
FROM maven:3.9-eclipse-temurin-25 AS dev

WORKDIR /app

EXPOSE 8080

CMD ["mvn", "spring-boot:run"]


# Build
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copia dependências primeiro para aproveitar o cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o código
COPY src ./src

# Gera o JAR
RUN mvn clean package -DskipTests


# Runtime
FROM eclipse-temurin:25-jre

WORKDIR /app

RUN useradd --system --uid 1001 spring

COPY --from=build /app/target/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
