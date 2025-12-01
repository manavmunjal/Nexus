package com.nexus.model;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CompanyTest {

    private Company company;

    @BeforeEach
    void setup() {
        company = new Company("c1", "Acme Inc.", new ArrayList<>(Arrays.asList("p1", "p2")));
    }

    // ---- Getters and setters ----

    @Test
    void testGetId() {
        assertThat(company.getId()).isEqualTo("c1");
    }

    @Test
    void testGetName() {
        assertThat(company.getName()).isEqualTo("Acme Inc.");
    }

    @Test
    void testGetProducts() {
        assertThat(company.getProducts()).containsExactly("p1", "p2");
    }

    @Test
    void testGetRating_DefaultIsZero() {
        assertThat(company.getRating()).isEqualTo(0.0);
    }

    @Test
    void testSetId() {
        company.setId("c2");
        assertThat(company.getId()).isEqualTo("c2");
    }

    @Test
    void testSetName() {
        company.setName("Beta Corp");
        assertThat(company.getName()).isEqualTo("Beta Corp");
    }

    @Test
    void testSetRating() {
        company.setRating(4.5);
        assertThat(company.getRating()).isEqualTo(4.5);
    }

    @Test
    void testSetProducts() {
        company.setProducts(Arrays.asList("p3", "p4"));
        assertThat(company.getProducts()).containsExactly("p3", "p4");
    }

    // ---- Add and remove product ----

    @Test
    void testAddAndRemoveProduct() {
        company.addProductId("p3");
        assertThat(company.getProducts()).containsExactly("p1", "p2", "p3");

        company.removeProductId("p2");
        assertThat(company.getProducts()).containsExactly("p1", "p3");
    }

    // ---- Find average rating ----

    @Test
    void testFindAverageRating_withProducts() {
        Product prod1 = new Product("p1", "Prod1", "Desc1", "Acme Inc.");
        prod1.setRating(3.0);

        Product prod2 = new Product("p2", "Prod2", "Desc2", "Acme Inc.");
        prod2.setRating(5.0);

        Product prod3 = new Product("p3", "Prod3", "Desc3", "Acme Inc.");
        prod3.setRating(4.0);

        List<Product> products = Arrays.asList(prod1, prod2, prod3);

        double avg = company.findAverageRating(products);

        assertThat(avg).isEqualTo(4.0);
        assertThat(company.getRating()).isEqualTo(4.0);
    }

    @Test
    void testFindAverageRating_withEmptyList() {
        double avg = company.findAverageRating(Collections.emptyList());

        assertThat(avg).isEqualTo(0.0);
        assertThat(company.getRating()).isEqualTo(0.0);
    }

    @Test
    void testFindAverageRating_withNullList() {
        double avg = company.findAverageRating(null);

        assertThat(avg).isEqualTo(0.0);
        assertThat(company.getRating()).isEqualTo(0.0);
    }
}
