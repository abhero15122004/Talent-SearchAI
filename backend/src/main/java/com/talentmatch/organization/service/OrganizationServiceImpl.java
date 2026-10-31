package com.talentmatch.organization.service;

import com.talentmatch.audit.service.AuditLogService;
import com.talentmatch.common.exception.DuplicateResourceException;
import com.talentmatch.common.exception.ResourceNotFoundException;
import com.talentmatch.common.util.SecurityUtils;
import com.talentmatch.organization.dto.CreateOrganizationRequest;
import com.talentmatch.organization.dto.OrganizationDto;
import com.talentmatch.organization.entity.Organization;
import com.talentmatch.organization.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AuditLogService auditLogService;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository, AuditLogService auditLogService) {
        this.organizationRepository = organizationRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public OrganizationDto createOrganization(CreateOrganizationRequest request) {
        String normalizedSlug = request.slug().toLowerCase().trim();

        if (organizationRepository.existsBySlug(normalizedSlug)) {
            throw new DuplicateResourceException("An organization with slug '" + normalizedSlug + "' already exists.");
        }

        Organization org = new Organization(request.name().trim(), normalizedSlug, request.domain());
        Organization saved = organizationRepository.save(org);

        auditLogService.recordEvent(
                saved.getId(),
                SecurityUtils.getCurrentUserPrincipal().map(p -> p.getId()).orElse(null),
                SecurityUtils.getCurrentUserPrincipal().map(p -> p.getUsername()).orElse("SYSTEM"),
                "CREATE_ORGANIZATION",
                "Organization",
                String.valueOf(saved.getId()),
                "Created organization: " + saved.getName(),
                "internal"
        );

        return OrganizationDto.from(saved);
    }

    @Override
    public OrganizationDto getOrganizationById(Long id) {
        SecurityUtils.validateOrganizationAccess(id);

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
        return OrganizationDto.from(org);
    }

    @Override
    public OrganizationDto getOrganizationBySlug(String slug) {
        Organization org = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "slug", slug));

        SecurityUtils.validateOrganizationAccess(org.getId());
        return OrganizationDto.from(org);
    }

    @Override
    public List<OrganizationDto> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(OrganizationDto::from)
                .toList();
    }

    @Override
    @Transactional
    public OrganizationDto updateOrganization(Long id, CreateOrganizationRequest request) {
        SecurityUtils.validateOrganizationAccess(id);

        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));

        String normalizedSlug = request.slug().toLowerCase().trim();
        if (!org.getSlug().equals(normalizedSlug) && organizationRepository.existsBySlug(normalizedSlug)) {
            throw new DuplicateResourceException("An organization with slug '" + normalizedSlug + "' already exists.");
        }

        org.setName(request.name().trim());
        org.setSlug(normalizedSlug);
        org.setDomain(request.domain());

        Organization updated = organizationRepository.save(org);
        return OrganizationDto.from(updated);
    }

    @Override
    @Transactional
    public void deactivateOrganization(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
        org.setActive(false);
        organizationRepository.save(org);
    }
}
