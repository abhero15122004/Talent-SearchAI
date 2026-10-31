import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { jobsApi } from '../../api/jobs';
import { workflowApi } from '../../api/workflow';
import {
  Job,
  JobStatus,
  JobMatchRun,
  CandidateMatch,
  ApplicationStage,
  CandidateComparison,
} from '../../types';
import { extractErrorMessage } from '../../api/client';
import { JobStatusBadge } from '../../components/jobs/JobStatusBadge';
import { WeightMeter } from '../../components/jobs/WeightMeter';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ExplainabilityDrawer } from '../../components/matching/ExplainabilityDrawer';
import { CandidateComparisonModal } from '../../components/matching/CandidateComparisonModal';
import { ApplicationNotesModal } from '../../components/workflow/ApplicationNotesModal';
import { StageConfirmModal } from '../../components/workflow/StageConfirmModal';
import {
  ArrowLeft,
  Briefcase,
  Calendar,
  User,
  Edit2,
  Archive,
  CheckCircle2,
  Sparkles,
  MapPin,
  Clock,
  Target,
  FileCheck2,
  AlertCircle,
  Cpu,
  Check,
  X,
  ExternalLink,
  Star,
  MessageSquare,
  Columns,
  Search,
  Filter,
  History,
  Info,
  HelpCircle,
  RefreshCw,
  FileText,
} from 'lucide-react';

