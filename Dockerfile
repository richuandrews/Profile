FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/richuandrewsprofile.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]