package com.operationstower.users.infrastructure;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("operationstower.users.authentik")
record AuthentikProperties(@NotBlank String baseUrl, String token) {}
