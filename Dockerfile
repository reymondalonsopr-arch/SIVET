
FROM maven:3-eclipse-temurin-26

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

EXPOSE 8080

CMD ["sh", "-c", "java -XX:MaxRAMPercentage=70.0 -Dserver.port=${PORT:-8080} -jar target/sivet-0.0.1-SNAPSHOT.jar"]
