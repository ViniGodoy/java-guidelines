package com.java25.sample.customer;

import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// Customer use cases. Maps entities to API records so no entity leaves this package.
@Service
public class CustomerService {

  private final CustomerRepository customers;
  private final CustomerProperties properties;
  private final Clock clock;

  CustomerService(CustomerRepository customers, CustomerProperties properties, Clock clock) {
    this.customers = customers;
    this.properties = properties;
    this.clock = clock;
  }

  @Transactional
  public CustomerResponse create(CreateCustomerRequest request) {
    if (customers.existsByEmail(request.email())) {
      throw new EmailAlreadyUsedException(request.email());
    }
    Customer saved =
        customers.save(new Customer(request.name(), request.email(), Instant.now(clock)));
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public CustomerResponse get(long id) {
    return toResponse(find(id));
  }

  @Transactional(readOnly = true)
  public Page<CustomerSummary> list(CustomerStatus status, Pageable pageable) {
    int pageSize = Math.clamp(pageable.getPageSize(), 1, properties.maxPageSize());
    return customers.findByStatus(
        status, PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort()));
  }

  @Transactional
  public void deactivate(long id) {
    find(id).deactivate();
  }

  private Customer find(long id) {
    return customers.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
  }

  private static CustomerResponse toResponse(Customer customer) {
    Long id = customer.getId();
    if (id == null) {
      throw new IllegalStateException("Customer has not been persisted");
    }
    return new CustomerResponse(
        id, customer.getName(), customer.getEmail(), customer.getStatus(), customer.getCreatedAt());
  }
}
