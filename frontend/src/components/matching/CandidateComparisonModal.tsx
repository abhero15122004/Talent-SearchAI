import React from 'react';
import { CandidateComparison } from '../../types';
import { X, CheckCircle2, XCircle, Scale, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';

interface CandidateComparisonModalProps {
  comparison: CandidateComparison | null;
  isOpen?: boolean;
  onClose: () => void;
}

export const CandidateComparisonModal: React.FC<CandidateComparisonModalProps> = ({
  comparison,
  isOpen = true,
  onClose,
}) => {
  if (!isOpen || !comparison) return null;

  const candidates = comparison.candidates;

  return (
    <div className="modal-overlay" style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(15, 23, 42, 0.6)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1100,
      padding: '24px',
    }}>
      <div className="modal-box" style={{
        background: '#ffffff',
        borderRadius: '12px',
        width: '90%',
        maxWidth: '1200px',
        maxHeight: '90vh',
        display: 'flex',
        flexDirection: 'column',
        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
        overflow: 'hidden',
      }}>
        {/* Modal Header */}
        <div style={{
          padding: '18px 24px',
          borderBottom: '1px solid #e2e8f0',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: '#f8fafc',
        }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Scale size={20} className="text-blue" />
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: '#0f172a' }}>
                Side-by-Side Candidate Comparison ({candidates.length} candidates)
              </h2>
            </div>
            <div style={{ fontSize: '0.85rem', color: '#64748b', marginTop: '2px' }}>
              Requisition: <strong>{comparison.jobTitle}</strong>
            </div>
          </div>
          <button
            onClick={onClose}
            className="btn-icon"
            style={{ border: 'none', background: 'transparent', cursor: 'pointer', padding: '6px' }}
          >
            <X size={20} color="#64748b" />
          </button>
        </div>

        {/* Modal Body / Comparison Matrix */}
        <div style={{ padding: '24px', overflowX: 'auto', overflowY: 'auto', flex: 1 }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: '700px' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                <th style={{ padding: '12px 16px', background: '#f8fafc', width: '220px', color: '#475569', fontWeight: 600, fontSize: '0.85rem' }}>
                  Criteria
                </th>
                {candidates.map((cand) => (
                  <th key={cand.candidateId} style={{ padding: '12px 16px', background: '#f8fafc', color: '#0f172a', fontWeight: 700, fontSize: '0.95rem' }}>
                    <div>{cand.candidateName || `Candidate #${cand.candidateId}`}</div>
                    <div style={{ fontSize: '0.75rem', fontWeight: 400, color: '#64748b' }}>{cand.candidateEmail}</div>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {/* Final Score */}
              <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '12px 16px', fontWeight: 600, color: '#334155', fontSize: '0.85rem' }}>Final Score</td>
                {candidates.map((c) => (
                  <td key={c.candidateId} style={{ padding: '12px 16px' }}>
                    <span style={{
                      fontSize: '1.3rem',
                      fontWeight: 800,
                      color: c.finalScore >= 75 ? '#16a34a' : c.finalScore >= 50 ? '#d97706' : '#dc2626'
                    }}>
                      {c.finalScore.toFixed(1)}%
                    </span>
                  </td>
                ))}
              </tr>

              {/* Eligibility */}
              <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '12px 16px', fontWeight: 600, color: '#334155', fontSize: '0.85rem' }}>Mandatory Eligibility</td>
                {candidates.map((c) => (
                  <td key={c.candidateId} style={{ padding: '12px 16px' }}>
                    {c.eligible ? (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', background: '#dcfce7', color: '#166534', padding: '3px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 600 }}>
                        <CheckCircle2 size={13} /> ELIGIBLE
                      </span>
                    ) : (
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', background: '#fee2e2', color: '#991b1b', padding: '3px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 600 }}>
                        <XCircle size={13} /> INELIGIBLE
                      </span>
                    )}
                  </td>
                ))}
              </tr>

              {/* Current Workflow Stage */}
              <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '12px 16px', fontWeight: 600, color: '#334155', fontSize: '0.85rem' }}>Workflow Stage</td>
                {candidates.map((c) => (
                  <td key={c.candidateId} style={{ padding: '12px 16px' }}>
                    <span style={{ background: '#f1f5f9', color: '#334155', padding: '2px 8px', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 600 }}>
                      {c.stage}
                    </span>
                  </td>
                ))}
              </tr>

              {/* Semantic Similarity */}
              <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '12px 16px', fontWeight: 600, color: '#334155', fontSize: '0.85rem' }}>Semantic Similarity</td>
                {candidates.map((c) => (
                  <td key={c.candidateId} style={{ padding: '12px 16px', fontSize: '0.9rem', color: '#1e293b' }}>
                    {c.semanticSimilarityScore.toFixed(1)}%
                  </td>
                ))}
              </tr>

              {/* Skill Coverage */}
              <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '12px 16px', fontWeight: 600, color: '#334155', fontSize: '0.85rem' }}>Skill Coverage</td>
                {candidates.map((c) => (
                  <td key={c.candidateId} style={{ padding: '12px 16px', fontSize: '0.9rem', color: '#1e293b' }}>
                    {c.skillCoverageScore.toFixed(1)}%
                  </td>
                ))}
              </tr>

              {/* Experience Relevance */}
              <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '12px 16px', fontWeight: 600, color: '#334155', fontSize: '0.85rem' }}>Experience Relevance</td>
                {candidates.map((c) => (
                  <td key={c.candidateId} style={{ padding: '12px 16px', fontSize: '0.9rem', color: '#1e293b' }}>
                    {c.experienceRelevanceScore.toFixed(1)}%
                  </td>
                ))}
              </tr>

              {/* Matched Skills */}
              <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '12px 16px', fontWeight: 600, color: '#334155', fontSize: '0.85rem' }}>Matched Skills</td>
                {candidates.map((c) => (
                  <td key={c.candidateId} style={{ padding: '12px 16px' }}>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                      {c.matchedSkills.map((s) => (
                        <span key={s} style={{ background: '#e0f2fe', color: '#0369a1', padding: '2px 6px', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 500 }}>
                          {s}
                        </span>
                      ))}
                    </div>
                  </td>
                ))}
              </tr>

              {/* Missing Mandatory Skills */}
              <tr style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '12px 16px', fontWeight: 600, color: '#334155', fontSize: '0.85rem' }}>Missing Mandatory</td>
                {candidates.map((c) => (
                  <td key={c.candidateId} style={{ padding: '12px 16px' }}>
                    {c.missingMandatorySkills.length > 0 ? (
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                        {c.missingMandatorySkills.map((s) => (
                          <span key={s} style={{ background: '#fee2e2', color: '#991b1b', padding: '2px 6px', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 500 }}>
                            {s}
                          </span>
                        ))}
                      </div>
                    ) : (
                      <span style={{ fontSize: '0.8rem', color: '#16a34a', fontWeight: 500 }}>None (Criteria Met)</span>
                    )}
                  </td>
                ))}
              </tr>

              {/* Action row */}
              <tr>
                <td style={{ padding: '12px 16px', fontWeight: 600, color: '#334155', fontSize: '0.85rem' }}>Full Profile</td>
                {candidates.map((c) => (
                  <td key={c.candidateId} style={{ padding: '12px 16px' }}>
                    <Link
                      to={`/candidates/${c.candidateId}`}
                      target="_blank"
                      className="btn-secondary"
                      style={{ fontSize: '0.8rem', padding: '4px 10px', display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                    >
                      <span>View Profile</span>
                      <ExternalLink size={12} />
                    </Link>
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
