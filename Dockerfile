FROM eclipse-temurin:17-jdk

WORKDIR /app

RUN apt-get update && apt-get install -y maven

COPY . .

RUN mvn clean package -DskipTests

EXPOSE 9090

CMD ["sh", "-c", "java -jar target/*.jar"]