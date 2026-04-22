package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = MenuItemReviewController.class)
@Import(TestConfig.class)
public class MenuItemReviewControllerTests extends ControllerTestCase {

  @MockBean MenuItemReviewRepository menuItemReviewRepository;
  @MockBean UserRepository userRepository;

  // --- GET ALL ---

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

    ArrayList<MenuItemReview> expectedReviews = new ArrayList<>(Arrays.asList(review1));
    when(menuItemReviewRepository.findAll()).thenReturn(expectedReviews);

    MvcResult response =
        mockMvc.perform(get("/api/MenuItemReview/all")).andExpect(status().isOk()).andReturn();

    verify(menuItemReviewRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedReviews);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  // --- GET BY ID ---

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
        mockMvc.perform(get("/api/MenuItemReview?id=7")).andExpect(status().isNotFound()).andReturn();

    verify(menuItemReviewRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("MenuItemReview with id 7 not found", json.get("message"));
  }

  // --- PUT ---

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_menuitemreview() throws Exception {
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    LocalDateTime ldt2 = LocalDateTime.parse("2023-01-03T00:00:00");

    MenuItemReview reviewOrig =
        MenuItemReview.builder()
            .itemId(1L)
            .reviewerEmail("old@ucsb.edu")
            .stars(3)
            .dateReviewed(ldt1)
            .comments("Old Comment")
            .build();

    MenuItemReview reviewChanged =
        MenuItemReview.builder()
            .itemId(2L)
            .reviewerEmail("new@ucsb.edu")
            .stars(5)
            .dateReviewed(ldt2)
            .comments("New Comment")
            .build();

    String requestBody = mapper.writeValueAsString(reviewChanged);
    when(menuItemReviewRepository.findById(eq(67L))).thenReturn(Optional.of(reviewOrig));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/MenuItemReview?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(menuItemReviewRepository, times(1)).findById(67L);
    verify(menuItemReviewRepository, times(1)).save(reviewChanged);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(mapper.writeValueAsString(reviewChanged), responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_menuitemreview_that_does_not_exist() throws Exception {
    LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
    MenuItemReview reviewChanged =
        MenuItemReview.builder()
            .itemId(2L)
            .reviewerEmail("new@ucsb.edu")
            .stars(5)
            .dateReviewed(ldt1)
            .comments("New Comment")
            .build();

    String requestBody = mapper.writeValueAsString(reviewChanged);
    when(menuItemReviewRepository.findById(eq(67L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/MenuItemReview?id=67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(menuItemReviewRepository, times(1)).findById(67L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("MenuItemReview with id 67 not found", json.get("message"));
  }

  // --- POST ---

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

    when(menuItemReviewRepository.save(eq(review1))).thenReturn(review1);

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
                .andExpect(status().isOk()).andReturn();

        verify(menuItemReviewRepository, times(1)).save(review1);
        String expectedJson = mapper.writeValueAsString(review1);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(expectedJson, responseString);
    }

    // --- PUT TESTS ---

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_can_edit_an_existing_menuitemreview() throws Exception {
        // arrange
        LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");
        LocalDateTime ldt2 = LocalDateTime.parse("2023-01-03T00:00:00");

        MenuItemReview reviewOrig = MenuItemReview.builder()
                .itemId(1L)
                .reviewerEmail("old@ucsb.edu")
                .stars(3)
                .dateReviewed(ldt1)
                .comments("Old Comment")
                .build();

        MenuItemReview reviewChanged = MenuItemReview.builder()
                .itemId(2L)
                .reviewerEmail("new@ucsb.edu")
                .stars(5)
                .dateReviewed(ldt2)
                .comments("New Comment")
                .build();

        String requestBody = mapper.writeValueAsString(reviewChanged);

        when(menuItemReviewRepository.findById(eq(67L))).thenReturn(Optional.of(reviewOrig));

        // act
        MvcResult response = mockMvc.perform(
                put("/api/MenuItemReview?id=67")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isOk()).andReturn();

        // assert
        verify(menuItemReviewRepository, times(1)).findById(67L);
        verify(menuItemReviewRepository, times(1)).save(reviewChanged);
        String responseString = response.getResponse().getContentAsString();
        assertEquals(mapper.writeValueAsString(reviewChanged), responseString);
    }

    @WithMockUser(roles = { "ADMIN", "USER" })
    @Test
    public void admin_cannot_edit_menuitemreview_that_does_not_exist() throws Exception {
        // arrange
        LocalDateTime ldt1 = LocalDateTime.parse("2022-01-03T00:00:00");

        MenuItemReview reviewChanged = MenuItemReview.builder()
                .itemId(2L)
                .reviewerEmail("new@ucsb.edu")
                .stars(5)
                .dateReviewed(ldt1)
                .comments("New Comment")
                .build();

        String requestBody = mapper.writeValueAsString(reviewChanged);

        when(menuItemReviewRepository.findById(eq(67L))).thenReturn(Optional.empty());

        // act
        MvcResult response = mockMvc.perform(
                put("/api/MenuItemReview?id=67")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(requestBody)
                        .with(csrf()))
                .andExpect(status().isNotFound()).andReturn();

        // assert
        verify(menuItemReviewRepository, times(1)).findById(67L);
        Map<String, Object> json = responseToJson(response);
        assertEquals("MenuItemReview with id 67 not found", json.get("message"));
    }
}
