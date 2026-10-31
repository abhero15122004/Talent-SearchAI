import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { dashboardApi, DashboardStats } from '../api/dashboard';
import {
  Briefcase,
  Users,
  FileCheck2,
  Sparkles,
  Building2,
  Activity,
  PlusCircle,
  UploadCloud,
  Search,
  RefreshCw,
  Layers,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { EmptyState } from '../components/EmptyState';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboardStats = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await dashboardApi.getStats(user?.organizationId ?? undefined);
      setStats(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to retrieve live dashboard statistics.');
    } finally {
      setLoading(false);
    }
  }, [user?.organizationId]);

  useEffect(() => {
    fetchDashboardStats();
  }, [fetchDashboardStats]);

  const metrics = [
    {
      title: 'Active Job Openings',
      value: stats ? stats.activeJobsCount.toString() : '—',
      subtitle: stats ? `${stats.totalJobsCount} total requisitions` : 'Scoped to organization',
      icon: Briefcase,
      color: 'metric-blue',
      link: '/jobs',
    },
    {
      title: 'Candidate Talent Pool',
      value: stats ? stats.candidateProfilesCount.toString() : '—',
      subtitle: stats ? `${stats.shortlistedCandidatesCount} shortlisted across jobs` : 'Ingested into pipeline',
      icon: Users,
      color: 'metric-purple',
      link: '/candidates',
    },
    {
      title: 'Resumes Extracted',
      value: stats ? stats.resumesProcessedCount.toString() : '—',
      subtitle: stats ? `${stats.resumesFailedCount} failed / malformed` : 'Parsed via Apache Tika',
      icon: FileCheck2,
      color: 'metric-emerald',
      link: '/resumes',
    },
    {
      title: 'AI Matches Evaluated',
      value: stats ? stats.aiMatchesEvaluatedCount.toString() : '—',
      subtitle: 'all-MiniLM-L6-v2 vector scores',
      icon: Sparkles,
      color: 'metric-amber',
      link: '/jobs',
    },
  ];

  return (
    <div className="dashboard-page" style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Welcome Banner */}
      <div className="welcome-banner" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div className="welcome-content">
          <h1>Welcome, {user?.fullName || 'Recruiter'}</h1>
          <p>
            Connected to <strong>{user?.organizationName || 'TalentMatch AI'}</strong> &bull;{' '}
            Role: <span className="role-chip">{user?.role}</span>
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button
            onClick={fetchDashboardStats}
            disabled={loading}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '8px 14px',
              background: '#ffffff',
              border: '1px solid #cbd5e1',
              borderRadius: '6px',
              fontSize: '0.85rem',
              fontWeight: 600,
              color: '#334155',
              cursor: loading ? 'not-allowed' : 'pointer',
              boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
            }}
            title="Refresh Dashboard Data"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            <span>Refresh</span>
          </button>
          <div className="tenant-status-indicator">
            <Building2 size={16} />
            <span>Tenant #{user?.organizationId ?? '1'}</span>
          </div>
        </div>
      </div>

      {error && (
        <div
          style={{
            padding: '14px 18px',
            background: '#fef2f2',
            border: '1px solid #fecaca',
            borderRadius: '8px',
            color: '#991b1b',
            fontSize: '0.9rem',
          }}
        >
          <strong>Notice:</strong> {error}
        </div>
      )}

      {/* Metrics Row */}
      <div className="metrics-grid">
        {metrics.map((metric, idx) => {
          const Icon = metric.icon;
          return (
            <Link
              key={idx}
              to={metric.link}
              style={{ textDecoration: 'none', color: 'inherit' }}
              className={`metric-card ${metric.color}`}
            >
              <div className="metric-header">
                <span className="metric-title">{metric.title}</span>
                <div className="metric-icon-box">
                  <Icon size={20} />
                </div>
              </div>
              <div className="metric-value">{metric.value}</div>
              <div className="metric-subtitle">{metric.subtitle}</div>
            </Link>
          );
        })}
      </div>

      {/* Quick Action Navigation Buttons */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
          gap: '14px',
        }}
      >
        <Link
          to="/jobs/create"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '14px 18px',
            background: '#ffffff',
            borderRadius: '8px',
            border: '1px solid #e2e8f0',
            textDecoration: 'none',
            color: '#1e293b',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            transition: 'all 0.15s ease',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#4f46e5')}
          onMouseLeave={(e) => (e.currentTarget.style.borderColor = '#e2e8f0')}
        >
          <div style={{ background: '#e0e7ff', color: '#4338ca', padding: '10px', borderRadius: '8px' }}>
            <PlusCircle size={20} />
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>Create Job Requisition</div>
            <div style={{ fontSize: '0.78rem', color: '#64748b' }}>Configure skills, weights &amp; targets</div>
          </div>
        </Link>

        <Link
          to="/resumes"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '14px 18px',
            background: '#ffffff',
            borderRadius: '8px',
            border: '1px solid #e2e8f0',
            textDecoration: 'none',
            color: '#1e293b',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            transition: 'all 0.15s ease',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#10b981')}
          onMouseLeave={(e) => (e.currentTarget.style.borderColor = '#e2e8f0')}
        >
          <div style={{ background: '#dcfce7', color: '#166534', padding: '10px', borderRadius: '8px' }}>
            <UploadCloud size={20} />
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>Bulk Resume Ingestion</div>
            <div style={{ fontSize: '0.78rem', color: '#64748b' }}>Upload ZIP / PDF folders</div>
          </div>
        </Link>

        <Link
          to="/candidates"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '14px 18px',
            background: '#ffffff',
            borderRadius: '8px',
            border: '1px solid #e2e8f0',
            textDecoration: 'none',
            color: '#1e293b',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            transition: 'all 0.15s ease',
          }}
          onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#8b5cf6')}
          onMouseLeave={(e) => (e.currentTarget.style.borderColor = '#e2e8f0')}
        >
          <div style={{ background: '#f3e8ff', color: '#7e22ce', padding: '10px', borderRadius: '8px' }}>
            <Search size={20} />
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>Talent Pool Search</div>
            <div style={{ fontSize: '0.78rem', color: '#64748b' }}>Search parsed candidate resumes</div>
          </div>
        </Link>
      </div>

      {/* Two Column Layout: Recruitment Stage Breakdown & Active Requisitions */}
      <div className="dashboard-grid">
        {/* Recruitment Pipeline Funnel Card */}
        <div className="content-card">
          <div className="card-header">
            <div className="card-title-group">
              <Layers className="card-icon text-purple" size={20} />
              <h2>Application Funnel Pipeline</h2>
            </div>
            <span className="badge-phase">
              {stats ? `${stats.totalApplicationsCount} Applications` : 'Live Pipeline'}
            </span>
          </div>

          <div className="card-body">
            {stats && Object.keys(stats.stageBreakdown).length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {Object.entries(stats.stageBreakdown).map(([stage, count]) => {
                  const total = stats.totalApplicationsCount || 1;
                  const pct = ((count / total) * 100).toFixed(0);

                  const stageColor =
                    stage === 'HIRED'
                      ? '#16a34a'
                      : stage === 'SHORTLISTED'
                      ? '#d97706'
                      : stage === 'INTERVIEW'
                      ? '#4f46e5'
                      : stage === 'REVIEWING'
                      ? '#0284c7'
                      : stage === 'REJECTED'
                      ? '#dc2626'
                      : '#64748b';

                  return (
                    <div key={stage}>
                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          fontSize: '0.85rem',
                          marginBottom: '4px',
                        }}
                      >
                        <span style={{ fontWeight: 600, color: '#334155' }}>{stage}</span>
                        <span style={{ color: '#64748b', fontSize: '0.8rem' }}>
                          <strong>{count}</strong> ({pct}%)
                        </span>
                      </div>
                      <div
                        style={{
                          width: '100%',
                          height: '7px',
                          background: '#f1f5f9',
                          borderRadius: '4px',
                          overflow: 'hidden',
                        }}
                      >
                        <div
                          style={{
                            width: `${Math.max(2, (count / total) * 100)}%`,
                            height: '100%',
                            background: stageColor,
                            borderRadius: '4px',
                          }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p style={{ color: '#94a3b8', fontSize: '0.9rem', fontStyle: 'italic', margin: 0 }}>
                No active candidate applications recorded yet.
              </p>
            )}
          </div>
        </div>

        {/* Active Job Requisitions Overview */}
        <div className="content-card">
          <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div className="card-title-group">
              <Briefcase className="card-icon text-blue" size={20} />
              <h2>Recent Job Requisitions</h2>
            </div>
            <Link to="/jobs" style={{ fontSize: '0.82rem', color: '#4f46e5', fontWeight: 600, textDecoration: 'none' }}>
              View All &rarr;
            </Link>
          </div>

          <div className="card-body">
            {stats && stats.recentJobs.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {stats.recentJobs.map((job) => (
                  <Link
                    key={job.id}
                    to={`/jobs/${job.id}`}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '12px 14px',
                      background: '#f8fafc',
                      borderRadius: '6px',
                      border: '1px solid #e2e8f0',
                      textDecoration: 'none',
                      color: 'inherit',
                      transition: 'all 0.15s ease',
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#cbd5e1')}
                    onMouseLeave={(e) => (e.currentTarget.style.borderColor = '#e2e8f0')}
                  >
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#0f172a' }}>{job.title}</div>
                      <div style={{ display: 'flex', gap: '10px', fontSize: '0.78rem', color: '#64748b', marginTop: '2px' }}>
                        <span>{job.department}</span>
                        {job.location && <span>• {job.location}</span>}
                      </div>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <span
                        style={{
                          fontSize: '0.75rem',
                          padding: '2px 8px',
                          borderRadius: '4px',
                          fontWeight: 700,
                          background: job.status === 'OPEN' ? '#dcfce7' : '#f1f5f9',
                          color: job.status === 'OPEN' ? '#166534' : '#64748b',
                        }}
                      >
                        {job.status}
                      </span>
                      <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '4px' }}>
                        <strong>{job.candidateCount}</strong> candidates ({job.shortlistedCount} starred)
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '24px 0' }}>
                <p style={{ color: '#64748b', fontSize: '0.88rem', margin: '0 0 10px 0' }}>No job requisitions created yet.</p>
                <Link to="/jobs/create" className="btn btn-primary" style={{ fontSize: '0.8rem', padding: '6px 12px' }}>
                  Create First Job Requisition
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Recent System & Recruiter Audit Trail */}
      <div className="content-card">
        <div className="card-header">
          <div className="card-title-group">
            <Activity className="card-icon text-emerald" size={20} />
            <h2>Recent Recruiter &amp; Match Actions</h2>
          </div>
          <span className="badge-secure">Tamper-Evident Audit Trail</span>
        </div>

        <div className="card-body">
          {stats && stats.recentActivities.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {stats.recentActivities.map((act) => (
                <div
                  key={act.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '10px 14px',
                    borderRadius: '6px',
                    border: '1px solid #f1f5f9',
                    background: '#f8fafc',
                    fontSize: '0.84rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <span
                      style={{
                        padding: '2px 8px',
                        borderRadius: '4px',
                        fontSize: '0.74rem',
                        fontWeight: 700,
                        background: '#e0e7ff',
                        color: '#4338ca',
                        textTransform: 'uppercase',
                      }}
                    >
                      {act.action}
                    </span>
                    <span style={{ color: '#1e293b', fontWeight: 500 }}>{act.details || `${act.action} on ${act.entityType}`}</span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '0.78rem', color: '#64748b' }}>
                    <span>{act.performedBy}</span>
                    <span>{act.timestamp ? new Date(act.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={Activity}
              title="Pipeline Ready for Data Ingestion"
              description="No recruitment activities or candidate submissions recorded yet for this organization. Create a job requisition or ingest resumes to begin."
              badge="Auditing Active"
            />
          )}
        </div>
      </div>
    </div>
  );
};
