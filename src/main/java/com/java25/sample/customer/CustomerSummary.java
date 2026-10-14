package com.java25.sample.customer;

/// Read-only projection used for customer listings.
public record CustomerSummary(Long id, String name, String email) {}
