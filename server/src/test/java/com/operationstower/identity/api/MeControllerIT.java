package com.operationstower.identity.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class MeControllerIT {

  @Autowired private MockMvc mockMvc;

  @Test
  void rejectsRequestsWithoutAnAccessToken() throws Exception {
    mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void describesTheCallerFromTheirAccessToken() throws Exception {
    mockMvc
        .perform(
            get("/api/me")
                .with(
                    jwt()
                        .jwt(
                            token ->
                                token
                                    .subject("u-1")
                                    .claim("name", "Ada Operator")
                                    .claim("email", "ada@example.com")
                                    .claim("groups", List.of("operators")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subject").value("u-1"))
        .andExpect(jsonPath("$.name").value("Ada Operator"))
        .andExpect(jsonPath("$.email").value("ada@example.com"))
        .andExpect(jsonPath("$.groups[0]").value("operators"));
  }

  @Test
  void allowsTheAngularDevServerToCallTheApiAcrossOrigins() throws Exception {
    mockMvc
        .perform(
            options("/api/me")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
  }
}
