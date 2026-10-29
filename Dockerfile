FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/vet-clinic-api-0.0.1-SNAPSHOT.jar app.jar
ENV PORT=8080
EXPOSE 8080
# JVM flags must come before -jar; otherwise they are passed to the application as arguments.
ENTRYPOINT ["sh", "-c", "exec java -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -jar /app.jar --server.port=${PORT}"]
