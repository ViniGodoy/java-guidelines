package com.java25.sample.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/customers")
class CustomerController {

  private final CustomerService service;

  CustomerController(CustomerService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<CustomerResponse> create(
      @Valid @RequestBody CreateCustomerRequest request, UriComponentsBuilder uri) {
    CustomerResponse created = service.create(request);
    return ResponseEntity.created(uri.path("/api/customers/{id}").build(created.id()))
        .body(created);
  }

  @GetMapping("/{id}")
  CustomerResponse get(@PathVariable @Positive long id) {
    return service.get(id);
  }

  @GetMapping
  PagedModel<CustomerSummary> list(
      @RequestParam(defaultValue = "ACTIVE") CustomerStatus status, Pageable pageable) {
    return new PagedModel<>(service.list(status, pageable));
  }

  @PostMapping("/{id}/deactivation")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deactivate(@PathVariable @Positive long id) {
    service.deactivate(id);
  }
}
