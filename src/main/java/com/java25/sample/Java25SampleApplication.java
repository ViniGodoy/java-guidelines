package com.java25.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/// Entry point of the toolchain sample service.
@SpringBootApplication
@ConfigurationPropertiesScan
public class Java25SampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(Java25SampleApplication.class, args);
  }
}
