package com.java25.sample.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/// Request body for creating a customer.
public record CreateCustomerRequest(
    @NotBlank @Size(max = Customer.NAME_MAX_LENGTH) String name,
    @NotBlank @Email @Size(max = Customer.EMAIL_MAX_LENGTH) String email) {}
