FROM eclipse-temurin:21-jdk-alpine
#VOLUME /tmp
COPY releases/latest/*.jar app.jar
EXPOSE ${PORT}
ENTRYPOINT ["java","-jar","/app.jar","-Djava.net.preferIPv4Stack=true","-Djava.net.preferIPv6Addresses=false"]
