package com.farmeazy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FARMEAZY APPLICATION - MAIN ENTRY POINT
 * 
 * PURPOSE: Bootstrap Spring Boot application for FarmEazy smart farm management system.
 * Initializes all Spring components, enables auto-configuration, and starts the embedded Tomcat server.
 * 
 * FUNCTIONALITY:
 * - Enables Spring Boot auto-configuration via @SpringBootApplication
 * - Scans classpath for Spring components (Controllers, Services, Repositories, Configurations)
 * - Loads application.properties configuration file
 * - Starts embedded Tomcat server on port 8080 (configurable via server.port)
 * - Initializes database connection pool (HikariCP)
 * - Sets up Spring Security and JWT authentication
 * - Loads Spring Data JPA for ORM
 * - Enables Spring Web MVC for REST API
 * - Enables Scheduled tasks for payout processing
 * 
 * STARTUP SEQUENCE:
 * 1. JVM executes main() method with Spring Boot launcher
 * 2. SpringApplication.run() creates Spring ApplicationContext
 * 3. Component scanning finds @Configuration, @Service, @Repository, @Controller classes
 * 4. Dependency injection wires all beans and their dependencies
 * 5. Database migrations/schema validation (via Hibernate)
 * 6. Application context initialization listeners execute
 * 7. Embedded Tomcat server starts listening on port 8080
 * 8. Application ready logs "Started FarmEazyApplication in X seconds"
 * 
 * CONFIGURATION FILES LOADED:
 * - src/main/resources/application.properties (main configuration)
 * - src/main/resources/application-h2.properties (H2 database profile)
 * - pom.xml (Maven dependencies: Spring Boot 3.2.0, Java 17)
 * 
 * REQUIRED SERVICES:
 * - MySQL 8.0 database on localhost:3306 (configured in application.properties)
 * - Alternatively: H2 embedded database (default, file-based at ./data/farmeazy_db.mv.db)
 * 
 * ENVIRONMENT REQUIREMENTS:
 * - Java 17+ (target version in pom.xml)
 * - Maven 3.6+ (for building)
 * - Port 8080 available (default server port)
 * - Sufficient disk space for H2 database file (~50MB)
 * 
 * ERROR HANDLING:
 * - If database connection fails: Application will not start (fail-fast)
 * - If properties file missing: Uses hardcoded defaults from @SpringBootApplication
 * - If component scan fails: Startup fails with detailed bean wiring error messages
 * 
 * SHUTDOWN:
 * - Graceful shutdown: Spring Boot closes all beans and DB connections
 * - Database connections pooled (HikariCP waits for active queries)
 * - H2 database checkpoint written before closing (data persistence)
 * 
 * @author FarmEazy Development Team
 * @version 1.0.0
 * @since January 2026
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EntityScan(basePackages = {"com.farmeazy.entity"})
public class FarmEazyApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmEazyApplication.class, args);
    }
}
