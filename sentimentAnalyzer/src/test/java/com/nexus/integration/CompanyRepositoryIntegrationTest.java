package com.nexus.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.controller.CompanyController;
import com.nexus.model.Company;
import com.nexus.repository.CompanyRepository;
import com.nexus.repository.ProductRepository;
import com.nexus.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

/**
 * Integration tests for {@link CompanyController}.
 *
 * <p>
 * These tests verify the main endpoints of the controller, including creating
 * companies and fetching
 * average ratings, covering both success and not-found scenarios.
 */
@WebMvcTest(CompanyController.class)
public class CompanyRepositoryIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private CompanyRepository companyRepository;
  @MockBean
  private ProductRepository productRepository;
  @MockBean
  private ReviewRepository reviewRepository;

  private ObjectMapper objectMapper;
  private Company company;

  /**
   * Sets up common test data and initializes the ObjectMapper before each test.
   *
   * <p>
   * Creates a sample Company object to be used in the tests.
   */
  @BeforeEach
  public void setUp() {
    objectMapper = new ObjectMapper();

    // Create a sample company
    company = new Company();
    company.setId("123");
    company.setName("Test Company");
    company.setRating(4.5);
  }

  /**
   * Test scenario: Successfully creating a company.
   *
   * <p>
   * Mocks the CompanyRepository to return the company and verifies the API
   * response status and
   * content.
   *
   * @throws Exception if the MockMvc request fails
   */
  @Test
  public void testCreateCompany_Success() throws Exception {
    when(companyRepository.save(any(Company.class))).thenReturn(company);

    mockMvc
        .perform(
            post("/api/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(company)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test Company"))
        .andExpect(jsonPath("$.rating").value(4.5));
  }

  /**
   * Test scenario: Fetch average rating for a company that exists.
   *
   * <p>
   * Mocks the CompanyRepository to return a valid company and verifies the API
   * response status and
   * content.
   *
   * @throws Exception if the MockMvc request fails
   */
  @Test
  public void testGetCompanyAverageRating_Success() throws Exception {
    when(companyRepository.findById("123")).thenReturn(Optional.of(company));

    mockMvc
        .perform(get("/api/companies/123/average-rating"))
        .andExpect(status().isOk())
        .andExpect(content().string("4.5"));
  }

  /**
   * Test scenario: Fetch average rating for a company that does not exist (404).
   *
   * <p>
   * Mocks the CompanyRepository to return empty and verifies that the API returns
   * a 500 status with
   * the correct error message (as per controller implementation which throws
   * RuntimeException).
   *
   * @throws Exception if the MockMvc request fails
   */
  @Test
  public void testGetCompanyAverageRating_NotFound() throws Exception {
    when(companyRepository.findById("123")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/companies/123/average-rating"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string("Unexpected error occurred: Company not found"));
  }
}
