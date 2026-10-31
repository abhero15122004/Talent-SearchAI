package com.talentmatch.organization.service;

import com.talentmatch.organization.dto.CreateOrganizationRequest;
import com.talentmatch.organization.dto.OrganizationDto;

import java.util.List;

public interface OrganizationService {

    OrganizationDto createOrganization(CreateOrganizationRequest request);

    OrganizationDto getOrganizationById(Long id);

    OrganizationDto getOrganizationBySlug(String slug);

    List<OrganizationDto> getAllOrganizations();

    OrganizationDto updateOrganization(Long id, CreateOrganizationRequest request);

    void deactivateOrganization(Long id);
}
