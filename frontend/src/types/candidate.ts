export interface CandidateSummary {
  id: number;
  organizationId: number;
  fullName: string | null;
  email: string | null;
  phone: string | null;
  summary: string | null;
  primarySkills: string | null;
  sourceFilename: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CandidateDetail extends CandidateSummary {
  educationHistory: string | null;
  experienceHistory: string | null;
  projectHistory: string | null;
  rawText: string | null;
}
