package com.operationstower.users.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.operationstower.users.domain.UserDirectory;
import com.operationstower.users.domain.UserDirectoryUnavailableException;
import com.operationstower.users.domain.UserId;
import com.operationstower.users.domain.UserNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class UserExceptionHandlerIT {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserDirectory directory;

  @Test
  void anUnknownUserIsReportedAsNotFound() throws Exception {
    given(directory.findById(new UserId(99))).willReturn(Optional.empty());

    mockMvc
        .perform(get("/api/users/99").with(admin()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("User not found: 99"));
  }

  @Test
  void deactivatingAnUnknownUserIsReportedAsNotFound() throws Exception {
    given(directory.deactivate(new UserId(99)))
        .willThrow(new UserNotFoundException(new UserId(99)));

    mockMvc
        .perform(post("/api/users/99/deactivate").with(admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void anAddressTheDomainRejectsIsReportedAsABadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"ada","name":"Ada Lovelace","email":"ada@localhost"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("email is not a valid address: ada@localhost"));

    verifyNoInteractions(directory);
  }

  @Test
  void anUnreachableIdentityProviderIsReportedAsABadGateway() throws Exception {
    given(directory.findAll())
        .willThrow(
            new UserDirectoryUnavailableException(
                "GET http://authentik.internal:9000/api/v3/core/users/ timed out",
                new IllegalStateException("connect timed out")));

    mockMvc
        .perform(get("/api/users").with(admin()))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value("The identity provider could not be reached"));
  }

  private static RequestPostProcessor admin() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINS"));
  }
}
