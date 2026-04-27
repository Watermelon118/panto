# Build stage:
# Use Java 21 JDK to compile the Spring Boot application into a runnable jar.
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

# Copy Maven Wrapper and pom.xml first so Docker can cache dependency downloads
# when only application source files change.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests dependency:go-offline

# Copy source code and build the application jar.
COPY src src
RUN ./mvnw -B -DskipTests clean package

# Runtime stage:
# Use a smaller Java 21 JRE image. The final image does not contain Maven,
# source code, or the full JDK toolchain.
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Run the API as a dedicated non-root user inside the container.
RUN groupadd --system panto \
    && useradd --system --gid panto --home-dir /app --shell /usr/sbin/nologin panto

# Keep runtime defaults non-secret. Real database credentials and JWT secrets
# must be provided by docker-compose, ECS, or the server environment.
ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=prod

COPY --from=build --chown=panto:panto /workspace/target/*.jar app.jar

EXPOSE 8080

USER panto

# Use sh -c so JAVA_OPTS can be expanded at container runtime.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
