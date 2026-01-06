FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/ci-cd-demo-1.0.0.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java","-jar","app.jar"]
