import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { candidatesApi } from '../../api/candidates';
import { CandidateDetail } from '../../types';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { extractErrorMessage } from '../../api/client';
import { ProfessionalResumeView } from '../../components/candidates/ProfessionalResumeView';

export const CandidateDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [candidate, setCandidate] = useState<CandidateDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showRawText, setShowRawText] = useState(false);

  useEffect(() => {
    if (!id) return;
    const fetchCandidate = async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await candidatesApi.getCandidateById(Number(id));
        setCandidate(res.data);
      } catch (err) {
        setError(extractErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };
    fetchCandidate();
  }, [id]);

  if (loading) {
    return (
      <div style={{ padding: '80px', textAlign: 'center' }}>
        <LoadingSpinner size="large" />
        <p style={{ marginTop: '16px', color: '#697386' }}>Loading candidate profile...</p>
      </div>
    );
  }

  if (error || !candidate) {
    return (
      <div style={{ padding: '40px', maxWidth: '800px', margin: '0 auto', textAlign: 'center' }}>
        <div style={{ padding: '24px', backgroundColor: '#fce8e6', borderRadius: '8px', color: '#c5221f', marginBottom: '20px' }}>
          <strong>Error:</strong> {error || 'Candidate not found'}
        </div>
        <Link to="/candidates" style={{ color: '#635bff', textDecoration: 'none', fontWeight: 600 }}>
          &larr; Back to Candidate Talent Pool
        </Link>
      </div>
    );
  }

  return (
    <div style={{ padding: '32px', maxWidth: '1000px', margin: '0 auto' }}>
      {/* Breadcrumb Navigation */}
      <div style={{ marginBottom: '20px' }}>
        <Link
          to="/candidates"
          style={{
            color: '#635bff',
            textDecoration: 'none',
            fontSize: '14px',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '6px',
          }}
        >
          &larr; Back to Candidate Talent Pool
        </Link>
      </div>

      {/* Header Profile Card */}
      <div
        style={{
          backgroundColor: '#fff',
          padding: '28px',
          borderRadius: '12px',
          border: '1px solid #e3e8ee',
          boxShadow: '0 2px 6px rgba(0,0,0,0.04)',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
          <div
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              backgroundColor: '#e8f0fe',
              color: '#1a73e8',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '22px',
              fontWeight: 700,
            }}
          >
            {candidate.fullName
              ? candidate.fullName.split(' ').map((n) => n[0]).join('').substring(0, 2).toUpperCase()
              : '??'}
          </div>
          <div>
            <h1 style={{ fontSize: '24px', fontWeight: 700, color: '#1a1f36', margin: '0 0 6px 0' }}>
              {candidate.fullName || 'Unnamed Candidate'}
            </h1>
            <div style={{ display: 'flex', gap: '18px', color: '#697386', fontSize: '14px', flexWrap: 'wrap' }}>
              {candidate.email && <span>✉ {candidate.email}</span>}
              {candidate.phone && <span>📞 {candidate.phone}</span>}
              {candidate.sourceFilename && <span>📄 {candidate.sourceFilename}</span>}
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={() => setShowRawText(!showRawText)}
          style={{
            padding: '8px 14px',
            backgroundColor: showRawText ? '#f1f3f4' : '#fff',
            border: '1px solid #dcdfe4',
            borderRadius: '6px',
            fontSize: '13px',
            fontWeight: 600,
            color: '#4f566b',
            cursor: 'pointer',
          }}
        >
          {showRawText ? 'Hide Raw Text' : 'Inspect Verbatim Text'}
        </button>
      </div>

      {/* Raw Extracted Text Drawer */}
      {showRawText && (
        <div
          style={{
            backgroundColor: '#1e293b',
            color: '#f8fafc',
            padding: '20px',
            borderRadius: '10px',
            marginBottom: '24px',
            fontSize: '13px',
            lineHeight: 1.6,
            maxHeight: '380px',
            overflowY: 'auto',
            fontFamily: 'monospace',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', borderBottom: '1px solid #334155', paddingBottom: '6px' }}>
            <span style={{ fontWeight: 600, color: '#94a3b8' }}>Verbatim Apache Tika Text Stream</span>
            <span style={{ color: '#64748b', fontSize: '11px' }}>Raw / Unhallucinated</span>
          </div>
          <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
            {candidate.rawText || 'No raw text stored.'}
          </pre>
        </div>
      )}

      {/* Professional Resume Dossier */}
      <ProfessionalResumeView candidate={candidate} hideHeaderCard={true} />
    </div>
  );
};
