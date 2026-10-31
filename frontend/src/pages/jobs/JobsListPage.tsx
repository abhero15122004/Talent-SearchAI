import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { jobsApi } from '../../api/jobs';
import { JobSummary, JobStatus } from '../../types';
import { extractErrorMessage } from '../../api/client';
import { JobStatusBadge } from '../../components/jobs/JobStatusBadge';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { EmptyState } from '../../components/EmptyState';
import {
  Briefcase,
  Plus,
  Search,
  MapPin,
  ChevronLeft,
  ChevronRight,
  Eye,
  Edit2,
  Archive,
  AlertCircle,
} from 'lucide-react';

const STATUS_TABS: { label: string; value: JobStatus | '' }[] = [
  { label: 'All Positions', value: '' },
  { label: 'Open', value: 'OPEN' },
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Paused', value: 'PAUSED' },
  { label: 'Archived', value: 'ARCHIVED' },
];

export const JobsListPage: React.FC = () => {
  const navigate = useNavigate();
  const [jobs, setJobs] = useState<JobSummary[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Pagination
  const [selectedStatus, setSelectedStatus] = useState<JobStatus | ''>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  const loadJobs = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await jobsApi.getJobs({
        page,
        size: 10,
        status: selectedStatus || undefined,
        search: searchQuery || undefined,
      });
      setJobs(data.content);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  }, [page, selectedStatus, searchQuery]);

  useEffect(() => {
    loadJobs();
  }, [loadJobs]);

  const handleStatusChange = (status: JobStatus | '') => {
    setSelectedStatus(status);
    setPage(0);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    loadJobs();
  };

  const handleArchive = async (id: number, title: string) => {
    if (!window.confirm(`Are you sure you want to archive "${title}"?`)) {
      return;
    }
    try {
      await jobsApi.archiveJob(id);
      loadJobs();
    } catch (err) {
      alert(`Failed to archive job: ${extractErrorMessage(err)}`);
    }
  };

  const formatEmploymentType = (type: string) => {
    return type.replace('_', ' ');
  };

  return (
    <div className="jobs-list-page">
      {/* Page Header */}
      <div className="section-header">
        <div className="section-title-group">
          <h1>Job Requisitions</h1>
          <p>Define role criteria, skill requirements, and multi-factor AI scoring weights</p>
        </div>
        <button
          onClick={() => navigate('/jobs/new')}
          className="btn-primary"
          title="Create a new job requisition"
        >
          <Plus size={18} />
          <span>New Job Requisition</span>
        </button>
      </div>

      {error && (
        <div className="login-alert" role="alert">
          <AlertCircle size={18} className="alert-icon" />
          <span>{error}</span>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="table-controls-card">
        <div className="status-tabs" role="tablist" aria-label="Job Status Filter">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.value}
              role="tab"
              aria-selected={selectedStatus === tab.value}
              className={`status-tab ${selectedStatus === tab.value ? 'status-tab-active' : ''}`}
              onClick={() => handleStatusChange(tab.value)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <form onSubmit={handleSearchSubmit} className="search-box-form">
          <div className="input-wrapper">
            <Search size={16} className="input-icon" aria-hidden="true" />
            <input
              type="text"
              className="form-control form-control-sm"
              placeholder="Search by title, department, or location..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <button type="submit" className="btn-secondary btn-sm">
            Search
          </button>
        </form>
      </div>

      {/* Main Content Area */}
      {isLoading ? (
        <div className="loading-state-container">
          <LoadingSpinner size="medium" />
          <p>Loading job requisitions...</p>
        </div>
      ) : jobs.length === 0 ? (
        <div className="content-card">
          <EmptyState
            icon={Briefcase}
            title={searchQuery || selectedStatus ? 'No Matching Requisitions Found' : 'No Job Requisitions Yet'}
            description={
              searchQuery || selectedStatus
                ? 'Try broadening your search term or selecting a different status filter.'
                : 'Create your first job requisition to begin matching candidates and extracting skills.'
            }
            actionLabel={searchQuery || selectedStatus ? 'Clear Filters' : 'Create Job Requisition'}
            onAction={
              searchQuery || selectedStatus
                ? () => {
                    setSearchQuery('');
                    setSelectedStatus('');
                    setPage(0);
                  }
                : () => navigate('/jobs/new')
            }
          />
        </div>
      ) : (
        <div className="content-card">
          <div className="table-responsive">
            <table className="data-table" aria-label="Job requisitions table">
              <thead>
                <tr>
                  <th>Job Title &amp; Department</th>
                  <th>Location &amp; Type</th>
                  <th>Status</th>
                  <th>Experience</th>
                  <th>Shortlist Target</th>
                  <th>Min Score</th>
                  <th>Skills Count</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {jobs.map((job) => (
                  <tr key={job.id} className="table-row-hover">
                    <td>
                      <div className="job-title-group">
                        <span
                          className="job-title-link"
                          onClick={() => navigate(`/jobs/${job.id}`)}
                          role="button"
                          tabIndex={0}
                        >
                          {job.title}
                        </span>
                        <span className="job-department-tag">{job.department}</span>
                      </div>
                    </td>
                    <td>
                      <div className="location-group">
                        <span className="location-text">
                          <MapPin size={13} className="inline-icon" /> {job.location}
                        </span>
                        <span className="employment-chip">
                          {formatEmploymentType(job.employmentType)}
                        </span>
                      </div>
                    </td>
                    <td>
                      <JobStatusBadge status={job.status} />
                    </td>
                    <td>
                      {job.minYearsExperience}
                      {job.maxYearsExperience ? ` - ${job.maxYearsExperience}` : '+'} yrs
                    </td>
                    <td>
                      <span className="badge-shortlist" title="Candidates to shortlist">
                        {job.shortlistCount} candidates
                      </span>
                    </td>
                    <td>
                      <span className="badge-score-threshold">
                        {job.minScoreThreshold}%
                      </span>
                    </td>
                    <td>
                      <div className="skills-counts">
                        <span className="mandatory-count" title="Mandatory Skills">
                          {job.mandatorySkillsCount} mandatory
                        </span>
                        {job.preferredSkillsCount > 0 && (
                          <span className="preferred-count" title="Preferred Skills">
                            +{job.preferredSkillsCount} preferred
                          </span>
                        )}
                      </div>
                    </td>
                    <td>
                      <div className="table-actions">
                        <button
                          onClick={() => navigate(`/jobs/${job.id}`)}
                          className="action-btn"
                          title="View Requisition Details"
                          aria-label={`View ${job.title}`}
                        >
                          <Eye size={16} />
                        </button>
                        <button
                          onClick={() => navigate(`/jobs/${job.id}/edit`)}
                          className="action-btn"
                          title="Edit Requisition"
                          aria-label={`Edit ${job.title}`}
                        >
                          <Edit2 size={16} />
                        </button>
                        {job.status !== 'ARCHIVED' && (
                          <button
                            onClick={() => handleArchive(job.id, job.title)}
                            className="action-btn action-btn-danger"
                            title="Archive Requisition"
                            aria-label={`Archive ${job.title}`}
                          >
                            <Archive size={16} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination Footer */}
          <div className="table-pagination-footer">
            <span className="pagination-info">
              Showing Page {page + 1} of {totalPages} &bull; Total {totalElements} Requisitions
            </span>
            <div className="pagination-controls">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="btn-pagination"
                aria-label="Previous page"
              >
                <ChevronLeft size={16} />
                <span>Previous</span>
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="btn-pagination"
                aria-label="Next page"
              >
                <span>Next</span>
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
