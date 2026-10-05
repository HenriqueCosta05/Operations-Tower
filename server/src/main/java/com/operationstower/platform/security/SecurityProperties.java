package com.operationstower.platform.security;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("operationstower.security")
public record SecurityProperties(
    @NotBlank String issuerUri, @NotBlank String audience, List<String> allowedOrigins) {

  public SecurityProperties {
    allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
  }
}
