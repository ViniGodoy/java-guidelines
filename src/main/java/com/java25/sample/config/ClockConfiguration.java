package com.java25.sample.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/// Single source of time for the application, so tests can fix it.
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
