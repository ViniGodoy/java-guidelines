package com.java25.sample.customer;

import java.time.Instant;

/// API representation of a customer.
public record CustomerResponse(
    long id, String name, String email, CustomerStatus status, Instant createdAt) {}
