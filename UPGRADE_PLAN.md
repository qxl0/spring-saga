# Spring Boot Upgrade Plan: 2.5.x → 3.4.x

## Current State

| Service | Spring Boot | Java | Key Dependencies |
|---------|-------------|------|------------------|
| CommonService | 2.5.5 | 11 | Axon 4.5.3 |
| OrderService | 2.5.5 | 11 | Axon 4.5.3, H2, JPA, Guava 31 |
| ProductService | 2.5.4 | 11 | Axon 4.5.3, H2, JPA, Guava 30 |
| PaymentService | 2.5.5 | 11 | Axon 4.5.3, H2, JPA, Guava 31 |
| ShipmentService | 2.5.5 | 11 | Axon 4.5.3, H2, JPA, Guava 31 |
| UserService | 2.5.5 | 11 | Axon 4.5.3, H2, JPA, Guava 31 |

## Version Targets

| Component | From | To |
|-----------|------|----|
| Spring Boot | 2.5.4 / 2.5.5 | **3.4.1** |
| Java | 11 | **21** |
| Axon Framework | 4.5.3 | **4.10.x** |
| Guava | 30.1.1 / 31.0.1 | **33.4.0-jre** |
| Hibernate | 5.x (auto) | **6.x (auto)** |
| Jakarta EE | javax.* | **jakarta.*** |

---

## Phase 1 — Prerequisites

### Step 1.1 — Upgrade Java from 11 to 21

Spring Boot 3.x requires Java 17 minimum. Java 21 (LTS) is recommended.

- Install JDK 21
- Update `JAVA_HOME` in your environment
- Update `<java.version>` in all 6 `pom.xml` files:

```xml
<java.version>21</java.version>
```

Affected files:
- `CommonService/pom.xml`
- `OrderService/pom.xml`
- `ProductService/pom.xml`
- `PaymentService/pom.xml`
- `ShipmentService/pom.xml`
- `UserService/pom.xml`

---

## Phase 2 — Intermediate Upgrade (2.5.x → 2.7.x)

Jumping directly to 3.x is risky. Go via 2.7.x first to surface deprecations early.

### Step 2.1 — Bump Spring Boot to 2.7.18 in all pom.xml files

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
    <relativePath/>
</parent>
```

### Step 2.2 — Fix H2 URL format

If any `application.properties` uses an H2 datasource URL, update it:

```properties
# Before
spring.datasource.url=jdbc:h2:mem:testdb

# After
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

### Step 2.3 — Upgrade Axon to 4.6.x

```xml
<dependency>
    <groupId>org.axonframework</groupId>
    <artifactId>axon-spring-boot-starter</artifactId>
    <version>4.6.3</version>
</dependency>
```

### Step 2.4 — Build all services and fix deprecation warnings

Build in dependency order (CommonService first, then dependents):

```bash
cd CommonService && mvn clean install
cd ../ProductService && mvn clean install
cd ../OrderService && mvn clean install
cd ../PaymentService && mvn clean install
cd ../ShipmentService && mvn clean install
cd ../UserService && mvn clean install
```

Address all deprecation warnings now — they become compile errors in Spring Boot 3.

---

## Phase 3 — Major Upgrade (2.7.x → 3.4.x)

### Step 3.1 — Bump Spring Boot to 3.4.1 in all pom.xml files

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.1</version>
    <relativePath/>
</parent>
```

### Step 3.2 — Migrate javax.* to jakarta.* (biggest change)

Spring Boot 3 uses Jakarta EE 10. Every `javax.*` import must be renamed:

| Old Package | New Package |
|-------------|-------------|
| `javax.persistence.*` | `jakarta.persistence.*` |
| `javax.validation.*` | `jakarta.validation.*` |
| `javax.servlet.*` | `jakarta.servlet.*` |
| `javax.transaction.*` | `jakarta.transaction.*` |

Find all affected files first (PowerShell):

```powershell
Get-ChildItem -Recurse -Filter "*.java" | Select-String "import javax\." | Select-Object Path -Unique
```

Then do a global find & replace in your IDE across all service `src/` directories:
- Find: `import javax.`
- Replace: `import jakarta.`

### Step 3.3 — Upgrade Axon to 4.10.x

Axon 4.9+ added Spring Boot 3 compatibility.

```xml
<dependency>
    <groupId>org.axonframework</groupId>
    <artifactId>axon-spring-boot-starter</artifactId>
    <version>4.10.3</version>
