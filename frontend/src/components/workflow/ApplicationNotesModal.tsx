import React, { useState, useEffect } from 'react';
import { X, MessageSquare, Plus, Clock, User as UserIcon, Tag, Loader2, AlertCircle } from 'lucide-react';
import { JobApplicationNote } from '../../types';
import { workflowApi } from '../../api/workflow';

interface ApplicationNotesModalProps {
  jobId: number;
  applicationId: number;
  candidateName: string;
  isOpen: boolean;
  onClose: () => void;
}

const DECISION_REASONS = [
  { value: '', label: 'None / General Comment' },
  { value: 'STRONG_TECHNICAL_SKILLS', label: 'Strong Technical Skills' },
  { value: 'EXPERIENCE_ALIGNMENT', label: 'Solid Experience Alignment' },
  { value: 'DOMAIN_KNOWLEDGE', label: 'Relevant Domain Knowledge' },
  { value: 'COMMUNICATION_SKILLS', label: 'Strong Communication Skills' },
  { value: 'INITIAL_SCREEN', label: 'Initial Screening Notes' },
  { value: 'TECH_INTERVIEW_FEEDBACK', label: 'Tech Interview Feedback' },
  { value: 'MISSING_MANDATORY_REQUIREMENTS', label: 'Missing Mandatory Requirements' },
  { value: 'INSUFFICIENT_EXPERIENCE', label: 'Insufficient Relevant Experience' },
  { value: 'SALARY_MISMATCH', label: 'Compensation / Salary Mismatch' },
  { value: 'DECLINED_BY_CANDIDATE', label: 'Candidate Withdrew / Declined' },
];