export const JobDetailPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();

  const [job, setJob] = useState<Job | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState<boolean>(false);

  // Phase 5 Matching and Recruitment Workflow State
  const [currentMatchRun, setCurrentMatchRun] = useState<JobMatchRun | null>(null);
  const [matchRunHistory, setMatchRunHistory] = useState<JobMatchRun[]>([]);
  const [selectedRunId, setSelectedRunId] = useState<number | null>(null);
  const [isLoadingMatches, setIsLoadingMatches] = useState<boolean>(false);
  const [isMatching, setIsMatching] = useState<boolean>(false);
  const [matchingError, setMatchingError] = useState<string | null>(null);

  // Filters & Search
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [stageFilter, setStageFilter] = useState<ApplicationStage | 'ALL'>('ALL');
  const [eligibilityFilter, setEligibilityFilter] = useState<'ALL' | 'ELIGIBLE_ONLY' | 'INELIGIBLE_ONLY'>('ALL');

  // Explainability Drawer State
  const [selectedMatchForExplain, setSelectedMatchForExplain] = useState<CandidateMatch | null>(null);

  // Candidate Comparison State
  const [selectedCandidateIds, setSelectedCandidateIds] = useState<number[]>([]);
  const [isCompareModalOpen, setIsCompareModalOpen] = useState<boolean>(false);
  const [comparisonData, setComparisonData] = useState<CandidateComparison | null>(null);
  const [isLoadingCompare, setIsLoadingCompare] = useState<boolean>(false);

  // Notes Modal State
  const [notesModalTarget, setNotesModalTarget] = useState<{
    applicationId: number;
    candidateName: string;
  } | null>(null);

  // Stage Transition Confirmation State
  const [stageTransitionTarget, setStageTransitionTarget] = useState<{
    applicationId: number;
    candidateName: string;
    currentStage: ApplicationStage;
    targetStage: ApplicationStage;
  } | null>(null);

  const loadJob = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setError(null);
    try {
      const data = await jobsApi.getJobById(Number(id));
      setJob(data);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  const loadLatestMatchRun = useCallback(async (jobId: number) => {
    try {
      setIsLoadingMatches(true);
      setMatchingError(null);
      const [latestRun, historyResp] = await Promise.all([
        workflowApi.getLatestMatchRun(jobId),
        workflowApi.getMatchRunHistory(jobId, { page: 0, size: 20 }),
      ]);
      setCurrentMatchRun(latestRun);
      if (latestRun) {
        setSelectedRunId(latestRun.id);
      }
      setMatchRunHistory(historyResp.content || []);
    } catch (err) {
      setMatchingError(extractErrorMessage(err));
    } finally {
      setIsLoadingMatches(false);
    }
  }, []);

  useEffect(() => {
    loadJob();
  }, [loadJob]);

  useEffect(() => {
    if (job?.id) {
      loadLatestMatchRun(job.id);
    }
  }, [job?.id, loadLatestMatchRun]);

  // Execute a new AI match run
  const handleExecuteMatchRun = async () => {
    if (!job) return;
    setIsMatching(true);
    setMatchingError(null);
    try {
      const newRun = await workflowApi.runMatching(job.id);
      setCurrentMatchRun(newRun);
      setSelectedRunId(newRun.id);
      setSelectedCandidateIds([]);
      // Refresh history
      const historyResp = await workflowApi.getMatchRunHistory(job.id, { page: 0, size: 20 });
      setMatchRunHistory(historyResp.content || []);
    } catch (err) {
      setMatchingError(extractErrorMessage(err));
    } finally {
      setIsMatching(false);
    }
  };

  // Switch between historical runs
  const handleSelectRun = async (runId: number) => {
    if (!job || runId === currentMatchRun?.id) return;
    try {
      setIsLoadingMatches(true);
      const run = await workflowApi.getMatchRunById(job.id, runId);
      setCurrentMatchRun(run);
      setSelectedRunId(run.id);
      setSelectedCandidateIds([]);
    } catch (err) {
      setMatchingError(extractErrorMessage(err));
    } finally {
      setIsLoadingMatches(false);
    }
  };

  // Toggle Shortlist Status
  const handleToggleShortlist = async (match: CandidateMatch) => {
    if (!job || !match.applicationId) return;
    try {
      const updatedApp = await workflowApi.toggleShortlist(job.id, match.applicationId);
      setCurrentMatchRun((prev) => {
        if (!prev) return null;
        const updatedMatches = prev.matches.map((m) => {
          if (m.id === match.id) {
            return {
              ...m,
              shortlisted: updatedApp.shortlisted,
              currentStage: updatedApp.stage,
            };
          }
          return m;
        });
        const shortlistedCount = updatedMatches.filter((m) => m.shortlisted).length;
        return {
          ...prev,
          shortlistedCount,
          matches: updatedMatches,
        };
      });
    } catch (err) {
      alert(`Failed to update shortlist status: ${extractErrorMessage(err)}`);
    }
  };

  // Request Stage Transition
  const handleStageSelect = (match: CandidateMatch, targetStage: ApplicationStage) => {
    if (!match.applicationId || match.currentStage === targetStage) return;

    // Prompt confirmation for terminal or important stages
    if (targetStage === 'REJECTED' || targetStage === 'HIRED' || match.currentStage === 'HIRED') {
      setStageTransitionTarget({
        applicationId: match.applicationId,
        candidateName: match.candidateName,
        currentStage: match.currentStage,
        targetStage,
      });
    } else {
      executeStageTransition(match.applicationId, targetStage);
    }
  };

  // Execute Stage Transition
  const executeStageTransition = async (
    applicationId: number,
    targetStage: ApplicationStage,
    decisionReason?: string,
    note?: string
  ) => {
    if (!job) return;
    try {
      const updatedApp = await workflowApi.updateStage(job.id, applicationId, {
        stage: targetStage,
        decisionReason,
        note,
      });

      setCurrentMatchRun((prev) => {
        if (!prev) return null;
        const updatedMatches = prev.matches.map((m) => {
          if (m.applicationId === applicationId) {
            return {
              ...m,
              currentStage: updatedApp.stage,
              shortlisted: updatedApp.shortlisted,
            };
          }
          return m;
        });
        const shortlistedCount = updatedMatches.filter((m) => m.shortlisted).length;
        return {
          ...prev,
          shortlistedCount,
          matches: updatedMatches,
        };
      });
    } catch (err) {
      alert(`Failed to transition candidate stage: ${extractErrorMessage(err)}`);
    }
  };

  // Handle Candidate Selection for Comparison
  const handleToggleSelectCandidate = (candidateId: number) => {
    setSelectedCandidateIds((prev) => {
      if (prev.includes(candidateId)) {
        return prev.filter((id) => id !== candidateId);
      }
      if (prev.length >= 5) {
        alert('You can compare a maximum of 5 candidates at a time.');
        return prev;
      }
      return [...prev, candidateId];
    });
  };

  // Open Candidate Comparison Modal
  const handleOpenComparison = async () => {
    if (!job || selectedCandidateIds.length < 2) return;
    try {
      setIsLoadingCompare(true);
      const comparison = await workflowApi.compareCandidates(job.id, selectedCandidateIds);
      setComparisonData(comparison);
      setIsCompareModalOpen(true);
    } catch (err) {
      alert(`Failed to load candidate comparison: ${extractErrorMessage(err)}`);
    } finally {
      setIsLoadingCompare(false);
    }
  };

  const handleStatusChange = async (newStatus: JobStatus) => {
    if (!job) return;
    setIsUpdatingStatus(true);
    try {
      const updated = await jobsApi.updateJobStatus(job.id, newStatus);
      setJob(updated);
    } catch (err) {
      alert(`Failed to update status: ${extractErrorMessage(err)}`);
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  const handleArchive = async () => {
    if (!job || !window.confirm(`Are you sure you want to archive "${job.title}"?`)) {
      return;
    }
    try {
      await jobsApi.archiveJob(job.id);
      navigate('/jobs');
    } catch (err) {
      alert(`Failed to archive job: ${extractErrorMessage(err)}`);
    }
  };

  // Filtered Candidates
  const filteredMatches = useMemo(() => {
    if (!currentMatchRun) return [];
    return currentMatchRun.matches.filter((m) => {
      // Stage filter
      if (stageFilter === 'SHORTLISTED' && !m.shortlisted) return false;
      if (stageFilter !== 'ALL' && stageFilter !== 'SHORTLISTED' && m.currentStage !== stageFilter) {
        return false;
      }

      // Eligibility filter
      if (eligibilityFilter === 'ELIGIBLE_ONLY' && !m.eligible) return false;
      if (eligibilityFilter === 'INELIGIBLE_ONLY' && m.eligible) return false;

      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesName = m.candidateName.toLowerCase().includes(q);
        const matchesEmail = m.candidateEmail.toLowerCase().includes(q);
        const matchesSkill = m.matchedSkills?.some((s) => s.toLowerCase().includes(q));
        if (!matchesName && !matchesEmail && !matchesSkill) return false;
      }

      return true;
    });
  }, [currentMatchRun, stageFilter, eligibilityFilter, searchQuery]);

  if (isLoading) {
    return (
      <div className="loading-state-container">
        <LoadingSpinner size="large" />
        <p>Loading job requisition details...</p>
      </div>
    );
  }

  if (error || !job) {
    return (
      <div className="content-card">
        <div className="login-alert" role="alert">
          <AlertCircle size={18} className="alert-icon" />
          <span>{error || 'Job requisition not found'}</span>
        </div>
        <button onClick={() => navigate('/jobs')} className="btn-secondary mt-3">
          <ArrowLeft size={16} />
          <span>Return to Job Requisitions</span>
        </button>
      </div>
    );
  }

  const mandatorySkills = job.skills?.filter((s) => s.mandatory) || [];
  const preferredSkills = job.skills?.filter((s) => !s.mandatory) || [];

  return (
    <div className="job-detail-page">
      {/* Back Link & Header Bar */}
      <div className="section-header">
        <div className="section-title-group">
          <Link to="/jobs" className="btn-back-link">
            <ArrowLeft size={16} />
            <span>Back to Job Requisitions</span>
          </Link>
          <div className="detail-title-cluster">
            <h1>{job.title}</h1>
            <JobStatusBadge status={job.status} />
          </div>
          <div className="detail-meta-row">
            <span className="meta-item">
              <Briefcase size={14} /> {job.department}
            </span>
            <span className="meta-item">
              <MapPin size={14} /> {job.location}
            </span>
            <span className="meta-item">
              <Clock size={14} /> {job.employmentType.replace('_', ' ')}
            </span>
            <span className="meta-item">
              <Calendar size={14} /> Created: {new Date(job.createdAt).toLocaleDateString()}
            </span>
            <span className="meta-item">
              <User size={14} /> Created by: {job.createdByUserName}
            </span>
          </div>
        </div>

        {/* Top Actions: Quick Status & Edit */}
        <div className="detail-actions-cluster">
          <div className="status-selector-wrapper">
            <label htmlFor="quick-status" className="status-selector-label">
              Status:
            </label>
            <select
              id="quick-status"
              className="form-control form-control-sm"
              value={job.status}
              disabled={isUpdatingStatus}
              onChange={(e) => handleStatusChange(e.target.value as JobStatus)}
            >
              <option value="DRAFT">DRAFT</option>
              <option value="OPEN">OPEN</option>
              <option value="PAUSED">PAUSED</option>
              <option value="CLOSED">CLOSED</option>
              <option value="ARCHIVED">ARCHIVED</option>
            </select>
          </div>

          <button
            onClick={handleExecuteMatchRun}
            className="btn-primary"
            disabled={isMatching}
            title="Evaluate explainable semantic matches and refresh workflow"
          >
            {isMatching ? <LoadingSpinner size="small" /> : <Cpu size={16} />}
            <span>{isMatching ? 'Running AI Match...' : 'Run AI Match'}</span>
          </button>

          <button
            onClick={() => navigate(`/jobs/${job.id}/edit`)}
            className="btn-secondary"
            title="Edit Requisition"
          >
            <Edit2 size={16} />
            <span>Edit Requisition</span>
          </button>

          {job.status !== 'ARCHIVED' && (
            <button
              onClick={handleArchive}
              className="btn-danger-outline"
              title="Archive Requisition"
            >
              <Archive size={16} />
              <span>Archive</span>
            </button>
          )}
        </div>
      </div>

      {/* 4 Core Metric KPI Cards */}
      <div className="metrics-grid mb-4">
        <div className="metric-card metric-blue">
          <div className="metric-header">
            <span className="metric-title">Shortlist Target</span>
            <div className="metric-icon-box">
              <Target size={20} />
            </div>
          </div>
          <div className="metric-value">{job.shortlistCount}</div>
          <div className="metric-subtitle">Target candidate count</div>
        </div>

        <div className="metric-card metric-emerald">
          <div className="metric-header">
            <span className="metric-title">Min Score Cutoff</span>
            <div className="metric-icon-box">
              <Sparkles size={20} />
            </div>
          </div>
          <div className="metric-value">{job.minScoreThreshold}%</div>
          <div className="metric-subtitle">Strict eligibility threshold</div>
        </div>

        <div className="metric-card metric-purple">
          <div className="metric-header">
            <span className="metric-title">Experience Scope</span>
            <div className="metric-icon-box">
              <Clock size={20} />
            </div>
          </div>
          <div className="metric-value">
            {job.minYearsExperience}
            {job.maxYearsExperience ? ` - ${job.maxYearsExperience}` : '+'} yrs
          </div>
          <div className="metric-subtitle">Required professional tenure</div>
        </div>

        <div className="metric-card metric-amber">
          <div className="metric-header">
            <span className="metric-title">Competency Criteria</span>
            <div className="metric-icon-box">
              <CheckCircle2 size={20} />
            </div>
          </div>
          <div className="metric-value">{job.skills?.length || 0} Skills</div>
          <div className="metric-subtitle">
            {mandatorySkills.length} mandatory &bull; {preferredSkills.length} preferred
          </div>
        </div>
      </div>

      {/* Match Configuration Card */}
      <div className="content-card mb-4">
        <div className="card-header">
          <div className="card-title-group">
            <Target size={18} className="card-icon text-blue" />
            <h2>Candidate Match Configuration &amp; Scoring Weights</h2>
          </div>
        </div>
        <div className="card-body">
          <WeightMeter
            skillWeight={Number(job.weightSkillCoverage)}
            expWeight={Number(job.weightExperienceRelevance)}
            projectWeight={Number(job.weightProjectRelevance)}
            semanticWeight={Number(job.weightSemanticSimilarity)}
          />
        </div>
      </div>

      {/* Skills & Qualifications Grid */}
      <div className="details-two-col mb-4">
        {/* Skills Card */}
        <div className="content-card">
          <div className="card-header">
            <div className="card-title-group">
              <Sparkles size={18} className="card-icon text-amber" />
              <h2>Required Skills ({job.skills?.length || 0})</h2>
            </div>
          </div>
          <div className="card-body">
            <div className="skills-categorized-group">
              <h3 className="skills-subheading mandatory-subheading">
                Mandatory Competencies ({mandatorySkills.length})
              </h3>
              {mandatorySkills.length === 0 ? (
                <p className="text-muted-p">No mandatory skills required for this role.</p>
              ) : (
                <div className="skills-chip-list">
                  {mandatorySkills.map((s, idx) => (
                    <div key={idx} className="skill-chip chip-mandatory">
                      <span className="skill-chip-name">{s.skillName}</span>
                      {s.minYearsRequired > 0 && (
                        <span className="skill-chip-exp">{s.minYearsRequired}y+ exp</span>
                      )}
                    </div>
                  ))}
                </div>
              )}

              <h3 className="skills-subheading preferred-subheading mt-4">
                Preferred Competencies ({preferredSkills.length})
              </h3>
              {preferredSkills.length === 0 ? (
                <p className="text-muted-p">No optional/preferred skills listed.</p>
              ) : (
                <div className="skills-chip-list">
                  {preferredSkills.map((s, idx) => (
                    <div key={idx} className="skill-chip chip-preferred">
                      <span className="skill-chip-name">{s.skillName}</span>
                      {s.minYearsRequired > 0 && (
                        <span className="skill-chip-exp">{s.minYearsRequired}y+ exp</span>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Qualifications Card */}
        <div className="content-card">
          <div className="card-header">
            <div className="card-title-group">
              <FileCheck2 size={18} className="card-icon text-purple" />
              <h2>Qualifications &amp; Eligibility ({job.qualifications?.length || 0})</h2>
            </div>
          </div>
          <div className="card-body">
            {!job.qualifications || job.qualifications.length === 0 ? (
              <p className="text-muted-p">No specific degree or certification requirements listed.</p>
            ) : (
              <ul className="qualifications-list">
                {job.qualifications.map((q, idx) => (
                  <li key={idx} className="qual-item">
                    <span className="qual-type-badge">{q.qualificationType}</span>
                    <span className="qual-text">{q.requirementText}</span>
                    {q.mandatory ? (
                      <span className="qual-mandatory-badge">Mandatory</span>
                    ) : (
                      <span className="qual-optional-badge">Preferred</span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>

      {/* Full Job Description Card */}
      <div className="content-card mb-4">
        <div className="card-header">
          <div className="card-title-group">
            <Briefcase size={18} className="card-icon text-emerald" />
            <h2>Job Description &amp; Responsibilities</h2>
          </div>
        </div>
        <div className="card-body">
          <div className="description-text-view">{job.description}</div>
        </div>
      </div>

      {/* ========================================================================= */}
      {/* PHASE 5: EXPLAINABLE CANDIDATE RANKING & RECRUITMENT WORKFLOW             */}
      {/* ========================================================================= */}
      <div className="content-card mb-4" id="recruitment-workflow-section">
        {/* Header & Controls */}
        <div
          className="card-header"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
            borderBottom: '1px solid #e2e8f0',
            paddingBottom: '1rem',
          }}
        >
          <div className="card-title-group" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Cpu size={22} className="card-icon text-blue" />
            <div>
              <h2 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 700, color: '#0f172a' }}>
                Explainable Candidate Ranking &amp; Recruitment Workflow
              </h2>
              <p style={{ margin: 0, fontSize: '0.85rem', color: '#64748b' }}>
                Deterministic hybrid scoring &bull; Transparent evidence snippets &bull; Multi-stage pipeline
              </p>
            </div>
          </div>

          {/* Action cluster: Run Selector, Run AI Match, Compare */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            {/* Historical Run Selector */}
            {matchRunHistory.length > 0 && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                <History size={16} style={{ color: '#64748b' }} />
                <select
                  value={selectedRunId || ''}
                  onChange={(e) => handleSelectRun(Number(e.target.value))}
                  disabled={isLoadingMatches}
                  style={{
                    padding: '0.4rem 0.6rem',
                    borderRadius: '6px',
                    border: '1px solid #cbd5e1',
                    fontSize: '0.85rem',
                    background: '#fff',
                    color: '#334155',
                  }}
                >
                  {matchRunHistory.map((run) => (
                    <option key={run.id} value={run.id}>
                      Run #{run.runNumber} ({new Date(run.createdAt).toLocaleDateString()}) - {run.totalCandidatesEvaluated} eval
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Compare Selected Candidates Button */}
            <button
              onClick={handleOpenComparison}
              disabled={selectedCandidateIds.length < 2 || isLoadingCompare}
              className="btn btn-secondary"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.4rem',
                fontSize: '0.85rem',
                padding: '0.45rem 0.85rem',
                background: selectedCandidateIds.length >= 2 ? '#e0e7ff' : '#f8fafc',
                color: selectedCandidateIds.length >= 2 ? '#3730a3' : '#94a3b8',
                borderColor: selectedCandidateIds.length >= 2 ? '#c7d2fe' : '#e2e8f0',
              }}
              title="Select 2 to 5 candidates using checkboxes to compare side-by-side"
            >
              <Columns size={16} />
              <span>
                {isLoadingCompare
                  ? 'Loading...'
                  : selectedCandidateIds.length >= 2
                  ? `Compare Selected (${selectedCandidateIds.length})`
                  : 'Compare Candidates'}
              </span>
            </button>

            {/* Execute AI Match Button */}
            <button
              onClick={handleExecuteMatchRun}
              className="btn btn-primary"
              disabled={isMatching}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.4rem',
                fontSize: '0.85rem',
                padding: '0.45rem 0.9rem',
              }}
            >
              {isMatching ? <LoadingSpinner size="small" /> : <RefreshCw size={15} />}
              <span>{isMatching ? 'Running Match...' : 'Run AI Match'}</span>
            </button>
          </div>
        </div>

        <div className="card-body">
          {matchingError && (
            <div className="login-alert mb-4" role="alert">
              <AlertCircle size={18} className="alert-icon" />
              <div>
                <strong>AI Match Error:</strong> {matchingError}
              </div>
            </div>
          )}

          {/* Run Metadata Banner */}
          {currentMatchRun && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '1rem',
                background: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                padding: '0.75rem 1rem',
                marginBottom: '1.25rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
                <span style={{ fontSize: '0.85rem', color: '#475569' }}>
                  <strong>Match Run #{currentMatchRun.runNumber}</strong> &bull; Version:{' '}
                  <code style={{ background: '#e2e8f0', padding: '0.1rem 0.3rem', borderRadius: '4px' }}>
                    {currentMatchRun.algorithmVersion}
                  </code>
                </span>
                <span style={{ fontSize: '0.85rem', color: '#64748b' }}>
                  Timestamp: {new Date(currentMatchRun.createdAt).toLocaleString()}
                </span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <span
                  style={{
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    background: '#e0f2fe',
                    color: '#0369a1',
                    padding: '0.2rem 0.6rem',
                    borderRadius: '4px',
                  }}
                >
                  Evaluated: {currentMatchRun.totalCandidatesEvaluated}
                </span>
                <span
                  style={{
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    background: '#dcfce7',
                    color: '#166534',
                    padding: '0.2rem 0.6rem',
                    borderRadius: '4px',
                  }}
                >
                  Eligible: {currentMatchRun.eligibleCandidatesCount}
                </span>
                <span
                  style={{
                    fontSize: '0.8rem',
                    fontWeight: 600,
                    background: '#fef3c7',
                    color: '#92400e',
                    padding: '0.2rem 0.6rem',
                    borderRadius: '4px',
                  }}
                >
                  Shortlisted: {currentMatchRun.shortlistedCount} / {currentMatchRun.shortlistTarget}
                </span>
              </div>
            </div>
          )}

          {/* Filtering and Search Controls */}
          {currentMatchRun && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '1rem',
                marginBottom: '1rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', flex: 1 }}>
                {/* Search Input */}
                <div style={{ position: 'relative', minWidth: '240px', flex: 1 }}>
                  <Search
                    size={16}
                    style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }}
                  />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search candidate name, email, or skill..."
                    style={{
                      width: '100%',
                      padding: '0.45rem 0.75rem 0.45rem 2.2rem',
                      borderRadius: '6px',
                      border: '1px solid #cbd5e1',
                      fontSize: '0.85rem',
                    }}
                  />
                  {searchQuery && (
                    <button
                      onClick={() => setSearchQuery('')}
                      style={{
                        position: 'absolute',
                        right: '8px',
                        top: '50%',
                        transform: 'translateY(-50%)',
                        background: 'transparent',
                        border: 'none',
                        cursor: 'pointer',
                        color: '#94a3b8',
                      }}
                    >
                      <X size={14} />
                    </button>
                  )}
                </div>

                {/* Stage Filter */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <Filter size={15} style={{ color: '#64748b' }} />
                  <select
                    value={stageFilter}
                    onChange={(e) => setStageFilter(e.target.value as any)}
                    style={{
                      padding: '0.45rem 0.6rem',
                      borderRadius: '6px',
                      border: '1px solid #cbd5e1',
                      fontSize: '0.85rem',
                      background: '#fff',
                    }}
                  >
                    <option value="ALL">All Stages</option>
                    <option value="SHORTLISTED">Starred / Shortlisted Only</option>
                    <option value="NEW">Stage: NEW</option>
                    <option value="REVIEWING">Stage: REVIEWING</option>
                    <option value="INTERVIEW">Stage: INTERVIEW</option>
                    <option value="HIRED">Stage: HIRED</option>
                    <option value="REJECTED">Stage: REJECTED</option>
                  </select>
                </div>

                {/* Eligibility Filter */}
                <select
                  value={eligibilityFilter}
                  onChange={(e) => setEligibilityFilter(e.target.value as any)}
                  style={{
                    padding: '0.45rem 0.6rem',
                    borderRadius: '6px',
                    border: '1px solid #cbd5e1',
                    fontSize: '0.85rem',
                    background: '#fff',
                  }}
                >
                  <option value="ALL">All Candidates</option>
                  <option value="ELIGIBLE_ONLY">Eligible Only (Passed)</option>
                  <option value="INELIGIBLE_ONLY">Ineligible (Failed / Not Verifiable)</option>
                </select>
              </div>

              {selectedCandidateIds.length > 0 && (
                <div style={{ fontSize: '0.85rem', color: '#475569' }}>
                  <strong>{selectedCandidateIds.length}</strong> selected for comparison &bull;{' '}
                  <button
                    onClick={() => setSelectedCandidateIds([])}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: '#4f46e5',
                      cursor: 'pointer',
                      padding: 0,
                      textDecoration: 'underline',
                    }}
                  >
                    Clear selection
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Match Runs Loading / Content */}
          {isLoadingMatches ? (
            <div style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>
              <LoadingSpinner size="large" />
              <p style={{ marginTop: '0.75rem', fontSize: '0.9rem' }}>Loading candidate ranking &amp; workflow data...</p>
            </div>
          ) : !currentMatchRun || currentMatchRun.matches.length === 0 ? (
            <div
              style={{
                textAlign: 'center',
                padding: '3rem 2rem',
                background: '#f8fafc',
                border: '1px dashed #cbd5e1',
                borderRadius: '8px',
                color: '#64748b',
              }}
            >
              <Cpu size={36} style={{ color: '#94a3b8', margin: '0 auto 1rem' }} />
              <h3 style={{ margin: '0 0 0.5rem', fontSize: '1.1rem', color: '#334155' }}>
                No AI Match Runs Executed Yet
              </h3>
              <p style={{ margin: '0 0 1.25rem', fontSize: '0.9rem', maxWidth: '480px', marginLeft: 'auto', marginRight: 'auto' }}>
                Run the explainable AI ranking engine to evaluate candidates against this job requisition's mandatory
                requirements, skills, experience, and semantic relevance.
              </p>
              <button
                onClick={handleExecuteMatchRun}
                className="btn btn-primary"
                disabled={isMatching}
                style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
              >
                {isMatching ? <LoadingSpinner size="small" /> : <Sparkles size={16} />}
                <span>{isMatching ? 'Running First Match...' : 'Execute First Match Run'}</span>
              </button>
            </div>
          ) : filteredMatches.length === 0 ? (
            <div
              style={{
                textAlign: 'center',
                padding: '2rem',
                background: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                color: '#64748b',
              }}
            >
              <p style={{ margin: 0, fontSize: '0.9rem' }}>
                No candidate matches match the selected stage, eligibility, or search filters.
              </p>
              <button
                onClick={() => {
                  setStageFilter('ALL');
                  setEligibilityFilter('ALL');
                  setSearchQuery('');
                }}
                className="btn btn-secondary mt-3"
                style={{ fontSize: '0.85rem', padding: '0.35rem 0.75rem' }}
              >
                Reset Filters
              </button>
            </div>
          ) : (
            /* Ranked Candidate Table */
            <div className="table-responsive" style={{ border: '1px solid #e2e8f0', borderRadius: '8px', overflow: 'hidden' }}>
              <table className="data-table" style={{ margin: 0, width: '100%', borderCollapse: 'collapse' }}>
                <thead style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                  <tr>
                    <th style={{ width: '40px', textAlign: 'center', padding: '0.75rem 0.5rem' }}>
                      <input
                        type="checkbox"
                        checked={
                          filteredMatches.length > 0 &&
                          filteredMatches.every((m) => selectedCandidateIds.includes(m.candidateId))
                        }
                        onChange={(e) => {
                          if (e.target.checked) {
                            const ids = filteredMatches.slice(0, 5).map((m) => m.candidateId);
                            setSelectedCandidateIds(ids);
                          } else {
                            setSelectedCandidateIds([]);
                          }
                        }}
                        title="Select for comparison (up to 5)"
                      />
                    </th>
                    <th style={{ width: '60px', textAlign: 'center' }}>Rank</th>
                    <th style={{ minWidth: '200px' }}>Candidate</th>
                    <th style={{ minWidth: '150px' }}>Match Score</th>
                    <th style={{ minWidth: '180px' }}>Component Signals</th>
                    <th style={{ minWidth: '130px' }}>Eligibility</th>
                    <th style={{ minWidth: '70px', textAlign: 'center' }}>Shortlist</th>
                    <th style={{ minWidth: '160px' }}>Workflow Stage</th>
                    <th style={{ minWidth: '140px', textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredMatches.map((m) => {
                    const isSelected = selectedCandidateIds.includes(m.candidateId);
                    const score = Number(m.finalScore || 0);

                    // Score color coding
                    const scoreColor = score >= 70 ? '#16a34a' : score >= 50 ? '#d97706' : '#dc2626';

                    return (
                      <tr
                        key={m.id}
                        style={{
                          background: isSelected ? '#f5f3ff' : undefined,
                          borderBottom: '1px solid #f1f5f9',
                          transition: 'background-color 0.15s ease',
                        }}
                      >
                        {/* Checkbox */}
                        <td style={{ textAlign: 'center', padding: '0.75rem 0.5rem' }}>
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => handleToggleSelectCandidate(m.candidateId)}
                            title="Select for comparison"
                          />
                        </td>

                        {/* Rank */}
                        <td style={{ textAlign: 'center', fontWeight: 700, color: '#475569', fontSize: '0.95rem' }}>
                          #{m.rankPosition}
                        </td>

                        {/* Candidate Details */}
                        <td>
                          <div style={{ display: 'flex', flexDirection: 'column' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                              <Link
                                to={`/candidates/${m.candidateId}`}
                                style={{
                                  fontWeight: 600,
                                  color: '#1e293b',
                                  textDecoration: 'none',
                                  fontSize: '0.95rem',
                                }}
                                onMouseEnter={(e) => (e.currentTarget.style.color = '#4f46e5')}
                                onMouseLeave={(e) => (e.currentTarget.style.color = '#1e293b')}
                              >
                                {m.candidateName}
                              </Link>
                              <ExternalLink size={13} style={{ color: '#94a3b8' }} />
                            </div>
                            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>{m.candidateEmail}</span>

                            {/* Direct Candidate Action Button: View Resume & Explainability */}
                            <button
                              type="button"
                              onClick={() => setSelectedMatchForExplain(m)}
                              style={{
                                marginTop: '0.4rem',
                                padding: '0.28rem 0.65rem',
                                fontSize: '0.76rem',
                                fontWeight: 600,
                                borderRadius: '6px',
                                border: '1px solid #c7d2fe',
                                background: '#f5f3ff',
                                color: '#4f46e5',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.35rem',
                                cursor: 'pointer',
                                width: 'fit-content',
                                transition: 'all 0.15s ease',
                                boxShadow: '0 1px 2px rgba(99, 102, 241, 0.08)',
                              }}
                              onMouseEnter={(e) => {
                                e.currentTarget.style.background = '#e0e7ff';
                                e.currentTarget.style.borderColor = '#6366f1';
                              }}
                              onMouseLeave={(e) => {
                                e.currentTarget.style.background = '#f5f3ff';
                                e.currentTarget.style.borderColor = '#c7d2fe';
                              }}
                              title="Inspect candidate resume and full AI explainability assessment"
                            >
                              <FileText size={12} />
                              <span>View Resume &amp; Explainability</span>
                            </button>
                          </div>
                        </td>

                        {/* Match Score */}
                        <td>
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                              <span style={{ fontWeight: 800, fontSize: '1.05rem', color: scoreColor }}>
                                {score.toFixed(1)}%
                              </span>
                              <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>Relevance</span>
                            </div>
                            {/* Score Bar */}
                            <div
                              style={{
                                width: '100%',
                                height: '6px',
                                background: '#e2e8f0',
                                borderRadius: '3px',
                                overflow: 'hidden',
                              }}
                            >
                              <div
                                style={{
                                  width: `${Math.min(100, Math.max(0, score))}%`,
                                  height: '100%',
                                  background: scoreColor,
                                  borderRadius: '3px',
                                }}
                              />
                            </div>
                          </div>
                        </td>

                        {/* Component Signals Breakdown */}
                        <td>
                          <div
                            style={{
                              display: 'grid',
                              gridTemplateColumns: 'repeat(2, 1fr)',
                              gap: '0.25rem',
                              fontSize: '0.75rem',
                              color: '#475569',
                            }}
                          >
                            <span title="Skill Coverage Score">
                              Skill: <strong>{Number(m.skillCoverageScore || 0).toFixed(0)}%</strong>
                            </span>
                            <span title="Experience Relevance Score">
                              Exp: <strong>{Number(m.experienceRelevanceScore || 0).toFixed(0)}%</strong>
                            </span>
                            <span title="Project Relevance Score">
                              Proj: <strong>{Number(m.projectRelevanceScore || 0).toFixed(0)}%</strong>
                            </span>
                            <span title="Semantic Similarity Score">
                              Sem: <strong>{Number(m.semanticSimilarityScore || 0).toFixed(0)}%</strong>
                            </span>
                          </div>
                        </td>

                        {/* Eligibility Status */}
                        <td>
                          {m.eligible ? (
                            <span
                              style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.3rem',
                                background: '#dcfce7',
                                color: '#166534',
                                padding: '0.2rem 0.55rem',
                                borderRadius: '4px',
                                fontSize: '0.75rem',
                                fontWeight: 600,
                              }}
                            >
                              <Check size={13} />
                              Passed
                            </span>
                          ) : m.mandatoryStatus === 'NOT_VERIFIABLE' ? (
                            <span
                              style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.3rem',
                                background: '#fef3c7',
                                color: '#92400e',
                                padding: '0.2rem 0.55rem',
                                borderRadius: '4px',
                                fontSize: '0.75rem',
                                fontWeight: 600,
                              }}
                              title="Missing verifiable text or proof of mandatory skills"
                            >
                              <HelpCircle size={13} />
                              Not Verifiable
                            </span>
                          ) : (
                            <span
                              style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.3rem',
                                background: '#fee2e2',
                                color: '#991b1b',
                                padding: '0.2rem 0.55rem',
                                borderRadius: '4px',
                                fontSize: '0.75rem',
                                fontWeight: 600,
                              }}
                              title={
                                m.missingMandatorySkills?.length > 0
                                  ? `Missing mandatory: ${m.missingMandatorySkills.join(', ')}`
                                  : 'Failed minimum criteria'
                              }
                            >
                              <X size={13} />
                              Failed
                            </span>
                          )}
                        </td>

                        {/* Shortlist Toggle */}
                        <td style={{ textAlign: 'center' }}>
                          <button
                            onClick={() => handleToggleShortlist(m)}
                            disabled={!m.applicationId}
                            style={{
                              background: 'transparent',
                              border: 'none',
                              cursor: m.applicationId ? 'pointer' : 'default',
                              color: m.shortlisted ? '#eab308' : '#cbd5e1',
                              padding: '0.3rem',
                              borderRadius: '4px',
                              display: 'inline-flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              transition: 'color 0.15s ease',
                            }}
                            title={
                              m.shortlisted
                                ? 'Shortlisted (Click to remove)'
                                : 'Not shortlisted (Click to add to shortlist)'
                            }
                          >
                            <Star
                              size={20}
                              fill={m.shortlisted ? '#eab308' : 'none'}
                              stroke={m.shortlisted ? '#ca8a04' : '#94a3b8'}
                            />
                          </button>
                        </td>

                        {/* Workflow Stage */}
                        <td>
                          <select
                            value={m.currentStage || 'NEW'}
                            onChange={(e) => handleStageSelect(m, e.target.value as ApplicationStage)}
                            disabled={!m.applicationId}
                            style={{
                              padding: '0.35rem 0.5rem',
                              borderRadius: '6px',
                              border: '1px solid #cbd5e1',
                              fontSize: '0.8rem',
                              fontWeight: 600,
                              background:
                                m.currentStage === 'HIRED'
                                  ? '#dcfce7'
                                  : m.currentStage === 'REJECTED'
                                  ? '#fee2e2'
                                  : m.currentStage === 'INTERVIEW'
                                  ? '#e0e7ff'
                                  : m.currentStage === 'SHORTLISTED'
                                  ? '#fef3c7'
                                  : '#f8fafc',
                              color:
                                m.currentStage === 'HIRED'
                                  ? '#166534'
                                  : m.currentStage === 'REJECTED'
                                  ? '#991b1b'
                                  : m.currentStage === 'INTERVIEW'
                                  ? '#3730a3'
                                  : m.currentStage === 'SHORTLISTED'
                                  ? '#92400e'
                                  : '#334155',
                            }}
                          >
                            <option value="NEW">NEW</option>
                            <option value="REVIEWING">REVIEWING</option>
                            <option value="SHORTLISTED">SHORTLISTED</option>
                            <option value="INTERVIEW">INTERVIEW</option>
                            <option value="HIRED">HIRED</option>
                            <option value="REJECTED">REJECTED</option>
                          </select>
                        </td>

                        {/* Actions */}
                        <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                            {/* Explain Match Button */}
                            <button
                              onClick={() => setSelectedMatchForExplain(m)}
                              className="btn btn-secondary"
                              style={{
                                padding: '0.35rem 0.65rem',
                                fontSize: '0.8rem',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.35rem',
                              }}
                              title="Inspect explainable score breakdown and evidence snippets"
                            >
                              <Info size={14} />
                              <span>Explain</span>
                            </button>

                            {/* Recruiter Notes Button */}
                            {m.applicationId && (
                              <button
                                onClick={() =>
                                  setNotesModalTarget({
                                    applicationId: m.applicationId!,
                                    candidateName: m.candidateName,
                                  })
                                }
                                className="btn btn-secondary"
                                style={{
                                  padding: '0.35rem 0.65rem',
                                  fontSize: '0.8rem',
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: '0.35rem',
                                }}
                                title="View and record recruiter evaluation notes and timeline"
                              >
                                <MessageSquare size={14} />
                                <span>Notes</span>
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* ========================================================================= */}
      {/* MODALS & DRAWERS                                                          */}
      {/* ========================================================================= */}

      {/* 1. Explainability Drawer */}
      <ExplainabilityDrawer
        match={selectedMatchForExplain}
        jobTitle={job.title}
        algorithmVersion={currentMatchRun?.algorithmVersion || '1.0.0-hybrid'}
        onClose={() => setSelectedMatchForExplain(null)}
      />

      {/* 2. Side-by-Side Candidate Comparison Modal */}
      <CandidateComparisonModal
        comparison={comparisonData}
        isOpen={isCompareModalOpen}
        onClose={() => setIsCompareModalOpen(false)}
      />

      {/* 3. Recruiter Notes Modal */}
      {notesModalTarget && (
        <ApplicationNotesModal
          jobId={job.id}
          applicationId={notesModalTarget.applicationId}
          candidateName={notesModalTarget.candidateName}
          isOpen={true}
          onClose={() => setNotesModalTarget(null)}
        />
      )}

      {/* 4. Stage Transition Confirmation Modal */}
      {stageTransitionTarget && (
        <StageConfirmModal
          isOpen={true}
          candidateName={stageTransitionTarget.candidateName}
          currentStage={stageTransitionTarget.currentStage}
          targetStage={stageTransitionTarget.targetStage}
          onConfirm={async (decisionReason, note) => {
            await executeStageTransition(
              stageTransitionTarget.applicationId,
              stageTransitionTarget.targetStage,
              decisionReason,
              note
            );
          }}
          onClose={() => setStageTransitionTarget(null)}
        />
      )}
    </div>
  );
};
