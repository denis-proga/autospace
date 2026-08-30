FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle :server:installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/server/build/install/server ./
EXPOSE 8080
CMD ["./bin/server"]