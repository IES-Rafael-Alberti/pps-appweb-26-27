FROM eclipse-temurin:21-jdk-alpine
#VOLUME /tmp
ARG LATEST_VERSION=$(curl -s https://api.github.com/repos/IES-Rafael-Alberti/pps-appweb-26-27/releases/latest | grep "tag_name" | cut -d'v' -f2 | cut -d'"' -f1)
COPY /releases/download/v0.0.1/vet-clinic-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE ${PORT}
ENTRYPOINT ["java","-jar","/app.jar","-Djava.net.preferIPv4Stack=true","-Djava.net.preferIPv6Addresses=false"]
