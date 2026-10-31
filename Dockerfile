FROM eclipse-temurin:21-jdk-alpine
ADD https://github.com/IES-Rafael-Alberti/pps-appweb-26-27/releases/download/v0.0.1/vet-clinic-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE ${PORT}
ENTRYPOINT ["java","-jar","/app.jar","-Djava.net.preferIPv4Stack=true","-Djava.net.preferIPv6Addresses=false"]
