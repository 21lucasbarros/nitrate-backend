# --- build: compila o jar com Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# baixa as dependências primeiro: essa camada só é refeita quando o pom.xml muda
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# os testes de integração precisam do banco; os unitários rodam no CI/local
RUN mvn -B -q package -DskipTests

# --- runtime: só a JRE e o jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 nitrate
COPY --from=build /app/target/nitrate-*.jar app.jar
USER nitrate

ENV SPRING_PROFILES_ACTIVE=prod
# usa até 75% da memória do container para o heap (o grafo fica em memória)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