export const ApplicationNotesModal: React.FC<ApplicationNotesModalProps> = ({
  jobId,
  applicationId,
  candidateName,
  isOpen,
  onClose,
}) => {
  const [notes, setNotes] = useState<JobApplicationNote[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [noteText, setNoteText] = useState('');
  const [decisionReason, setDecisionReason] = useState('');

  useEffect(() => {
    if (isOpen && applicationId) {
      loadNotes();
    }
  }, [isOpen, applicationId]);

  const loadNotes = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await workflowApi.getNotes(jobId, applicationId);
      setNotes(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load application notes');
    } finally {
      setLoading(false);
    }
  };

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!noteText.trim()) return;

    try {
      setSubmitting(true);
      setError(null);
      const newNote = await workflowApi.addNote(jobId, applicationId, {
        noteText: noteText.trim(),
        decisionReason: decisionReason || undefined,
      });
      setNotes((prev) => [newNote, ...prev]);
      setNoteText('');
      setDecisionReason('');
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to record recruiter note');
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-backdrop">
      <div
        className="modal-content"
        style={{
          maxWidth: '680px',
          width: '100%',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          borderRadius: '12px',
          padding: 0,
          overflow: 'hidden',
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
            background: '#f8fafc',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                background: '#e0e7ff',
                color: '#4338ca',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <MessageSquare size={18} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 600, color: '#0f172a' }}>
                Recruiter Notes & Timeline
              </h3>
              <p style={{ margin: 0, fontSize: '0.85rem', color: '#64748b' }}>
                Candidate: <strong style={{ color: '#334155' }}>{candidateName}</strong>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="btn-icon"
            style={{ border: 'none', background: 'transparent', cursor: 'pointer', color: '#64748b' }}
            title="Close"
          >
            <X size={20} />
          </button>
        </div>

        {/* Body / Timeline */}
        <div
          style={{
            padding: '1.25rem 1.5rem',
            overflowY: 'auto',
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
          }}
        >
          {error && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                background: '#fef2f2',
                color: '#991b1b',
                padding: '0.75rem 1rem',
                borderRadius: '6px',
                fontSize: '0.875rem',
                border: '1px solid #fecaca',
              }}
            >
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          {/* New Note Form */}
          <form
            onSubmit={handleAddNote}
            style={{
              background: '#f1f5f9',
              padding: '1rem',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
            }}
          >
            <div style={{ marginBottom: '0.75rem' }}>
              <label
                style={{
                  display: 'block',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                  color: '#475569',
                  marginBottom: '0.25rem',
                }}
              >
                Add Recruiter Observation / Evaluation Note
              </label>
              <textarea
                value={noteText}
                onChange={(e) => setNoteText(e.target.value)}
                placeholder="Document interview feedback, screening comments, or reason for status update..."
                rows={3}
                required
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

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '0.75rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flex: 1, minWidth: '220px' }}>
                <Tag size={15} style={{ color: '#64748b' }} />
                <select
                  value={decisionReason}
                  onChange={(e) => setDecisionReason(e.target.value)}
                  style={{
                    padding: '0.4rem 0.6rem',
                    borderRadius: '6px',
                    border: '1px solid #cbd5e1',
                    fontSize: '0.8rem',
                    background: '#fff',
                    flex: 1,
                  }}
                >
                  {DECISION_REASONS.map((r) => (
                    <option key={r.value} value={r.value}>
                      {r.label}
                    </option>
                  ))}
                </select>
              </div>

              <button
                type="submit"
                disabled={submitting || !noteText.trim()}
                className="btn btn-primary"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.4rem',
                  fontSize: '0.85rem',
                  padding: '0.4rem 0.85rem',
                }}
              >
                {submitting ? <Loader2 size={15} className="spinner" /> : <Plus size={15} />}
                Save Note
              </button>
            </div>
          </form>

          {/* Notes History */}
          <div style={{ marginTop: '0.5rem' }}>
            <h4
              style={{
                fontSize: '0.85rem',
                fontWeight: 600,
                color: '#64748b',
                textTransform: 'uppercase',
                letterSpacing: '0.05em',
                marginBottom: '0.75rem',
              }}
            >
              Timeline History ({notes.length})
            </h4>

            {loading ? (
              <div style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                <Loader2 size={24} className="spinner" style={{ margin: '0 auto 0.5rem' }} />
                <p style={{ margin: 0, fontSize: '0.875rem' }}>Loading notes timeline...</p>
              </div>
            ) : notes.length === 0 ? (
              <div
                style={{
                  textAlign: 'center',
                  padding: '2rem',
                  background: '#f8fafc',
                  borderRadius: '8px',
                  border: '1px dashed #cbd5e1',
                  color: '#64748b',
                }}
              >
                <p style={{ margin: 0, fontSize: '0.875rem' }}>No notes recorded for this candidate yet.</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {notes.map((note) => (
                  <div
                    key={note.id}
                    style={{
                      background: '#fff',
                      border: '1px solid #e2e8f0',
                      borderRadius: '8px',
                      padding: '0.85rem 1rem',
                      boxShadow: '0 1px 2px rgba(0,0,0,0.03)',
                    }}
                  >
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        marginBottom: '0.5rem',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <UserIcon size={14} style={{ color: '#64748b' }} />
                        <span style={{ fontSize: '0.85rem', fontWeight: 600, color: '#1e293b' }}>
                          {note.authorName}
                        </span>
                        {note.stageAtCreation && (
                          <span
                            style={{
                              fontSize: '0.7rem',
                              fontWeight: 600,
                              background: '#e0e7ff',
                              color: '#3730a3',
                              padding: '0.15rem 0.45rem',
                              borderRadius: '4px',
                            }}
                          >
                            {note.stageAtCreation}
                          </span>
                        )}
                        {note.decisionReason && (
                          <span
                            style={{
                              fontSize: '0.7rem',
                              fontWeight: 500,
                              background: '#f1f5f9',
                              color: '#475569',
                              padding: '0.15rem 0.45rem',
                              borderRadius: '4px',
                            }}
                          >
                            {note.decisionReason}
                          </span>
                        )}
                      </div>
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.35rem',
                          fontSize: '0.75rem',
                          color: '#94a3b8',
                        }}
                      >
                        <Clock size={12} />
                        <span>{new Date(note.createdAt).toLocaleString()}</span>
                      </div>
                    </div>
                    <p
                      style={{
                        margin: 0,
                        fontSize: '0.875rem',
                        color: '#334155',
                        whiteSpace: 'pre-wrap',
                        lineHeight: 1.5,
                      }}
                    >
                      {note.noteText}
                    </p>
                  </div>
                ))}
              </div>
            )}
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
          }}
        >
          <button type="button" onClick={onClose} className="btn btn-secondary">
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
