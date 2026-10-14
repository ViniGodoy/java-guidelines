package com.java25.sample.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CustomerRepository extends JpaRepository<Customer, Long> {

  boolean existsByEmail(String email);

  Page<CustomerSummary> findByStatus(CustomerStatus status, Pageable pageable);
}
