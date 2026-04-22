package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.MenuItemReview;
import edu.ucsb.cs156.example.repositories.MenuItemReviewRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = MenuItemReviewController.class)
@Import(TestConfig.class)
public class MenuItemReviewControllerTests extends ControllerTestCase {

  @MockBean MenuItemReviewRepository menuItemReviewRepository;
  @MockBean UserRepository userRepository;

  // -------------------------------------------------------------------------
  // Tests for GET /api/MenuItemReview/all
  // -------------------------------------------------------------------------

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc.perform(get("/api/MenuItemReview/all")).andExpect(status().isOk());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_menuitemreviews() throws Exception {

    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview review1 =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("cgaucho@ucsb.edu")
            .stars(5)
            .dateReviewed(ldt1)
            .comments("Great food")
            .build();

    ArrayList<MenuItemReview> expectedReviews = new ArrayList<>();
    expectedReviews.addAll(Arrays.asList(review1));

    when(menuItemReviewRepository.findAll()).thenReturn(expectedReviews);

    MvcResult response =
        mockMvc.perform(get("/api/MenuItemReview/all")).andExpect(status().isOk()).andReturn();

    verify(menuItemReviewRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedReviews);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // -------------------------------------------------------------------------
  // Tests for GET /api/MenuItemReview?id=...
  // -------------------------------------------------------------------------

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_getById_returns_review_if_it_exists() throws Exception {

    LocalDateTime ldt = LocalDateTime.parse("2022-01-03T00:00:00");
    MenuItemReview review =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("arjun@ucsb.edu")
            .stars(5)
            .dateReviewed(ldt)
            .comments("Amazing")
            .build();

    when(menuItemReviewRepository.findById(eq(7L))).thenReturn(Optional.of(review));

    MvcResult response =
        mockMvc.perform(get("/api/MenuItemReview?id=7")).andExpect(status().isOk()).andReturn();

    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(review);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_getById_returns_404_if_it_does_not_exist() throws Exception {

    when(menuItemReviewRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/MenuItemReview?id=7"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("MenuItemReview with id 7 not found", json.get("message"));
  }

  // -------------------------------------------------------------------------
  // Tests for POST /api/MenuItemReview/post
  // -------------------------------------------------------------------------

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_review() throws Exception {
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

    MenuItemReview review1 =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("cgaucho@ucsb.edu")
            .stars(5)
            .dateReviewed(ldt1)
            .comments("Great food")
            .build();

    when(menuItemReviewRepository.save(review1)).thenReturn(review1);

    MvcResult response =
        mockMvc
            .perform(
                post("/api/MenuItemReview/post")
                    .param("itemId", "1")
                    .param("reviewerEmail", "cgaucho@ucsb.edu")
                    .param("stars", "5")
                    .param("dateReviewed", "2022-01-03T00:00:00")
                    .param("comments", "Great food")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(menuItemReviewRepository, times(1)).save(review1);
    String expectedJson = mapper.writeValueAsString(review1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // -------------------------------------------------------------------------
  // Tests for PUT /api/MenuItemReview?id=...
  // -------------------------------------------------------------------------

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_update_an_existing_review() throws Exception {

    LocalDateTime oldDate = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime newDate = LocalDateTime.parse("2022-02-04T00:00:00");

    MenuItemReview originalReview =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("old@ucsb.edu")
            .stars(3)
            .dateReviewed(oldDate)
            .comments("Old comment")
            .build();

    MenuItemReview incomingReview =
        MenuItemReview.builder()
            .itemId(9L)
            .reviewerEmail("new@ucsb.edu")
            .stars(5)
            .dateReviewed(newDate)
            .comments("Updated comment")
            .build();

    MenuItemReview expectedReview =
        MenuItemReview.builder()
            .itemId(9L)
            .reviewerEmail("new@ucsb.edu")
            .stars(5)
            .dateReviewed(newDate)
            .comments("Updated comment")
            .build();

    when(menuItemReviewRepository.findById(eq(7L))).thenReturn(Optional.of(originalReview));
    when(menuItemReviewRepository.save(eq(originalReview))).thenReturn(expectedReview);

    String requestBody = mapper.writeValueAsString(incomingReview);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/MenuItemReview?id=7")
                    .contentType("application/json")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    verify(menuItemReviewRepository, times(1)).save(eq(originalReview));

    String expectedJson = mapper.writeValueAsString(expectedReview);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_update_a_missing_review() throws Exception {

    LocalDateTime newDate = LocalDateTime.parse("2022-02-04T00:00:00");

    MenuItemReview incomingReview =
        MenuItemReview.builder()
            .itemId(9L)
            .reviewerEmail("new@ucsb.edu")
            .stars(5)
            .dateReviewed(newDate)
            .comments("Updated comment")
            .build();

    when(menuItemReviewRepository.findById(eq(7L))).thenReturn(Optional.empty());

    String requestBody = mapper.writeValueAsString(incomingReview);

    MvcResult response =
        mockMvc
            .perform(
                put("/api/MenuItemReview?id=7")
                    .contentType("application/json")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("MenuItemReview with id 7 not found", json.get("message"));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_user_cannot_update_a_review() throws Exception {

    LocalDateTime newDate = LocalDateTime.parse("2022-02-04T00:00:00");

    MenuItemReview incomingReview =
        MenuItemReview.builder()
            .itemId(9L)
            .reviewerEmail("new@ucsb.edu")
            .stars(5)
            .dateReviewed(newDate)
            .comments("Updated comment")
            .build();

    String requestBody = mapper.writeValueAsString(incomingReview);

    mockMvc
        .perform(
            put("/api/MenuItemReview?id=7")
                .contentType("application/json")
                .content(requestBody)
                .with(csrf()))
        .andExpect(status().isForbidden());
  }
}
