FROM node:24-alpine AS frontend
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM gradle:8.14.6-jdk21-alpine AS application-build
WORKDIR /workspace
COPY --from=frontend /workspace/frontend/dist ./frontend/dist
COPY . .
RUN gradle bootJar --no-daemon -x test -PskipFrontendBuild

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S campushub && adduser -S -G campushub campushub
COPY --from=application-build /workspace/build/libs/campus-hub-events-0.1.0.jar /app/app.jar
USER campushub
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 CMD wget -q -O - http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-Xms32m", "-Xmx192m", "-XX:MaxMetaspaceSize=128m", "-jar", "/app/app.jar"]
