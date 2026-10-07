package com.operationstower.users.infrastructure;

import com.operationstower.users.domain.UserDirectory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AuthentikProperties.class)
class AuthentikUsersConfig {

  @Bean
  UserDirectory userDirectory(AuthentikProperties properties) {
    RestClient client =
        RestClient.builder()
            .baseUrl(properties.baseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
            .build();
    return new AuthentikUserDirectory(client);
  }
}
