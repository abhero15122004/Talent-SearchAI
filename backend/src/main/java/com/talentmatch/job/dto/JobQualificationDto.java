package com.talentmatch.job.dto;

import com.talentmatch.job.entity.QualificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class JobQualificationDto {

    private Long id;

    @NotNull(message = "Qualification type is required")
    private QualificationType qualificationType = QualificationType.OTHER;

    @NotBlank(message = "Requirement text is required")
    @Size(max = 255, message = "Requirement text cannot exceed 255 characters")
    private String requirementText;

    private boolean mandatory = true;

    public JobQualificationDto() {
    }

    public JobQualificationDto(Long id, QualificationType qualificationType, String requirementText, boolean mandatory) {
        this.id = id;
        this.qualificationType = qualificationType;
        this.requirementText = requirementText;
        this.mandatory = mandatory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public QualificationType getQualificationType() {
        return qualificationType;
    }

    public void setQualificationType(QualificationType qualificationType) {
        this.qualificationType = qualificationType;
    }

    public String getRequirementText() {
        return requirementText;
    }

    public void setRequirementText(String requirementText) {
        this.requirementText = requirementText;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
}
