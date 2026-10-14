package com.java25.sample.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CustomerTest {

  private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

  @Test
  void newCustomerIsActive() {
    var customer = new Customer("Ada", "ada@example.com", NOW);

    assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
  }

  @Test
  void deactivateMovesActiveToInactive() {
    var customer = new Customer("Ada", "ada@example.com", NOW);

    customer.deactivate();

    assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
  }

  @Test
  void deactivatingTwiceIsRejected() {
    var customer = new Customer("Ada", "ada@example.com", NOW);
    customer.deactivate();

    assertThatThrownBy(customer::deactivate).isInstanceOf(CustomerStateException.class);
  }

  @Test
  void unsavedCustomersAreNeverEqual() {
    var first = new Customer("Ada", "ada@example.com", NOW);
    var second = new Customer("Ada", "ada@example.com", NOW);

    assertThat(first).isNotEqualTo(second);
  }
}
