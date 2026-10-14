package com.java25.sample.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.java25.sample.TestcontainersConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class CustomerRepositoryTest {

  private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

  @Autowired private CustomerRepository customers;

  @Test
  void findsSummariesByStatus() {
    customers.saveAndFlush(new Customer("Ada", "ada@example.com", NOW));

    var page = customers.findByStatus(CustomerStatus.ACTIVE, PageRequest.of(0, 10));

    assertThat(page.getContent()).extracting(CustomerSummary::email).contains("ada@example.com");
  }

  @Test
  void duplicateEmailViolatesUniqueConstraint() {
    customers.saveAndFlush(new Customer("Ada", "dup@example.com", NOW));

    assertThatThrownBy(() -> customers.saveAndFlush(new Customer("Bob", "dup@example.com", NOW)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
