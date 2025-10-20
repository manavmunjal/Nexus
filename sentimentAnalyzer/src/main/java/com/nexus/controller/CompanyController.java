package com.nexus.controller;

import com.nexus.model.Company;
import com.nexus.repository.CompanyRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyRepository companyRepository;

    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @PostMapping
    public Company createCompany(@RequestBody Company company) {
        return companyRepository.save(company);
    }
}
