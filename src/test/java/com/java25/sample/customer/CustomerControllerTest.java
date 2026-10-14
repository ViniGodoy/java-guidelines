package com.java25.sample.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.java25.sample.config.SecurityConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(
    controllers = CustomerController.class,
    properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.example.test")
@Import(SecurityConfiguration.class)
class CustomerControllerTest {

  private static final Instant CREATED_AT = Instant.parse("2026-10-13T12:00:00Z");

  @Autowired private MockMvcTester mvc;

  @MockitoBean private CustomerService service;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void rejectsRequestsWithoutToken() {
    assertThat(mvc.get().uri("/api/customers/1")).hasStatus(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void returnsCustomer() {
    given(service.get(1L))
        .willReturn(
            new CustomerResponse(1L, "Ada", "ada@example.com", CustomerStatus.ACTIVE, CREATED_AT));

    assertThat(mvc.get().uri("/api/customers/1").with(jwt()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.email")
        .isEqualTo("ada@example.com");
  }

  @Test
  void missingCustomerIsProblemDetail404() {
    given(service.get(42L)).willThrow(new CustomerNotFoundException(42L));

    assertThat(mvc.get().uri("/api/customers/42").with(jwt()))
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void invalidBodyIsProblemDetail400() {
    assertThat(
            mvc.post()
                .uri("/api/customers")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "", "email": "not-an-email"}
                    """))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void createReturns201WithLocation() {
    given(service.create(any()))
        .willReturn(
            new CustomerResponse(7L, "Ada", "ada@example.com", CustomerStatus.ACTIVE, CREATED_AT));

    assertThat(
            mvc.post()
                .uri("/api/customers")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name": "Ada", "email": "ada@example.com"}
                    """))
        .hasStatus(HttpStatus.CREATED)
        .hasHeader("Location", "http://localhost/api/customers/7");
  }
}
