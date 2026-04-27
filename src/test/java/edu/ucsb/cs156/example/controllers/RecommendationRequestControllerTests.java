package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.RecommendationRequest;
import edu.ucsb.cs156.example.repositories.RecommendationRequestRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = RecommendationRequestController.class)
@Import(TestConfig.class)
public class RecommendationRequestControllerTests extends ControllerTestCase {

  @MockitoBean RecommendationRequestRepository recommendationRequestRepository;

  @MockitoBean UserRepository userRepository;

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc.perform(get("/api/RecommendationRequest/all")).andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/RecommendationRequest").param("id", "123"))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/RecommendationRequest/post")
                .param("requesterEmail", "student1@ucsb.edu")
                .param("professorEmail", "prof1@ucsb.edu")
                .param("explanation", "Applying to graduate school")
                .param("dateRequested", "2026-04-20T10:15:30")
                .param("dateNeeded", "2026-05-01T17:00:00")
                .param("done", "false")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_put() throws Exception {
    LocalDateTime requested = LocalDateTime.parse("2026-04-20T10:15:30");
    LocalDateTime needed = LocalDateTime.parse("2026-05-01T17:00:00");

    RecommendationRequest incomingRequest =
        RecommendationRequest.builder()
            .requesterEmail("student1@ucsb.edu")
            .professorEmail("prof1@ucsb.edu")
            .explanation("Updated explanation")
            .dateRequested(requested)
            .dateNeeded(needed)
            .done(true)
            .build();

    String requestBody = mapper.writeValueAsString(incomingRequest);

    mockMvc
        .perform(
            put("/api/RecommendationRequest?id=123")
                .contentType("application/json")
                .content(requestBody)
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc
        .perform(delete("/api/RecommendationRequest").param("id", "123").with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_recommendation_requests() throws Exception {
    LocalDateTime requested = LocalDateTime.parse("2026-04-20T10:15:30");
    LocalDateTime needed = LocalDateTime.parse("2026-05-01T17:00:00");

    RecommendationRequest recommendationRequest1 =
        RecommendationRequest.builder()
            .id(1L)
            .requesterEmail("student1@ucsb.edu")
            .professorEmail("prof1@ucsb.edu")
            .explanation("Applying to graduate school")
            .dateRequested(requested)
            .dateNeeded(needed)
            .done(false)
            .build();

    RecommendationRequest recommendationRequest2 =
        RecommendationRequest.builder()
            .id(2L)
            .requesterEmail("student2@ucsb.edu")
            .professorEmail("prof2@ucsb.edu")
            .explanation("Scholarship application")
            .dateRequested(requested.plusDays(1))
            .dateNeeded(needed.plusDays(7))
            .done(true)
            .build();

    ArrayList<RecommendationRequest> expectedRequests = new ArrayList<>();
    expectedRequests.addAll(Arrays.asList(recommendationRequest1, recommendationRequest2));

    when(recommendationRequestRepository.findAll()).thenReturn(expectedRequests);

    MvcResult response =
        mockMvc
            .perform(get("/api/RecommendationRequest/all"))
            .andExpect(status().isOk())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedRequests);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_recommendation_request_by_id() throws Exception {
    LocalDateTime requested = LocalDateTime.parse("2026-04-20T10:15:30");
    LocalDateTime needed = LocalDateTime.parse("2026-05-01T17:00:00");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .id(123L)
            .requesterEmail("student1@ucsb.edu")
            .professorEmail("prof1@ucsb.edu")
            .explanation("Applying to graduate school")
            .dateRequested(requested)
            .dateNeeded(needed)
            .done(false)
            .build();

    when(recommendationRequestRepository.findById(eq(123L)))
        .thenReturn(Optional.of(recommendationRequest));

    MvcResult response =
        mockMvc
            .perform(get("/api/RecommendationRequest").param("id", "123"))
            .andExpect(status().isOk())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).findById(eq(123L));
    String expectedJson = mapper.writeValueAsString(recommendationRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_user_can_get_recommendation_request_by_id() throws Exception {
    LocalDateTime requested = LocalDateTime.parse("2026-04-20T10:15:30");
    LocalDateTime needed = LocalDateTime.parse("2026-05-01T17:00:00");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .id(123L)
            .requesterEmail("student1@ucsb.edu")
            .professorEmail("prof1@ucsb.edu")
            .explanation("Applying to graduate school")
            .dateRequested(requested)
            .dateNeeded(needed)
            .done(false)
            .build();

    when(recommendationRequestRepository.findById(eq(123L)))
        .thenReturn(Optional.of(recommendationRequest));

    MvcResult response =
        mockMvc
            .perform(get("/api/RecommendationRequest").param("id", "123"))
            .andExpect(status().isOk())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).findById(eq(123L));
    String expectedJson = mapper.writeValueAsString(recommendationRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void get_recommendation_request_by_id_returns_404_for_missing_id() throws Exception {
    when(recommendationRequestRepository.findById(eq(123L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/RecommendationRequest").param("id", "123"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).findById(eq(123L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("id 123 not found", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_update_recommendation_request_by_id() throws Exception {
    LocalDateTime oldRequested = LocalDateTime.parse("2026-04-20T10:15:30");
    LocalDateTime oldNeeded = LocalDateTime.parse("2026-05-01T17:00:00");
    LocalDateTime newRequested = LocalDateTime.parse("2026-04-25T09:00:00");
    LocalDateTime newNeeded = LocalDateTime.parse("2026-05-10T12:30:00");

    RecommendationRequest originalRequest =
        RecommendationRequest.builder()
            .id(123L)
            .requesterEmail("student1@ucsb.edu")
            .professorEmail("prof1@ucsb.edu")
            .explanation("Original explanation")
            .dateRequested(oldRequested)
            .dateNeeded(oldNeeded)
            .done(false)
            .build();

    RecommendationRequest incomingRequest =
        RecommendationRequest.builder()
            .id(999L)
            .requesterEmail("student2@ucsb.edu")
            .professorEmail("prof2@ucsb.edu")
            .explanation("Updated explanation")
            .dateRequested(newRequested)
            .dateNeeded(newNeeded)
            .done(true)
            .build();

    RecommendationRequest expectedRequest =
        RecommendationRequest.builder()
            .id(123L)
            .requesterEmail("student2@ucsb.edu")
            .professorEmail("prof2@ucsb.edu")
            .explanation("Updated explanation")
            .dateRequested(newRequested)
            .dateNeeded(newNeeded)
            .done(true)
            .build();

    when(recommendationRequestRepository.findById(eq(123L)))
        .thenReturn(Optional.of(originalRequest));
    when(recommendationRequestRepository.save(eq(originalRequest))).thenReturn(expectedRequest);

    String requestBody = mapper.writeValueAsString(incomingRequest);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/RecommendationRequest?id=123")
                    .contentType("application/json")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).findById(eq(123L));
    verify(recommendationRequestRepository, times(1)).save(eq(originalRequest));

    String expectedJson = mapper.writeValueAsString(expectedRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_update_missing_recommendation_request() throws Exception {
    LocalDateTime newRequested = LocalDateTime.parse("2026-04-25T09:00:00");
    LocalDateTime newNeeded = LocalDateTime.parse("2026-05-10T12:30:00");

    RecommendationRequest incomingRequest =
        RecommendationRequest.builder()
            .requesterEmail("student2@ucsb.edu")
            .professorEmail("prof2@ucsb.edu")
            .explanation("Updated explanation")
            .dateRequested(newRequested)
            .dateNeeded(newNeeded)
            .done(true)
            .build();

    when(recommendationRequestRepository.findById(eq(123L))).thenReturn(Optional.empty());

    String requestBody = mapper.writeValueAsString(incomingRequest);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/RecommendationRequest?id=123")
                    .contentType("application/json")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).findById(eq(123L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("id 123 not found", json.get("message"));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_user_cannot_update_recommendation_request() throws Exception {
    LocalDateTime newRequested = LocalDateTime.parse("2026-04-25T09:00:00");
    LocalDateTime newNeeded = LocalDateTime.parse("2026-05-10T12:30:00");

    RecommendationRequest incomingRequest =
        RecommendationRequest.builder()
            .requesterEmail("student2@ucsb.edu")
            .professorEmail("prof2@ucsb.edu")
            .explanation("Updated explanation")
            .dateRequested(newRequested)
            .dateNeeded(newNeeded)
            .done(true)
            .build();

    String requestBody = mapper.writeValueAsString(incomingRequest);

    mockMvc
        .perform(
            put("/api/RecommendationRequest?id=123")
                .contentType("application/json")
                .content(requestBody)
                .with(csrf()))
        .andExpect(status().isForbidden());

    verify(recommendationRequestRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_delete_recommendation_request() throws Exception {
    LocalDateTime requested = LocalDateTime.parse("2026-04-20T10:15:30");
    LocalDateTime needed = LocalDateTime.parse("2026-05-01T17:00:00");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .id(123L)
            .requesterEmail("student1@ucsb.edu")
            .professorEmail("prof1@ucsb.edu")
            .explanation("Applying to graduate school")
            .dateRequested(requested)
            .dateNeeded(needed)
            .done(false)
            .build();

    when(recommendationRequestRepository.findById(eq(123L)))
        .thenReturn(Optional.of(recommendationRequest));

    MvcResult response =
        mockMvc
            .perform(delete("/api/RecommendationRequest").param("id", "123").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).findById(eq(123L));
    verify(recommendationRequestRepository, times(1)).delete(eq(recommendationRequest));

    Map<String, Object> json = responseToJson(response);
    assertEquals("record 123 deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_delete_missing_recommendation_request() throws Exception {
    when(recommendationRequestRepository.findById(eq(123L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/RecommendationRequest").param("id", "123").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).findById(eq(123L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("record 123 not found", json.get("message"));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_user_cannot_delete_recommendation_request() throws Exception {
    mockMvc
        .perform(delete("/api/RecommendationRequest").param("id", "123").with(csrf()))
        .andExpect(status().isForbidden());

    verify(recommendationRequestRepository, never()).delete(any());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_user_cannot_post_a_new_recommendation_request() throws Exception {
    mockMvc
        .perform(
            post("/api/RecommendationRequest/post")
                .param("requesterEmail", "student1@ucsb.edu")
                .param("professorEmail", "prof1@ucsb.edu")
                .param("explanation", "Applying to graduate school")
                .param("dateRequested", "2026-04-20T10:15:30")
                .param("dateNeeded", "2026-05-01T17:00:00")
                .param("done", "false")
                .with(csrf()))
        .andExpect(status().isForbidden());

    verify(recommendationRequestRepository, never()).save(any());
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_recommendation_request() throws Exception {
    LocalDateTime requested = LocalDateTime.parse("2026-04-20T10:15:30");
    LocalDateTime needed = LocalDateTime.parse("2026-05-01T17:00:00");

    RecommendationRequest recommendationRequest =
        RecommendationRequest.builder()
            .requesterEmail("student1@ucsb.edu")
            .professorEmail("prof1@ucsb.edu")
            .explanation("Applying to graduate school")
            .dateRequested(requested)
            .dateNeeded(needed)
            .done(true)
            .build();

    when(recommendationRequestRepository.save(eq(recommendationRequest)))
        .thenReturn(recommendationRequest);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/RecommendationRequest/post")
                    .param("requesterEmail", "student1@ucsb.edu")
                    .param("professorEmail", "prof1@ucsb.edu")
                    .param("explanation", "Applying to graduate school")
                    .param("dateRequested", "2026-04-20T10:15:30")
                    .param("dateNeeded", "2026-05-01T17:00:00")
                    .param("done", "true")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(recommendationRequestRepository, times(1)).save(recommendationRequest);
    String expectedJson = mapper.writeValueAsString(recommendationRequest);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }
}
