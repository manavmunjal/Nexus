package com.nexus.controller;

import com.nexus.model.Company;
import com.nexus.repository.CompanyRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Company entities.
 * Provides endpoints for creating companies.
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyRepository companyRepository;

    /**
     * Constructs a CompanyController with the specified CompanyRepository.
     *
     * @param companyRepository the repository for company operations
     */
    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /**
     * Creates a new company.
     *
     * @param company the company to create
     * @return the created company, or null if an error occurs
     */
    @PostMapping
    public Company createCompany(@RequestBody Company company) {
        try {
            return companyRepository.save(company);
        } catch (Exception e) {
            return null;
        }
    }
}