</dependency>
```

Review Axon migration notes for 4.5 → 4.10:
- Deprecated `AggregateLifecycle` method signatures may have changed
- Event handler configuration API updates
- Check https://docs.axoniq.io for the full migration guide

### Step 3.4 — Upgrade and align Guava across all services

ProductService is on 30.1.1, all others on 31.0.1. Align everyone to latest:

```xml
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>33.4.0-jre</version>
</dependency>
```

### Step 3.5 — Handle Hibernate 6 breaking changes

Spring Boot 3 upgrades Hibernate from 5.x to 6.x. Key breaking changes:

- `@Type(type="...")` → `@Type(value = ...)` (annotation attribute renamed)
- `CriteriaQuery` API has changes — review any custom criteria queries
- Native SQL queries may need review for dialect differences
- H2 dialect is auto-detected differently — remove explicit dialect config if set:
  ```properties
  # Remove this if present — Spring Boot 3 auto-detects it
  spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
  ```

### Step 3.6 — Review application.properties for renamed keys

Many property keys were renamed in Spring Boot 3. Common ones to check:

```properties
# Spring Boot 3: enable RFC 7807 problem details (new feature)
spring.mvc.problemdetails.enabled=true

# H2 console (unchanged, but verify)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA (unchanged)
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=create-drop
```

Run the Spring Boot migrator to detect all renamed properties:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.config.import=optional:configtree:/etc/config/
```

Or use IntelliJ's built-in Spring Boot migration inspection.

### Step 3.7 — Fix Spring Security (if used)

Spring Security 6 (bundled with Spring Boot 3) removed `WebSecurityConfigurerAdapter`.
If any service uses Spring Security, replace the old pattern:

```java
// Old (Spring Boot 2.x) - REMOVED in 3.x
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception { ... }
}

// New (Spring Boot 3.x)
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/**").authenticated()  // antMatchers → requestMatchers
            .anyRequest().permitAll());
        return http.build();
    }
}
```

---

## Phase 4 — Build and Verify

### Step 4.1 — Build CommonService first (no inter-service dependencies)

```bash
cd CommonService && mvn clean install
```

### Step 4.2 — Build all dependent services

```bash
cd ../ProductService && mvn clean install
cd ../OrderService && mvn clean install
cd ../PaymentService && mvn clean install
cd ../ShipmentService && mvn clean install
cd ../UserService && mvn clean install
```

### Step 4.3 — Run all tests

```bash
# From each service directory
mvn test
```

### Step 4.4 — Start each service and verify startup

Check logs for:
- No `javax` import errors (ClassNotFoundException)
- Axon connects to event store successfully
- JPA schema created correctly (H2 in-memory)
- No `BeanCreationException` on startup

---

## Phase 5 — Cleanup

### Step 5.1 — Remove migration workarounds

- Delete any `@SuppressWarnings("deprecation")` added during migration
- Remove any compatibility shims no longer needed

### Step 5.2 — Enable Spring Boot 3 features

```properties
# application.properties — enable RFC 7807 problem details
spring.mvc.problemdetails.enabled=true
```

### Step 5.3 — Final dependency audit

Run dependency analysis to check for outdated or conflicting transitive deps:

```bash
mvn dependency:analyze
mvn versions:display-dependency-updates
```

---

## Key Risk: Axon Framework Compatibility

The **most uncertain dependency** is Axon Framework. Axon 4.9+ claims Spring Boot 3
support, but the CQRS/event sourcing configuration API changed significantly between 4.5
and 4.10. Verify compatibility at https://docs.axoniq.io before starting Phase 3.

If Axon 4.10.x has blocking incompatibilities, consider upgrading to Axon 5.x (if
available and stable) which is designed for Spring Boot 3 from the ground up.

---

## Build Order Reference

Always build in this order (CommonService has no local deps, others depend on it):

1. `CommonService`
2. `ProductService` (no CommonService dependency)
3. `OrderService`, `PaymentService`, `ShipmentService`, `UserService` (all depend on CommonService)
