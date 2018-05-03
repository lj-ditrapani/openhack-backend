FROM openjdk:8-jre-alpine
RUN mkdir -p /opt/app
WORKDIR /opt/app
COPY ./target/scala-2.12/openhackbackend-assembly-0.0.1.jar .

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/app/openhackbackend-assembly-0.0.1.jar"]
