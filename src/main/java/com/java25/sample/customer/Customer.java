package com.java25.sample.customer;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/// A customer account. Aggregate root.
@Entity
@Table(
    name = "customer",
    uniqueConstraints = @UniqueConstraint(name = "uk_customer_email", columnNames = "email"),
    indexes = @Index(name = "idx_customer_status", columnList = "status"))
public class Customer {

  public static final int NAME_MAX_LENGTH = 100;
  public static final int EMAIL_MAX_LENGTH = 254;

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
  @SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq", allocationSize = 50)
  private @Nullable Long id;

  @Version private long version;

  @NotBlank
  @Size(max = NAME_MAX_LENGTH)
  private String name;

  @NotBlank
  @Size(max = EMAIL_MAX_LENGTH)
  private String email;

  @NotNull
  @Enumerated(EnumType.STRING)
  private CustomerStatus status;

  @NotNull private Instant createdAt;

  /// For JPA only.
  protected Customer() {}

  public Customer(String name, String email, Instant createdAt) {
    this.name = name;
    this.email = email;
    this.status = CustomerStatus.ACTIVE;
    this.createdAt = createdAt;
  }

  public @Nullable Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public CustomerStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void rename(String newName) {
    this.name = newName;
  }

  /// Deactivates an active customer.
  ///
  /// @throws CustomerStateException if the customer is already inactive
  public void deactivate() {
    status =
        switch (status) {
          case ACTIVE -> CustomerStatus.INACTIVE;
          case INACTIVE -> throw new CustomerStateException("Customer is already inactive");
        };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Customer other)) {
      return false;
    }
    Long thisId = getId();
    return thisId != null && thisId.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return Customer.class.hashCode();
  }
}
