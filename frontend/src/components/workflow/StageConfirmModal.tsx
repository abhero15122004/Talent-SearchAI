import React, { useState } from 'react';
import { AlertTriangle, CheckCircle, X, Loader2 } from 'lucide-react';
import { ApplicationStage } from '../../types';

interface StageConfirmModalProps {
  isOpen: boolean;
  candidateName: string;
  currentStage: ApplicationStage;
  targetStage: ApplicationStage;
  onConfirm: (decisionReason?: string, note?: string) => Promise<void>;
  onClose: () => void;
}

const REJECT_REASONS = [
  { value: 'NOT_ENOUGH_EXPERIENCE', label: 'Insufficient Relevant Experience' },
  { value: 'MISSING_MANDATORY_SKILLS', label: 'Missing Mandatory Technical Skills' },
  { value: 'FAILED_TECHNICAL_INTERVIEW', label: 'Failed Technical Interview / Assessment' },
  { value: 'CULTURE_OR_COMMUNICATION', label: 'Communication or Team Fit' },
  { value: 'SALARY_MISMATCH', label: 'Compensation / Salary Expectations Mismatch' },
  { value: 'POSITION_FILLED', label: 'Position Filled / Requisition Closed' },
  { value: 'OTHER', label: 'Other Reason' },
];

const HIRE_REASONS = [
  { value: 'TOP_PERFORMER', label: 'Top Candidate Across All Criteria' },
  { value: 'EXCEPTIONAL_TECH_SKILLS', label: 'Exceptional Technical Assessment & Portfolio' },
  { value: 'EXPERIENCED_HIRE', label: 'Strong Domain & Leadership Experience' },
  { value: 'IMMEDIATE_AVAILABILITY', label: 'Strong Fit & Immediate Availability' },
];

export const StageConfirmModal: React.FC<StageConfirmModalProps> = ({
  isOpen,
  candidateName,
  currentStage,
  targetStage,
  onConfirm,
  onClose,
}) => {
  const [decisionReason, setDecisionReason] = useState<string>('');
  const [note, setNote] = useState<string>('');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const isReject = targetStage === 'REJECTED';
  const isHire = targetStage === 'HIRED';

  const reasonList = isReject ? REJECT_REASONS : isHire ? HIRE_REASONS : [];

  const handleConfirm = async () => {
    try {
      setSubmitting(true);
      setError(null);
      await onConfirm(decisionReason || undefined, note.trim() || undefined);
      onClose();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to update candidate workflow stage');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div
        className="modal-content"
        style={{
          maxWidth: '520px',
          width: '100%',
          borderRadius: '12px',
          overflow: 'hidden',
          padding: 0,
        }}
      >
        {/* Header */}
        <div
          style={{
            padding: '1.25rem 1.5rem',
            borderBottom: '1px solid #e2e8f0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: isReject ? '#fef2f2' : isHire ? '#f0fdf4' : '#f8fafc',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            {isReject ? (
              <div
                style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '8px',
                  background: '#fee2e2',
                  color: '#dc2626',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <AlertTriangle size={20} />
              </div>
            ) : isHire ? (
              <div
                style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '8px',
                  background: '#dcfce7',
                  color: '#16a34a',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <CheckCircle size={20} />
              </div>
            ) : (
              <div
                style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '8px',
                  background: '#e0e7ff',
                  color: '#4f46e5',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <CheckCircle size={20} />
              </div>
            )}
            <div>
              <h3
                style={{
                  margin: 0,
                  fontSize: '1.1rem',
                  fontWeight: 600,
                  color: isReject ? '#991b1b' : isHire ? '#166534' : '#0f172a',
                }}
              >
                Confirm Stage Transition
              </h3>
              <p style={{ margin: 0, fontSize: '0.825rem', color: '#64748b' }}>
                Moving to <strong>{targetStage}</strong>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{ border: 'none', background: 'transparent', cursor: 'pointer', color: '#64748b' }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Content */}
        <div style={{ padding: '1.25rem 1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {error && (
            <div
              style={{
                background: '#fef2f2',
                color: '#991b1b',
                padding: '0.75rem',
                borderRadius: '6px',
                fontSize: '0.85rem',
                border: '1px solid #fecaca',
              }}
            >
              {error}
            </div>
          )}

          <p style={{ margin: 0, fontSize: '0.9rem', color: '#334155', lineHeight: 1.5 }}>
            Are you sure you want to transition candidate{' '}
            <strong style={{ color: '#0f172a' }}>{candidateName}</strong> from{' '}
            <span
              style={{
                background: '#f1f5f9',
                padding: '0.15rem 0.4rem',
                borderRadius: '4px',
                fontSize: '0.8rem',
                fontWeight: 600,
              }}
            >
              {currentStage}
            </span>{' '}
            to{' '}
            <span
              style={{
                background: isReject ? '#fee2e2' : isHire ? '#dcfce7' : '#e0e7ff',
                color: isReject ? '#991b1b' : isHire ? '#166534' : '#3730a3',
                padding: '0.15rem 0.4rem',
                borderRadius: '4px',
                fontSize: '0.8rem',
                fontWeight: 600,
              }}
            >
              {targetStage}
            </span>
            ?
          </p>

          {reasonList.length > 0 && (
            <div>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                  color: '#475569',
                  marginBottom: '0.25rem',
                }}
              >
                Decision Reason (Optional)
              </label>
              <select
                value={decisionReason}
                onChange={(e) => setDecisionReason(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.5rem 0.75rem',
                  borderRadius: '6px',
                  border: '1px solid #cbd5e1',
                  fontSize: '0.875rem',
                  background: '#fff',
                }}
              >
                <option value="">-- Select a reason --</option>
                {reasonList.map((r) => (
                  <option key={r.value} value={r.value}>
                    {r.label}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div>
            <label
              style={{
                display: 'block',
                fontSize: '0.8rem',
                fontWeight: 600,
                color: '#475569',
                marginBottom: '0.25rem',
              }}
            >
              Recruiter Note / Feedback (Optional)
            </label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Record details about the interview, debrief notes, or reason for this decision..."
              rows={3}
              style={{
                width: '100%',
                padding: '0.5rem 0.75rem',
                borderRadius: '6px',
                border: '1px solid #cbd5e1',
                fontSize: '0.875rem',
                fontFamily: 'inherit',
                resize: 'vertical',
              }}
            />
          </div>
        </div>

        {/* Footer */}
        <div
          style={{
            padding: '1rem 1.5rem',
            borderTop: '1px solid #e2e8f0',
            background: '#f8fafc',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '0.75rem',
          }}
        >
          <button type="button" onClick={onClose} disabled={submitting} className="btn btn-secondary">
            Cancel
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={submitting}
            className={`btn ${isReject ? 'btn-danger' : 'btn-primary'}`}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              background: isReject ? '#dc2626' : isHire ? '#16a34a' : undefined,
              borderColor: isReject ? '#dc2626' : isHire ? '#16a34a' : undefined,
            }}
          >
            {submitting && <Loader2 size={16} className="spinner" />}
            Confirm Transition
          </button>
        </div>
      </div>
    </div>
  );
};
