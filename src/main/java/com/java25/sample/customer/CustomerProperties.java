package com.java25.sample.customer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/// Customer module settings.
///
/// @param maxPageSize upper bound for any customer listing page
@Validated
@ConfigurationProperties("sample.customers")
public record CustomerProperties(@Min(1) @Max(500) int maxPageSize) {}
