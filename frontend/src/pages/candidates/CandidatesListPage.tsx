import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Users } from 'lucide-react';
import { candidatesApi } from '../../api/candidates';
import { CandidateSummary } from '../../types';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { EmptyState } from '../../components/EmptyState';
import { extractErrorMessage } from '../../api/client';

export const CandidatesListPage: React.FC = () => {
  const [candidates, setCandidates] = useState<CandidateSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Debounce search query
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(0);
    }, 350);
    return () => clearTimeout(handler);
  }, [search]);

  const fetchCandidates = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await candidatesApi.getCandidates({
        search: debouncedSearch || undefined,
        page,
        size: 10,
      });
      setCandidates(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, page]);

  useEffect(() => {
    fetchCandidates();
  }, [fetchCandidates]);

  const renderSkills = (skillsString: string | null) => {
    if (!skillsString) return <span style={{ color: '#8792a2', fontSize: '12px' }}>No skills listed</span>;
    const skills = skillsString.split(',').map((s) => s.trim()).filter(Boolean);
    const displayed = skills.slice(0, 4);
    const extra = skills.length - displayed.length;

    return (
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
        {displayed.map((skill) => (
          <span
            key={skill}
            style={{
              padding: '2px 8px',
              borderRadius: '4px',
              backgroundColor: '#f0f4ff',
              color: '#3b5998',
              fontSize: '11px',
              fontWeight: 600,
            }}
          >
            {skill}
          </span>
        ))}
        {extra > 0 && (
          <span
            style={{
              padding: '2px 6px',
              borderRadius: '4px',
              backgroundColor: '#f1f3f4',
              color: '#5f6368',
              fontSize: '11px',
              fontWeight: 600,
            }}
          >
            +{extra} more
          </span>
        )}
      </div>
    );
  };

  return (
    <div style={{ padding: '32px', maxWidth: '1200px', margin: '0 auto' }}>
      {/* Header */}
      <div style={{ marginBottom: '24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <h1 style={{ fontSize: '26px', fontWeight: 700, color: '#1a1f36', margin: '0 0 6px 0' }}>
            Candidate Talent Pool
          </h1>
          <p style={{ color: '#697386', margin: 0, fontSize: '14px' }}>
            {totalElements} candidates ingested and indexed across all resume batches.
          </p>
        </div>
        <Link
          to="/resumes"
          style={{
            padding: '10px 18px',
            backgroundColor: '#635bff',
            color: '#fff',
            borderRadius: '8px',
            textDecoration: 'none',
            fontWeight: 600,
            fontSize: '14px',
            boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
          }}
        >
          + Upload Resumes
        </Link>
      </div>

      {/* Filter / Search Bar */}
      <div
        style={{
          backgroundColor: '#fff',
          padding: '16px 20px',
          borderRadius: '10px',
          border: '1px solid #e3e8ee',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
        }}
      >
        <div style={{ flex: 1, position: 'relative' }}>
          <input
            type="text"
            placeholder="Search candidates by name, email, or skill (e.g. Java, Python, React)..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: '100%',
              padding: '10px 14px',
              border: '1px solid #dcdfe4',
              borderRadius: '6px',
              fontSize: '14px',
            }}
          />
        </div>
        {search && (
          <button
            type="button"
            onClick={() => setSearch('')}
            style={{
              padding: '8px 14px',
              backgroundColor: '#f1f3f4',
              border: 'none',
              borderRadius: '6px',
              color: '#4f566b',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Clear
          </button>
        )}
      </div>

      {/* Table Content */}
      {error && (
        <div style={{ padding: '14px 18px', backgroundColor: '#fce8e6', borderRadius: '8px', color: '#c5221f', marginBottom: '20px' }}>
          {error}
        </div>
      )}

      {loading ? (
        <div style={{ padding: '60px', textAlign: 'center' }}>
          <LoadingSpinner size="large" />
          <p style={{ marginTop: '16px', color: '#697386', fontSize: '14px' }}>Loading candidate directory...</p>
        </div>
      ) : candidates.length === 0 ? (
        <EmptyState
          icon={Users}
          title={search ? 'No candidates match your search' : 'No candidates ingested yet'}
          description={
            search
              ? 'Try modifying your search terms or keywords.'
              : 'Upload a folder or ZIP file of resumes to extract talent profiles.'
          }
          actionLabel={search ? undefined : 'Upload Resumes'}
          onAction={search ? undefined : () => { window.location.href = '/resumes'; }}
        />
      ) : (
        <div
          style={{
            backgroundColor: '#fff',
            borderRadius: '10px',
            border: '1px solid #e3e8ee',
            overflow: 'hidden',
            boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
          }}
        >
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ backgroundColor: '#f8fafc', borderBottom: '1px solid #e3e8ee', color: '#4f566b' }}>
                <th style={{ padding: '14px 18px' }}>Candidate Name</th>
                <th style={{ padding: '14px 18px' }}>Contact</th>
                <th style={{ padding: '14px 18px' }}>Extracted Skills</th>
                <th style={{ padding: '14px 18px' }}>Source File</th>
                <th style={{ padding: '14px 18px', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {candidates.map((c) => {
                const initials = c.fullName
                  ? c.fullName.split(' ').map((n) => n[0]).join('').substring(0, 2).toUpperCase()
                  : '??';

                return (
                  <tr key={c.id} style={{ borderBottom: '1px solid #f0f2f5' }}>
                    <td style={{ padding: '14px 18px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                        <div
                          style={{
                            width: '36px',
                            height: '36px',
                            borderRadius: '50%',
                            backgroundColor: '#e8f0fe',
                            color: '#1a73e8',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            fontWeight: 700,
                            fontSize: '13px',
                          }}
                        >
                          {initials}
                        </div>
                        <div>
                          <div style={{ fontWeight: 600, color: '#1a1f36' }}>
                            {c.fullName || 'Unnamed Candidate'}
                          </div>
                          <div style={{ fontSize: '12px', color: '#8792a2' }}>
                            ID: #{c.id}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td style={{ padding: '14px 18px' }}>
                      <div style={{ color: '#1a1f36' }}>{c.email || '—'}</div>
                      <div style={{ fontSize: '12px', color: '#697386' }}>{c.phone || '—'}</div>
                    </td>
                    <td style={{ padding: '14px 18px', maxWidth: '320px' }}>
                      {renderSkills(c.primarySkills)}
                    </td>
                    <td style={{ padding: '14px 18px', color: '#697386', fontSize: '13px' }}>
                      {c.sourceFilename || '—'}
                    </td>
                    <td style={{ padding: '14px 18px', textAlign: 'right' }}>
                      <Link
                        to={`/candidates/${c.id}`}
                        style={{
                          padding: '6px 12px',
                          backgroundColor: '#f8fafc',
                          border: '1px solid #dcdfe4',
                          borderRadius: '6px',
                          color: '#4f566b',
                          textDecoration: 'none',
                          fontWeight: 600,
                          fontSize: '12px',
                        }}
                      >
                        View Profile &rarr;
                      </Link>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>

          {/* Pagination */}
          {totalPages > 1 && (
            <div
              style={{
                padding: '14px 18px',
                borderTop: '1px solid #e3e8ee',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                backgroundColor: '#fafbfc',
                fontSize: '13px',
                color: '#697386',
              }}
            >
              <span>
                Page {page + 1} of {totalPages}
              </span>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  type="button"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  style={{
                    padding: '5px 12px',
                    border: '1px solid #dcdfe4',
                    borderRadius: '4px',
                    backgroundColor: '#fff',
                    cursor: page === 0 ? 'not-allowed' : 'pointer',
                    opacity: page === 0 ? 0.5 : 1,
                  }}
                >
                  Previous
                </button>
                <button
                  type="button"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                  style={{
                    padding: '5px 12px',
                    border: '1px solid #dcdfe4',
                    borderRadius: '4px',
                    backgroundColor: '#fff',
                    cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
                    opacity: page >= totalPages - 1 ? 0.5 : 1,
                  }}
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
