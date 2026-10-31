package com.talentmatch.job.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class JobSkillDto {

    private Long id;

    @NotBlank(message = "Skill name is required")
    @Size(max = 100, message = "Skill name cannot exceed 100 characters")
    private String skillName;

    private boolean mandatory = false;

    @Min(value = 0, message = "Minimum years required cannot be negative")
    private int minYearsRequired = 0;

    public JobSkillDto() {
    }

    public JobSkillDto(Long id, String skillName, boolean mandatory, int minYearsRequired) {
        this.id = id;
        this.skillName = skillName;
        this.mandatory = mandatory;
        this.minYearsRequired = minYearsRequired;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public int getMinYearsRequired() {
        return minYearsRequired;
    }

    public void setMinYearsRequired(int minYearsRequired) {
        this.minYearsRequired = minYearsRequired;
    }
}
