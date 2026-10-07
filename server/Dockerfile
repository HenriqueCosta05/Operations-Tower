FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -DskipTests -Dspotless.check.skip=true package \
 && java -Djarmode=tools -jar target/*.jar extract --layers --destination target/layers

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /app/target/layers/dependencies/ ./
COPY --from=build /app/target/layers/spring-boot-loader/ ./
COPY --from=build /app/target/layers/snapshot-dependencies/ ./
COPY --from=build /app/target/layers/application/ ./
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "exec java -jar /app/server-0.0.1-SNAPSHOT.jar"]
