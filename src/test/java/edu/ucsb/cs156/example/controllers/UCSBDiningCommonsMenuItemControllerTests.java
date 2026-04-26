package edu.ucsb.cs156.example.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.ucsb.cs156.example.ControllerTestCase;
import edu.ucsb.cs156.example.entities.UCSBDiningCommonsMenuItem;
import edu.ucsb.cs156.example.repositories.UCSBDiningCommonsMenuItemRepository;
import edu.ucsb.cs156.example.repositories.UserRepository;
import edu.ucsb.cs156.example.testconfig.TestConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = UCSBDiningCommonsMenuItemController.class)
@Import(TestConfig.class)
public class UCSBDiningCommonsMenuItemControllerTests extends ControllerTestCase {

  @MockitoBean UCSBDiningCommonsMenuItemRepository ucsbDiningCommonsMenuItemRepository;

  @MockitoBean UserRepository userRepository;

  // Authorization tests for /api/ucsbdiningcommonsmenuitem/admin/all

  @Test
  public void logged_out_users_cannot_get_all() throws Exception {
    mockMvc
        .perform(get("/api/ucsbdiningcommonsmenuitem/all"))
        .andExpect(status().is(403)); // logged out users can't get all
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_users_can_get_all() throws Exception {
    mockMvc
        .perform(get("/api/ucsbdiningcommonsmenuitem/all"))
        .andExpect(status().is(200)); // logged
  }

  // Authorization tests for /api/ucsbdiningcommonsmenuitem/post
  // (Perhaps should also have these for put and delete)

  @Test
  public void logged_out_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/ucsbdiningcommonsmenuitem/post")
                .param("diningCommonsCode", "ortega")
                .param("name", "Chicken Caesar Salad")
                .param("station", "Entrees")
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_regular_users_cannot_post() throws Exception {
    mockMvc
        .perform(
            post("/api/ucsbdiningcommonsmenuitem/post")
                .param("diningCommonsCode", "ortega")
                .param("name", "Chicken Caesar Salad")
                .param("station", "Entrees")
                .with(csrf()))
        .andExpect(status().is(403)); // only admins can post
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void an_admin_user_can_post_a_new_ucsbdiningcommonsmenuitem() throws Exception {
    // arrange

    UCSBDiningCommonsMenuItem ucsbDiningCommonsMenuItem1 =
        UCSBDiningCommonsMenuItem.builder()
            .diningCommonsCode("DLG")
            .name("Chicken Bowl")
            .station("Entrees")
            .build();

    when(ucsbDiningCommonsMenuItemRepository.save(eq(ucsbDiningCommonsMenuItem1)))
        .thenReturn(ucsbDiningCommonsMenuItem1);

    // act
    MvcResult response =
        mockMvc
            .perform(
                post("/api/ucsbdiningcommonsmenuitem/post")
                    .param("diningCommonsCode", "DLG")
                    .param("name", "Chicken Bowl")
                    .param("station", "Entrees")
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    // assert
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).save(ucsbDiningCommonsMenuItem1);
    String expectedJson = mapper.writeValueAsString(ucsbDiningCommonsMenuItem1);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void logged_in_user_can_get_all_ucsbdiningcommonsmenuitem() throws Exception {

    UCSBDiningCommonsMenuItem ucsbDiningCommonsMenuItem1 =
        UCSBDiningCommonsMenuItem.builder()
            .diningCommonsCode("Ortega")
            .name("Chicken Caesar Salad")
            .station("Entrees")
            .build();

    UCSBDiningCommonsMenuItem ucsbDiningCommonsMenuItem2 =
        UCSBDiningCommonsMenuItem.builder()
            .diningCommonsCode("DLG")
            .name("Chicken Bowl")
            .station("Entrees")
            .build();

    ArrayList<UCSBDiningCommonsMenuItem> expectedDiningCommonsMenuItem = new ArrayList<>();
    expectedDiningCommonsMenuItem.addAll(
        Arrays.asList(ucsbDiningCommonsMenuItem1, ucsbDiningCommonsMenuItem2));

    when(ucsbDiningCommonsMenuItemRepository.findAll()).thenReturn(expectedDiningCommonsMenuItem);

    // act
    MvcResult response =
        mockMvc
            .perform(get("/api/ucsbdiningcommonsmenuitem/all"))
            .andExpect(status().isOk())
            .andReturn();

    // assert

    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findAll();
    String expectedJson = mapper.writeValueAsString(expectedDiningCommonsMenuItem);
    String responseString = response.getResponse().getContentAsString();
    assertEquals(expectedJson, responseString);
  }

  @Test
  public void logged_out_users_cannot_get_by_id() throws Exception {
    mockMvc
        .perform(get("/api/ucsbdiningcommonsmenuitem").param("id", "7"))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_exists() throws Exception {
    UCSBDiningCommonsMenuItem item =
        UCSBDiningCommonsMenuItem.builder()
            .id(7)
            .diningCommonsCode("ortega")
            .name("Chicken Caesar Salad")
            .station("Entrees")
            .build();

    when(ucsbDiningCommonsMenuItemRepository.findById(eq(7L))).thenReturn(Optional.of(item));

    MvcResult response =
        mockMvc
            .perform(get("/api/ucsbdiningcommonsmenuitem").param("id", "7"))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(eq(7L));
    String expectedJson = mapper.writeValueAsString(item);
    assertEquals(expectedJson, response.getResponse().getContentAsString());
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void test_that_logged_in_user_can_get_by_id_when_the_id_does_not_exist() throws Exception {
    when(ucsbDiningCommonsMenuItemRepository.findById(eq(7L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(get("/api/ucsbdiningcommonsmenuitem").param("id", "7"))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(eq(7L));
    Map<String, Object> json = responseToJson(response);
    assertEquals("EntityNotFoundException", json.get("type"));
    assertEquals("UCSBDiningCommonsMenuItem with id 7 not found", json.get("message"));
  }

  @Test
  public void logged_out_users_cannot_edit() throws Exception {
    UCSBDiningCommonsMenuItem item =
        UCSBDiningCommonsMenuItem.builder()
            .diningCommonsCode("ortega")
            .name("Chicken Caesar Salad")
            .station("Entrees")
            .build();

    String requestBody = mapper.writeValueAsString(item);

    mockMvc
        .perform(
            put("/api/ucsbdiningcommonsmenuitem")
                .param("id", "67")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8")
                .content(requestBody)
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_users_cannot_edit() throws Exception {
    UCSBDiningCommonsMenuItem item =
        UCSBDiningCommonsMenuItem.builder()
            .diningCommonsCode("ortega")
            .name("Chicken Caesar Salad")
            .station("Entrees")
            .build();

    String requestBody = mapper.writeValueAsString(item);

    mockMvc
        .perform(
            put("/api/ucsbdiningcommonsmenuitem")
                .param("id", "67")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8")
                .content(requestBody)
                .with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_edit_an_existing_menu_item() throws Exception {
    UCSBDiningCommonsMenuItem original =
        UCSBDiningCommonsMenuItem.builder()
            .id(67)
            .diningCommonsCode("ortega")
            .name("Chicken Caesar Salad")
            .station("Entrees")
            .build();

    UCSBDiningCommonsMenuItem edited =
        UCSBDiningCommonsMenuItem.builder()
            .id(67)
            .diningCommonsCode("portola")
            .name("Cream of Broccoli Soup")
            .station("Greens & Grains")
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(ucsbDiningCommonsMenuItemRepository.findById(eq(67L))).thenReturn(Optional.of(original));

    MvcResult response =
        mockMvc
            .perform(
                put("/api/ucsbdiningcommonsmenuitem")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(67L);
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).save(edited);

    String responseString = response.getResponse().getContentAsString();
    assertEquals(requestBody, responseString);
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_cannot_edit_menu_item_that_does_not_exist() throws Exception {
    UCSBDiningCommonsMenuItem edited =
        UCSBDiningCommonsMenuItem.builder()
            .diningCommonsCode("portola")
            .name("Cream of Broccoli Soup")
            .station("Greens & Grains")
            .build();

    String requestBody = mapper.writeValueAsString(edited);

    when(ucsbDiningCommonsMenuItemRepository.findById(eq(67L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(
                put("/api/ucsbdiningcommonsmenuitem")
                    .param("id", "67")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding("utf-8")
                    .content(requestBody)
                    .with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(67L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBDiningCommonsMenuItem with id 67 not found", json.get("message"));
  }

  @Test
  public void logged_out_users_cannot_delete() throws Exception {
    mockMvc
        .perform(delete("/api/ucsbdiningcommonsmenuitem").param("id", "15").with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"USER"})
  @Test
  public void regular_users_cannot_delete() throws Exception {
    mockMvc
        .perform(delete("/api/ucsbdiningcommonsmenuitem").param("id", "15").with(csrf()))
        .andExpect(status().is(403));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_can_delete_a_menu_item() throws Exception {
    UCSBDiningCommonsMenuItem item =
        UCSBDiningCommonsMenuItem.builder()
            .id(15)
            .diningCommonsCode("ortega")
            .name("Chicken Caesar Salad")
            .station("Entrees")
            .build();

    when(ucsbDiningCommonsMenuItemRepository.findById(eq(15L))).thenReturn(Optional.of(item));

    MvcResult response =
        mockMvc
            .perform(delete("/api/ucsbdiningcommonsmenuitem").param("id", "15").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(15L);
    verify(ucsbDiningCommonsMenuItemRepository, times(1)).delete(any());

    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBDiningCommonsMenuItem with id 15 deleted", json.get("message"));
  }

  @WithMockUser(roles = {"ADMIN", "USER"})
  @Test
  public void admin_tries_to_delete_nonexistent_menu_item_and_gets_right_error_message()
      throws Exception {
    when(ucsbDiningCommonsMenuItemRepository.findById(eq(15L))).thenReturn(Optional.empty());

    MvcResult response =
        mockMvc
            .perform(delete("/api/ucsbdiningcommonsmenuitem").param("id", "15").with(csrf()))
            .andExpect(status().isNotFound())
            .andReturn();

    verify(ucsbDiningCommonsMenuItemRepository, times(1)).findById(15L);
    Map<String, Object> json = responseToJson(response);
    assertEquals("UCSBDiningCommonsMenuItem with id 15 not found", json.get("message"));
  }
}
