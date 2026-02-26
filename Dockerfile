# ============================================================================
# DOCKERFILE FOR FARMEAZY BACKEND SPRING BOOT APPLICATION
# ============================================================================
#
# Purpose: Build and package FarmEazy backend for production deployment
# Target Platform: Any Docker-compatible container runtime
# Deployment Options: Docker, Kubernetes, Cloud Run, Supabase, etc
#
# This Dockerfile uses MULTI-STAGE BUILD pattern:
# Stage 1: Builder - Compile Java source code using Maven
# Stage 2: Runtime - Run compiled application with minimal dependencies
#
# Benefits:
# - Smaller final image (only runtime dependencies, no build tools)
# - Faster image pulls (less data to download)
# - More secure (no Java compiler/source code in production)
# - Better caching (separate layers for each stage)
#
# ============================================================================

# ============================================================================
# STAGE 1: BUILDER
# ============================================================================
# Base Image: maven:3.9-eclipse-temurin-17
# Contains:
#   - Maven 3.9 (build tool for Java)
#   - Eclipse Temurin JDK 17 (Java compiler)
#   - Linux Alpine OS (minimal, ~150MB)
#
# Purpose: Compile Java source code into runnable JAR file
#

FROM maven:3.9-eclipse-temurin-17 AS builder

# Set working directory inside container
# All subsequent commands run from /app
WORKDIR /app

# Copy pom.xml to container
# pom.xml contains:
#   - Project metadata (name, version, etc)
#   - Dependencies list (Spring Boot, PostgreSQL driver, JWT, etc)
#   - Build configuration (compiler settings, plugins)
#
# Why copy first (before source code)?
# Docker caching! If source changes but pom.xml doesn't, use cached layer
# Maven installs dependencies → if no cache, this takes 2-3 minutes
# If pom.xml unchanged, Docker uses cached dependency layer → 2 seconds
#
COPY pom.xml .

# Download all dependencies to local Maven cache
# Command: mvn dependency:go-offline
# This:
#   - Reads pom.xml
#   - Downloads all required JAR files
#   - Stores in ~/.m2/repository
#   - Works offline later (useful in isolated environments)
#
# Example dependencies downloaded:
#   - spring-boot-starter-web-3.2.0.jar
#   - postgresql-42.7.0.jar
#   - jjwt-0.12.3.jar
#   - And 100+ transitive dependencies
#
RUN mvn dependency:go-offline

# Copy source code to container
# Copies entire src/ directory containing:
#   - src/main/java/com/farmeazy/controller/*.java (API endpoints)
#   - src/main/java/com/farmeazy/service/*.java (business logic)
#   - src/main/java/com/farmeazy/entity/*.java (database models)
#   - src/main/java/com/farmeazy/security/*.java (JWT, auth)
#   - src/main/resources/*.properties (configuration)
#
# Now we have:
#   - pom.xml
#   - src/
#   - /app/ can be compiled
#
COPY src/ src/

# Build the application
# Command: mvn clean package -DskipTests
#
# What it does:
#   - clean: Remove old build artifacts
#   - package: Compile source → create JAR
#   - -DskipTests: Don't run unit tests (saves time in Docker)
#
# Output created:
#   - target/farmeazy-backend-1.0.0.jar
#   - This is the deployable application
#
# Process:
# 1. Compile all .java files → .class files
# 2. Run Spring Boot repackaging (makes executable JAR)
# 3. Embed tomcat server, all dependencies inside JAR
# 4. Result: Single ~100MB JAR file with everything needed
#
RUN mvn clean package -DskipTests

# ============================================================================
# STAGE 2: RUNTIME
# ============================================================================
# Base Image: eclipse-temurin:17-jdk-alpine
# Contains:
#   - Eclipse Temurin JDK 17 (Java runtime)
#   - Alpine Linux (minimal OS, ~50MB)
#
# Purpose: Run the compiled JAR in production
# Size: ~200-250MB total (vs ~600MB with build tools)
#

FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy compiled JAR from builder stage
# From: builder stage → /app/target/farmeazy-backend-1.0.0.jar
# To: runtime container → /app/app.jar
#
# This is the final executable JAR containing:
#   - All Java compiled classes
#   - All dependencies (Spring Boot, PostgreSQL driver, etc)
#   - Embedded Tomcat web server
#   - Application configuration
#
# It's a complete, self-contained application
#
COPY --from=builder /app/target/farmeazy-backend-1.0.0.jar app.jar

# Expose port 8080
# Documentation: This container will listen on port 8080
# Docker/Kubernetes can see this and know to forward traffic to this port
# Note: Doesn't actually open port; just documents intention
EXPOSE 8080

# ============================================================================
# ENVIRONMENT VARIABLES FOR SUPABASE
# ============================================================================
# These variables configure the runtime behavior
# Can be overridden when starting container:
#   docker run -e DB_PASSWORD=xyz -e JWT_SECRET=abc ...
#
# Or set in:
#   - Docker Compose
#   - Kubernetes ConfigMap/Secret
#   - Cloud platform (Supabase, Cloud Run, etc)
#

# Set Spring profile to "supabase"
# This tells Spring Boot to use application-supabase.properties
# Instead of default application.properties (which uses H2)
ENV SPRING_PROFILES_ACTIVE=supabase

# Database password placeholder
# Value set at runtime (not hardcoded for security)
# Example: docker run -e DB_PASSWORD="your-supabase-password" ...
ENV DB_PASSWORD=${DB_PASSWORD}

# JWT secret for token signing
# Value set at runtime
# Example: docker run -e JWT_SECRET="your-secret-key" ...
# Generate with: openssl rand -base64 32
ENV JWT_SECRET=${JWT_SECRET}

# ============================================================================
# ENTRY POINT
# ============================================================================
# Command executed when container starts
# Syntax: ENTRYPOINT ["executable", "param1", "param2", ...]
# This runs: java -jar app.jar
#
# What happens:
# 1. JVM starts (Java Virtual Machine)
# 2. Tomcat embedded web server initializes
# 3. Spring Boot context loads
# 4. Application connects to PostgreSQL (using application-supabase.properties)
# 5. REST API available at: http://container-ip:8080/api/
#
# Logs appear in:
#   - docker logs [container-name/id]
#   - Cloud platform logs (Supabase, Cloud Run, etc)
#
ENTRYPOINT ["java", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]

# ============================================================================
# USAGE EXAMPLES
# ============================================================================
#
# 1. Build image locally
#    docker build -t farmeazy-backend:latest .
#
# 2. Run container locally (with environment variables)
#    docker run \
#      -p 8080:8080 \
#      -e DB_PASSWORD="your-password" \
#      -e JWT_SECRET="your-secret" \
#      -e spring.datasource.url="jdbc:postgresql://db.xxx.supabase.co:5432/postgres" \
#      farmeazy-backend:latest
#
# 3. Push to Docker Hub
#    docker tag farmeazy-backend:latest username/farmeazy-backend:latest
#    docker push username/farmeazy-backend:latest
#
# 4. Deploy to Supabase Docker Registry
#    docker tag farmeazy-backend:latest registry.supabase.com/your-project/farmeazy-backend:latest
#    docker push registry.supabase.com/your-project/farmeazy-backend:latest
#
# 5. Check logs
#    docker logs [container-id]
#    docker logs -f [container-id]  # Follow logs
#
# 6. Access API
#    curl http://localhost:8080/api/swagger-ui.html
#    curl http://localhost:8080/api/v3/api-docs
#
# ============================================================================
