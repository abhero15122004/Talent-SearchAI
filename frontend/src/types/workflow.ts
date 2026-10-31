export type ApplicationStage =
  | 'NEW'
  | 'REVIEWING'
  | 'SHORTLISTED'
  | 'INTERVIEW'
  | 'REJECTED'
  | 'HIRED';

export type MandatoryStatus = 'PASSED' | 'FAILED' | 'NOT_VERIFIABLE';

export interface JobApplication {
  id: number;
  jobId: number;
  jobTitle: string;
  candidateId: number;
  candidateName: string;
  candidateEmail: string;
  candidatePhone?: string;
  candidateSkills?: string;
  stage: ApplicationStage;
  shortlisted: boolean;
  decisionReason?: string;
  assignedToUserName?: string;
  latestMatchScore?: number;
  latestMatchRank?: number;
  latestMatchEligible?: boolean;
  stageUpdatedAt: string;
  createdAt: string;
}

export interface JobApplicationNote {
  id: number;
  applicationId: number;
  authorName: string;
  authorEmail: string;
  noteText: string;
  stageAtCreation: string;
  decisionReason?: string;
  createdAt: string;
}

export interface CandidateMatch {
  id: number;
  candidateId: number;
  candidateName: string;
  candidateEmail: string;
  rankPosition: number;
  finalScore: number;
  semanticSimilarityScore: number;
  skillCoverageScore: number;
  experienceRelevanceScore: number;
  projectRelevanceScore: number;
  eligible: boolean;
  mandatoryStatus: MandatoryStatus;
  shortlisted: boolean;
  matchedSkills: string[];
  missingMandatorySkills: string[];
  matchedPreferredSkills: string[];
  evidenceSnippets: Record<string, string>;
  warnings: string[];
  currentStage: ApplicationStage;
  applicationId?: number;
  createdAt: string;
}

export interface JobMatchRun {
  id: number;
  jobId: number;
  jobTitle: string;
  runNumber: number;
  algorithmVersion: string;
  weightSkillCoverage: number;
  weightExperienceRelevance: number;
  weightProjectRelevance: number;
  weightSemanticSimilarity: number;
  minScoreThreshold?: number;
  shortlistTarget: number;
  totalCandidatesEvaluated: number;
  eligibleCandidatesCount: number;
  shortlistedCount: number;
  createdAt: string;
  matches: CandidateMatch[];
}

export interface CandidateComparisonItem {
  candidateId: number;
  candidateName: string;
  candidateEmail: string;
  stage: ApplicationStage;
  finalScore: number;
  semanticSimilarityScore: number;
  skillCoverageScore: number;
  experienceRelevanceScore: number;
  projectRelevanceScore: number;
  eligible: boolean;
  mandatoryStatus: MandatoryStatus;
  matchedSkills: string[];
  missingMandatorySkills: string[];
  matchedPreferredSkills: string[];
  evidenceSnippets: Record<string, string>;
  warnings: string[];
}

export interface CandidateComparison {
  jobId: number;
  jobTitle: string;
  candidates: CandidateComparisonItem[];
}
