import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { jobsApi } from '../../api/jobs';
import {
  CreateJobPayload,
  EmploymentType,
  JobStatus,
  QualificationType,
  JobSkill,
  JobQualification,
} from '../../types';
import { extractErrorMessage } from '../../api/client';
import { WeightMeter } from '../../components/jobs/WeightMeter';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import {
  Briefcase,
  ArrowLeft,
  Plus,
  Trash2,
  Sparkles,
  AlertCircle,
  Save,
  CheckCircle2,
} from 'lucide-react';

const EMPLOYMENT_TYPES: { label: string; value: EmploymentType }[] = [
  { label: 'Full Time', value: 'FULL_TIME' },
  { label: 'Part Time', value: 'PART_TIME' },
  { label: 'Contract', value: 'CONTRACT' },
  { label: 'Internship', value: 'INTERNSHIP' },
  { label: 'Remote', value: 'REMOTE' },
];

const QUALIFICATION_TYPES: { label: string; value: QualificationType }[] = [
  { label: 'Education', value: 'EDUCATION' },
  { label: 'Certification', value: 'CERTIFICATION' },
  { label: 'Eligibility', value: 'ELIGIBILITY' },
  { label: 'Other Requirement', value: 'OTHER' },
];

export const JobCreateEditPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id?: string }>();
  const isEditMode = Boolean(id);

  const [isLoading, setIsLoading] = useState<boolean>(isEditMode);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  // Form Fields
  const [title, setTitle] = useState('');
  const [department, setDepartment] = useState('');
  const [location, setLocation] = useState('');
  const [employmentType, setEmploymentType] = useState<EmploymentType>('FULL_TIME');
  const [status, setStatus] = useState<JobStatus>('DRAFT');
  const [description, setDescription] = useState('');
  const [minYearsExperience, setMinYearsExperience] = useState<number>(2);
  const [maxYearsExperience, setMaxYearsExperience] = useState<string>('5');

  // Weights & Thresholds
  const [shortlistCount, setShortlistCount] = useState<number>(10);
  const [minScoreThreshold, setMinScoreThreshold] = useState<number>(65);
  const [weightSkillCoverage, setWeightSkillCoverage] = useState<number>(40);
  const [weightExperienceRelevance, setWeightExperienceRelevance] = useState<number>(25);
  const [weightProjectRelevance, setWeightProjectRelevance] = useState<number>(15);
  const [weightSemanticSimilarity, setWeightSemanticSimilarity] = useState<number>(20);

  // Dynamic Lists
  const [skills, setSkills] = useState<JobSkill[]>([
    { skillName: 'Java', mandatory: true, minYearsRequired: 2 },
    { skillName: 'Spring Boot', mandatory: true, minYearsRequired: 2 },
    { skillName: 'React', mandatory: false, minYearsRequired: 1 },
  ]);

  const [qualifications, setQualifications] = useState<JobQualification[]>([
    { qualificationType: 'EDUCATION', requirementText: "Bachelor's Degree in Computer Science or related", mandatory: true },
  ]);

  // Load existing data in Edit mode
  useEffect(() => {
    if (!id) return;
    const loadJobDetails = async () => {
      setIsLoading(true);
      try {
        const job = await jobsApi.getJobById(Number(id));
        setTitle(job.title);
        setDepartment(job.department);
        setLocation(job.location);
        setEmploymentType(job.employmentType);
        setStatus(job.status);
        setDescription(job.description);
        setMinYearsExperience(job.minYearsExperience);
        setMaxYearsExperience(job.maxYearsExperience ? String(job.maxYearsExperience) : '');
        setShortlistCount(job.shortlistCount);
        setMinScoreThreshold(Number(job.minScoreThreshold));
        setWeightSkillCoverage(Number(job.weightSkillCoverage));
        setWeightExperienceRelevance(Number(job.weightExperienceRelevance));
        setWeightProjectRelevance(Number(job.weightProjectRelevance));
        setWeightSemanticSimilarity(Number(job.weightSemanticSimilarity));
        setSkills(job.skills || []);
        setQualifications(job.qualifications || []);
      } catch (err) {
        setErrorMessage(extractErrorMessage(err));
      } finally {
        setIsLoading(false);
      }
    };
    loadJobDetails();
  }, [id]);

  // Skill Handlers
  const handleAddSkill = () => {
    setSkills([...skills, { skillName: '', mandatory: false, minYearsRequired: 0 }]);
  };

  const handleUpdateSkill = (index: number, field: keyof JobSkill, value: unknown) => {
    const updated = [...skills];
    updated[index] = { ...updated[index], [field]: value };
    setSkills(updated);
  };

  const handleRemoveSkill = (index: number) => {
    setSkills(skills.filter((_, idx) => idx !== index));
  };

  // Qualification Handlers
  const handleAddQualification = () => {
    setQualifications([
      ...qualifications,
      { qualificationType: 'EDUCATION', requirementText: '', mandatory: true },
    ]);
  };

  const handleUpdateQualification = (index: number, field: keyof JobQualification, value: unknown) => {
    const updated = [...qualifications];
    updated[index] = { ...updated[index], [field]: value };
    setQualifications(updated);
  };

  const handleRemoveQualification = (index: number) => {
    setQualifications(qualifications.filter((_, idx) => idx !== index));
  };

  // Weight Calculation
  const totalWeight = Number(
    (weightSkillCoverage + weightExperienceRelevance + weightProjectRelevance + weightSemanticSimilarity).toFixed(2)
  );
  const isWeightSumValid = Math.abs(totalWeight - 100) < 0.01;

  // Validation
  const validateClientSide = (): boolean => {
    const errors: Record<string, string> = {};

    if (!title.trim()) errors.title = 'Job title is required';
    if (!department.trim()) errors.department = 'Department is required';
    if (!location.trim()) errors.location = 'Location is required';
    if (!description.trim()) errors.description = 'Job description is required';

    if (minYearsExperience < 0) errors.minYearsExperience = 'Minimum experience cannot be negative';
    if (maxYearsExperience.trim() !== '') {
      const maxVal = Number(maxYearsExperience);
      if (isNaN(maxVal) || maxVal < minYearsExperience) {
        errors.maxYearsExperience = 'Maximum experience cannot be less than minimum experience';
      }
    }

    if (!isWeightSumValid) {
      errors.weights = `Matching weights must sum exactly to 100% (currently ${totalWeight}%)`;
    }

    if (shortlistCount < 1 || shortlistCount > 500) {
      errors.shortlistCount = 'Shortlist target must be between 1 and 500 candidates';
    }

    if (minScoreThreshold < 0 || minScoreThreshold > 100) {
      errors.minScoreThreshold = 'Minimum score threshold must be between 0 and 100';
    }

    // Filter out empty skills
    const validSkills = skills.filter((s) => s.skillName.trim() !== '');
    if (validSkills.length === 0) {
      errors.skills = 'Please specify at least one skill requirement for the role';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!validateClientSide()) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    setIsSubmitting(true);

    const payload: CreateJobPayload = {
      title: title.trim(),
      department: department.trim(),
      location: location.trim(),
      employmentType,
      status,
      description: description.trim(),
      minYearsExperience,
      maxYearsExperience: maxYearsExperience.trim() !== '' ? Number(maxYearsExperience) : null,
      shortlistCount,
      minScoreThreshold,
      weightSkillCoverage,
      weightExperienceRelevance,
      weightProjectRelevance,
      weightSemanticSimilarity,
      skills: skills.filter((s) => s.skillName.trim() !== ''),
      qualifications: qualifications.filter((q) => q.requirementText.trim() !== ''),
    };

    try {
      if (isEditMode && id) {
        await jobsApi.updateJob(Number(id), payload);
      } else {
        await jobsApi.createJob(payload);
      }
      navigate('/jobs');
    } catch (err) {
      setErrorMessage(extractErrorMessage(err));
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="loading-state-container">
        <LoadingSpinner size="large" />
        <p>Loading requisition editor...</p>
      </div>
    );
  }

  return (
    <div className="job-editor-page">
      {/* Header */}
      <div className="section-header">
        <div className="section-title-group">
          <button onClick={() => navigate('/jobs')} className="btn-back-link">
            <ArrowLeft size={16} />
            <span>Back to Job Requisitions</span>
          </button>
          <h1>{isEditMode ? 'Edit Job Requisition' : 'Create Job Requisition'}</h1>
          <p>Configure candidate qualification criteria, required skills, and AI scoring weights</p>
        </div>
      </div>

      {errorMessage && (
        <div className="login-alert" role="alert">
          <AlertCircle size={18} className="alert-icon" />
          <span>{errorMessage}</span>
        </div>
      )}

      {Object.keys(validationErrors).length > 0 && (
        <div className="login-alert" role="alert">
          <AlertCircle size={18} className="alert-icon" />
          <div>
            <strong>Please resolve the following issues:</strong>
            <ul className="error-bullet-list">
              {Object.values(validationErrors).map((err, i) => (
                <li key={i}>{err}</li>
              ))}
            </ul>
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        {/* Section 1: Role Overview */}
        <div className="content-card mb-4">
          <div className="card-header">
            <div className="card-title-group">
              <Briefcase size={18} className="card-icon text-blue" />
              <h2>1. Role Overview &amp; Position Details</h2>
            </div>
          </div>
          <div className="card-body">
            <div className="form-grid-2">
              <div className="form-group">
                <label htmlFor="job-title" className="form-label">
                  Job Title <span className="text-danger">*</span>
                </label>
                <input
                  id="job-title"
                  type="text"
                  className={`form-control ${validationErrors.title ? 'is-invalid' : ''}`}
                  placeholder="e.g. Senior Backend Engineer"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="job-department" className="form-label">
                  Department <span className="text-danger">*</span>
                </label>
                <input
                  id="job-department"
                  type="text"
                  className={`form-control ${validationErrors.department ? 'is-invalid' : ''}`}
                  placeholder="e.g. Engineering / Cloud Infrastructure"
                  value={department}
                  onChange={(e) => setDepartment(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="job-location" className="form-label">
                  Location <span className="text-danger">*</span>
                </label>
                <input
                  id="job-location"
                  type="text"
                  className={`form-control ${validationErrors.location ? 'is-invalid' : ''}`}
                  placeholder="e.g. Remote, San Francisco, CA"
                  value={location}
                  onChange={(e) => setLocation(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="job-type" className="form-label">
                  Employment Type <span className="text-danger">*</span>
                </label>
                <select
                  id="job-type"
                  className="form-control"
                  value={employmentType}
                  onChange={(e) => setEmploymentType(e.target.value as EmploymentType)}
                >
                  {EMPLOYMENT_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>
                      {t.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="job-min-exp" className="form-label">
                  Minimum Experience (Years) <span className="text-danger">*</span>
                </label>
                <input
                  id="job-min-exp"
                  type="number"
                  min="0"
                  max="40"
                  className={`form-control ${validationErrors.minYearsExperience ? 'is-invalid' : ''}`}
                  value={minYearsExperience}
                  onChange={(e) => setMinYearsExperience(Math.max(0, parseInt(e.target.value) || 0))}
                />
              </div>

              <div className="form-group">
                <label htmlFor="job-max-exp" className="form-label">
                  Maximum Experience (Years, Optional)
                </label>
                <input
                  id="job-max-exp"
                  type="number"
                  min="0"
                  max="40"
                  placeholder="Leave empty for open-ended"
                  className={`form-control ${validationErrors.maxYearsExperience ? 'is-invalid' : ''}`}
                  value={maxYearsExperience}
                  onChange={(e) => setMaxYearsExperience(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label htmlFor="job-status" className="form-label">
                  Requisition Status
                </label>
                <select
                  id="job-status"
                  className="form-control"
                  value={status}
                  onChange={(e) => setStatus(e.target.value as JobStatus)}
                >
                  <option value="DRAFT">DRAFT (Internal drafting)</option>
                  <option value="OPEN">OPEN (Active candidate pipeline)</option>
                  <option value="PAUSED">PAUSED (Review hold)</option>
                  <option value="CLOSED">CLOSED (Role filled)</option>
                </select>
              </div>
            </div>

            <div className="form-group mt-3">
              <label htmlFor="job-description" className="form-label">
                Job Description &amp; Responsibilities <span className="text-danger">*</span>
              </label>
              <textarea
                id="job-description"
                rows={6}
                className={`form-control ${validationErrors.description ? 'is-invalid' : ''}`}
                placeholder="Describe role responsibilities, core deliverables, and organizational impact..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
              />
            </div>
          </div>
        </div>

        {/* Section 2: Skills & Competency Matrix */}
        <div className="content-card mb-4">
          <div className="card-header">
            <div className="card-title-group">
              <CheckCircle2 size={18} className="card-icon text-emerald" />
              <h2>2. Required &amp; Preferred Technical Skills</h2>
            </div>
            <button
              type="button"
              onClick={handleAddSkill}
              className="btn-secondary btn-sm"
            >
              <Plus size={15} />
              <span>Add Skill</span>
            </button>
          </div>

          <div className="card-body">
            <p className="section-description">
              Define mandatory skills (strictly required for candidate qualification) versus preferred skills (scoring multipliers).
            </p>

            <div className="dynamic-list">
              {skills.map((skill, idx) => (
                <div key={idx} className="dynamic-row">
                  <div className="row-col flex-2">
                    <label className="sub-label">Skill Name</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. Python, Docker, PostgreSQL"
                      value={skill.skillName}
                      onChange={(e) => handleUpdateSkill(idx, 'skillName', e.target.value)}
                    />
                  </div>

                  <div className="row-col flex-1">
                    <label className="sub-label">Min Experience</label>
                    <input
                      type="number"
                      min="0"
                      max="30"
                      className="form-control"
                      value={skill.minYearsRequired}
                      onChange={(e) =>
                        handleUpdateSkill(idx, 'minYearsRequired', Math.max(0, parseInt(e.target.value) || 0))
                      }
                    />
                  </div>

                  <div className="row-col flex-auto">
                    <label className="sub-label">Skill Type</label>
                    <div className="toggle-wrapper">
                      <label className="checkbox-label">
                        <input
                          type="checkbox"
                          checked={skill.mandatory}
                          onChange={(e) => handleUpdateSkill(idx, 'mandatory', e.target.checked)}
                        />
                        <span>Mandatory</span>
                      </label>
                    </div>
                  </div>

                  <div className="row-col flex-action">
                    <button
                      type="button"
                      onClick={() => handleRemoveSkill(idx)}
                      className="action-btn action-btn-danger"
                      title="Remove skill"
                      aria-label="Remove skill"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Section 3: Qualifications & Eligibility */}
        <div className="content-card mb-4">
          <div className="card-header">
            <div className="card-title-group">
              <CheckCircle2 size={18} className="card-icon text-purple" />
              <h2>3. Qualifications &amp; Eligibility Criteria</h2>
            </div>
            <button
              type="button"
              onClick={handleAddQualification}
              className="btn-secondary btn-sm"
            >
              <Plus size={15} />
              <span>Add Requirement</span>
            </button>
          </div>

          <div className="card-body">
            <p className="section-description">
              Specify non-skill eligibility rules such as degree requirements, professional certifications, or work authorization.
            </p>

            <div className="dynamic-list">
              {qualifications.map((qual, idx) => (
                <div key={idx} className="dynamic-row">
                  <div className="row-col flex-1">
                    <label className="sub-label">Type</label>
                    <select
                      className="form-control"
                      value={qual.qualificationType}
                      onChange={(e) =>
                        handleUpdateQualification(idx, 'qualificationType', e.target.value as QualificationType)
                      }
                    >
                      {QUALIFICATION_TYPES.map((t) => (
                        <option key={t.value} value={t.value}>
                          {t.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="row-col flex-3">
                    <label className="sub-label">Requirement Description</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. AWS Certified Solutions Architect, Master's Degree"
                      value={qual.requirementText}
                      onChange={(e) => handleUpdateQualification(idx, 'requirementText', e.target.value)}
                    />
                  </div>

                  <div className="row-col flex-auto">
                    <label className="sub-label">Required?</label>
                    <div className="toggle-wrapper">
                      <label className="checkbox-label">
                        <input
                          type="checkbox"
                          checked={qual.mandatory}
                          onChange={(e) => handleUpdateQualification(idx, 'mandatory', e.target.checked)}
                        />
                        <span>Mandatory</span>
                      </label>
                    </div>
                  </div>

                  <div className="row-col flex-action">
                    <button
                      type="button"
                      onClick={() => handleRemoveQualification(idx)}
                      className="action-btn action-btn-danger"
                      title="Remove qualification"
                      aria-label="Remove qualification"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Section 4: AI Matching Weights & Shortlist Thresholds */}
        <div className="content-card mb-4">
          <div className="card-header">
            <div className="card-title-group">
              <Sparkles size={18} className="card-icon text-amber" />
              <h2>4. AI Matching Algorithm Weights &amp; Shortlist Rules</h2>
            </div>
          </div>

          <div className="card-body">
            <p className="section-description">
              Calibrate the multi-factor candidate ranking engine. The 4 weights define how candidate suitability is scored and must sum exactly to <strong>100%</strong>.
            </p>

            <WeightMeter
              skillWeight={weightSkillCoverage}
              expWeight={weightExperienceRelevance}
              projectWeight={weightProjectRelevance}
              semanticWeight={weightSemanticSimilarity}
            />

            <div className="form-grid-4 mt-4">
              <div className="form-group">
                <label className="form-label">
                  Skill Coverage (%)
                </label>
                <input
                  type="number"
                  min="0"
                  max="100"
                  className="form-control"
                  value={weightSkillCoverage}
                  onChange={(e) => setWeightSkillCoverage(Number(e.target.value) || 0)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">
                  Experience Relevance (%)
                </label>
                <input
                  type="number"
                  min="0"
                  max="100"
                  className="form-control"
                  value={weightExperienceRelevance}
                  onChange={(e) => setWeightExperienceRelevance(Number(e.target.value) || 0)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">
                  Project Relevance (%)
                </label>
                <input
                  type="number"
                  min="0"
                  max="100"
                  className="form-control"
                  value={weightProjectRelevance}
                  onChange={(e) => setWeightProjectRelevance(Number(e.target.value) || 0)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">
                  Semantic Similarity (%)
                </label>
                <input
                  type="number"
                  min="0"
                  max="100"
                  className="form-control"
                  value={weightSemanticSimilarity}
                  onChange={(e) => setWeightSemanticSimilarity(Number(e.target.value) || 0)}
                />
              </div>
            </div>

            <hr className="divider-subtle" />

            <div className="form-grid-2">
              <div className="form-group">
                <label htmlFor="shortlist-count" className="form-label">
                  Shortlist Candidate Target <span className="text-danger">*</span>
                </label>
                <input
                  id="shortlist-count"
                  type="number"
                  min="1"
                  max="500"
                  className="form-control"
                  value={shortlistCount}
                  onChange={(e) => setShortlistCount(Math.max(1, parseInt(e.target.value) || 1))}
                />
                <span className="field-hint">
                  The target number of top-qualifying candidates to advance to recruiter review.
                </span>
              </div>

              <div className="form-group">
                <label htmlFor="score-threshold" className="form-label">
                  Minimum Score Threshold (%) <span className="text-danger">*</span>
                </label>
                <input
                  id="score-threshold"
                  type="number"
                  min="0"
                  max="100"
                  className="form-control"
                  value={minScoreThreshold}
                  onChange={(e) => setMinScoreThreshold(Number(e.target.value) || 0)}
                />
                <span className="field-hint">
                  Strict cutoff threshold: candidates scoring below this mark will never be shortlisted automatically.
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="form-actions-bar">
          <button
            type="button"
            onClick={() => navigate('/jobs')}
            className="btn-secondary"
            disabled={isSubmitting}
          >
            Cancel
          </button>

          <button
            type="submit"
            className="btn-primary"
            disabled={isSubmitting || !isWeightSumValid}
            title={!isWeightSumValid ? 'Weights must sum to 100% to submit' : 'Save Requisition'}
          >
            {isSubmitting ? (
              <>
                <LoadingSpinner size="small" />
                <span>Saving Requisition...</span>
              </>
            ) : (
              <>
                <Save size={16} />
                <span>{isEditMode ? 'Update Requisition' : 'Create Requisition'}</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};
