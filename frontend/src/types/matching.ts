export interface ScoreBreakdown {
  semantic_similarity: number;
  skill_coverage: number;
  experience_relevance: number;
  project_relevance: number;
  final_score: number;
}

export interface CandidateMatchResult {
  candidate_id: string;
  candidate_name?: string;
  score: number;
  score_breakdown: ScoreBreakdown;
  is_eligible: boolean;
  matched_skills: string[];
  missing_mandatory_skills: string[];
  matched_preferred_skills: string[];
  evidence_snippets: Record<string, string>;
  uncertainty_indicators: string[];
}

export interface MatchResponse {
  job_id: string;
  total_candidates_evaluated: number;
  eligible_candidates_count: number;
  shortlisted_count: number;
  matches: CandidateMatchResult[];
  model_metadata: Record<string, string>;
}
