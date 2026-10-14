package com.java25.sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/// Boots the whole application against PostgreSQL: Flyway migrates, then Hibernate validates the
/// schema, including the named unique key and index.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
    properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.example.test")
@Testcontainers(disabledWithoutDocker = true)
class Java25SampleApplicationTests {

  @Test
  void contextLoads() {
    // Startup itself is the assertion: schema validation fails the context on any mismatch.
  }
}
