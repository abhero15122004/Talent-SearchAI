export type JobStatus = 'DRAFT' | 'OPEN' | 'PAUSED' | 'CLOSED' | 'ARCHIVED';

export type EmploymentType = 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERNSHIP' | 'REMOTE';

export type QualificationType = 'EDUCATION' | 'CERTIFICATION' | 'ELIGIBILITY' | 'OTHER';

export interface JobSkill {
  id?: number;
  skillName: string;
  mandatory: boolean;
  minYearsRequired: number;
}

export interface JobQualification {
  id?: number;
  qualificationType: QualificationType;
  requirementText: string;
  mandatory: boolean;
}

export interface JobSummary {
  id: number;
  organizationId: number;
  title: string;
  department: string;
  location: string;
  employmentType: EmploymentType;
  status: JobStatus;
  minYearsExperience: number;
  maxYearsExperience: number | null;
  shortlistCount: number;
  minScoreThreshold: number;
  mandatorySkillsCount: number;
  preferredSkillsCount: number;
  qualificationsCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Job {
  id: number;
  organizationId: number;
  organizationName: string;
  createdByUserId: number;
  createdByUserName: string;
  title: string;
  department: string;
  location: string;
  employmentType: EmploymentType;
  description: string;
  minYearsExperience: number;
  maxYearsExperience: number | null;
  status: JobStatus;
  shortlistCount: number;
  minScoreThreshold: number;
  weightSkillCoverage: number;
  weightExperienceRelevance: number;
  weightProjectRelevance: number;
  weightSemanticSimilarity: number;
  skills: JobSkill[];
  qualifications: JobQualification[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateJobPayload {
  title: string;
  department: string;
  location: string;
  employmentType: EmploymentType;
  description: string;
  minYearsExperience: number;
  maxYearsExperience: number | null;
  status?: JobStatus;
  shortlistCount: number;
  minScoreThreshold: number;
  weightSkillCoverage: number;
  weightExperienceRelevance: number;
  weightProjectRelevance: number;
  weightSemanticSimilarity: number;
  skills: JobSkill[];
  qualifications: JobQualification[];
}

export interface JobFilterParams {
  page?: number;
  size?: number;
  status?: JobStatus | '';
  search?: string;
}
