package com.operationstower.users.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.operationstower.users.domain.User;
import com.operationstower.users.domain.UserDirectory;
import com.operationstower.users.domain.UserDraft;
import com.operationstower.users.domain.UserId;
import java.util.List;
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
class UserControllerIT {

  private static final User ADA =
      new User(new UserId(7), "ada", "Ada Lovelace", "ada@example.com", true);
  private static final String ADA_JSON =
      """
      {"id":7,"username":"ada","name":"Ada Lovelace","email":"ada@example.com","active":true}
      """;
  private static final String ADA_REQUEST =
      """
      {"username":"ada","name":"Ada Lovelace","email":"ada@example.com"}
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserDirectory directory;

  @Test
  void rejectsRequestsWithoutAnAccessToken() throws Exception {
    mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
  }

  @Test
  void forbidsReadingUsersToCallersOutsideTheAdminsGroup() throws Exception {
    mockMvc.perform(get("/api/users").with(operator())).andExpect(status().isForbidden());
  }

  @Test
  void forbidsCreatingUsersToCallersOutsideTheAdminsGroup() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
                .with(operator())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADA_REQUEST))
        .andExpect(status().isForbidden());

    verifyNoInteractions(directory);
  }

  @Test
  void listsEveryUserInTheDirectory() throws Exception {
    User grace = new User(new UserId(8), "grace", "Grace Hopper", "grace@example.com", false);
    given(directory.findAll()).willReturn(List.of(ADA, grace));

    mockMvc
        .perform(get("/api/users").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].username").value("ada"))
        .andExpect(jsonPath("$[1].id").value(8))
        .andExpect(jsonPath("$[1].active").value(false));
  }

  @Test
  void describesASingleUser() throws Exception {
    given(directory.findById(new UserId(7))).willReturn(Optional.of(ADA));

    mockMvc
        .perform(get("/api/users/7").with(admin()))
        .andExpect(status().isOk())
        .andExpect(content().json(ADA_JSON, true));
  }

  @Test
  void createsAUserAndPointsToItsLocation() throws Exception {
    given(directory.create(new UserDraft("ada", "Ada Lovelace", "ada@example.com")))
        .willReturn(ADA);

    mockMvc
        .perform(
            post("/api/users")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADA_REQUEST))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/users/7"))
        .andExpect(content().json(ADA_JSON, true));
  }

  @Test
  void rejectsCreatingAUserWithABlankUsername() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":" ","name":"Ada Lovelace","email":"ada@example.com"}
                    """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(directory);
  }

  @Test
  void rejectsCreatingAUserWithoutAnEmail() throws Exception {
    mockMvc
        .perform(
            post("/api/users")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"ada","name":"Ada Lovelace"}
                    """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(directory);
  }

  @Test
  void updatesTheProfileOfTheUserNamedInThePath() throws Exception {
    User renamed = new User(new UserId(7), "ada", "Ada King", "ada@king.org", true);
    given(directory.update(new UserId(7), new UserDraft("ada", "Ada King", "ada@king.org")))
        .willReturn(renamed);

    mockMvc
        .perform(
            put("/api/users/7")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"ada","name":"Ada King","email":"ada@king.org"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Ada King"))
        .andExpect(jsonPath("$.email").value("ada@king.org"));
  }

  @Test
  void rejectsUpdatingAUserWithAMalformedEmail() throws Exception {
    mockMvc
        .perform(
            put("/api/users/7")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"ada","name":"Ada King","email":"not-an-email"}
                    """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(directory);
  }

  @Test
  void deactivatesTheUserNamedInThePath() throws Exception {
    given(directory.deactivate(new UserId(7)))
        .willReturn(new User(new UserId(7), "ada", "Ada Lovelace", "ada@example.com", false));

    mockMvc
        .perform(post("/api/users/7/deactivate").with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void rejectsANonNumericUserId() throws Exception {
    mockMvc.perform(get("/api/users/ada").with(admin())).andExpect(status().isBadRequest());

    verifyNoInteractions(directory);
  }

  @Test
  void trimsSurroundingWhitespaceBeforeTheProfileReachesTheDirectory() throws Exception {
    given(directory.create(any())).willReturn(ADA);

    mockMvc
        .perform(
            post("/api/users")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":" ada ","name":" Ada Lovelace ","email":"ada@example.com"}
                    """))
        .andExpect(status().isCreated());

    verify(directory).create(new UserDraft("ada", "Ada Lovelace", "ada@example.com"));
  }

  private static RequestPostProcessor admin() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINS"));
  }

  private static RequestPostProcessor operator() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATORS"));
  }
}
