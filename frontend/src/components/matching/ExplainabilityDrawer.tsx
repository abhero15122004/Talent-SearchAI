import React, { useState, useEffect } from 'react';
import { CandidateMatch, CandidateDetail } from '../../types';
import { candidatesApi } from '../../api/candidates';
import { ProfessionalResumeView } from '../candidates/ProfessionalResumeView';
import {
  X,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Sparkles,
  FileText,
  Cpu,
} from 'lucide-react';

interface ExplainabilityDrawerProps {
  match: CandidateMatch | null;
  jobTitle: string;
  algorithmVersion?: string;
  onClose: () => void;
  initialTab?: 'explain' | 'resume';
}

const cleanText = (text?: string | null): string => {
  if (!text) return '';
  return text
    .replace(/Â/g, '')
    .replace(/\u00a0/g, ' ')
    .replace(/â€™/g, "'")
    .replace(/â€œ/g, '"')
    .replace(/â€/g, '"')
    .replace(/â€"/g, '—')
    .replace(/\s+/g, ' ')
    .trim();
};

export const ExplainabilityDrawer: React.FC<ExplainabilityDrawerProps> = ({
  match,
  jobTitle,
  algorithmVersion = '1.0.0-hybrid',
  onClose,
  initialTab = 'explain',
}) => {
  const [activeTab, setActiveTab] = useState<'explain' | 'resume'>(initialTab);
  const [candidateDetail, setCandidateDetail] = useState<CandidateDetail | null>(null);
  const [loadingCandidate, setLoadingCandidate] = useState<boolean>(false);
  const [candidateError, setCandidateError] = useState<string | null>(null);

  useEffect(() => {
    setActiveTab(initialTab);
  }, [initialTab, match]);

  useEffect(() => {
    if (!match?.candidateId) {
      setCandidateDetail(null);
      return;
    }

    let isMounted = true;
    const fetchCandidate = async () => {
      try {
        setLoadingCandidate(true);
        setCandidateError(null);
        const res = await candidatesApi.getCandidateById(match.candidateId);
        if (isMounted) {
          setCandidateDetail(res.data);
        }
      } catch (err: any) {
        if (isMounted) {
          setCandidateError(err.response?.data?.message || 'Failed to load candidate profile details.');
        }
      } finally {
        if (isMounted) {
          setLoadingCandidate(false);
        }
      }
    };

    fetchCandidate();

    return () => {
      isMounted = false;
    };
  }, [match?.candidateId]);

  if (!match) return null;

  const getScoreColor = (score: number) => {
    if (score >= 75) return '#16a34a';
    if (score >= 50) return '#d97706';
    return '#dc2626';
  };

  // Generate structured executive narrative and bullet points
  const generateFitReport = () => {
    const isEligible = match.eligible;
    const score = match.finalScore;
    const candName = match.candidateName || `Candidate #${match.candidateId}`;
    const matchedSkills = match.matchedSkills || [];
    const missingMandatory = match.missingMandatorySkills || [];
    const matchedPreferred = match.matchedPreferredSkills || [];

    // 1. Executive Summary Paragraph
    let executiveSummary = '';
    if (isEligible && score >= 75) {
      executiveSummary = `${candName} demonstrates high qualification for the ${jobTitle} role with an overall match score of ${score.toFixed(1)}%. The profile satisfies 100% of mandatory requirements, demonstrating verified technical competency in ${matchedSkills.slice(0, 4).join(', ')}${matchedSkills.length > 4 ? ` along with ${matchedSkills.length - 4} additional skills` : ''}. Background and career achievements align directly with target requisition expectations.`;
    } else if (isEligible && score >= 50) {
      executiveSummary = `${candName} qualifies as a prospective candidate for ${jobTitle} with a moderate alignment score of ${score.toFixed(1)}%. Core eligibility criteria are fulfilled, with demonstrable skills in ${matchedSkills.slice(0, 3).join(', ')}. Further review of specialized domain depth and project complexity is recommended.`;
    } else if (!isEligible) {
      executiveSummary = `${candName} scored ${score.toFixed(1)}%, but did not satisfy all mandatory eligibility criteria for ${jobTitle}. Specifically, verified documentation was absent for: ${missingMandatory.join(', ')}. Gating criteria have been applied to flag this candidate for recruiter discretion.`;
    } else {
      executiveSummary = `${candName} has been evaluated against ${jobTitle} requirements, obtaining a normalized fit score of ${score.toFixed(1)}% across skill coverage, experience tenure, and semantic relevance.`;
    }

    // 2. Strengths & Verified Evidence (Bullet Points)
    const strengthBullets: { title: string; text: string }[] = [];
    if (isEligible) {
      strengthBullets.push({
        title: 'Mandatory Requirements Satisfied',
        text: 'Candidate verified all mandatory skill gates established by the hiring team.',
      });
    }

    if (matchedSkills.length > 0) {
      strengthBullets.push({
        title: 'Verified Technical Competencies',
        text: `Extracted and normalized skills: ${matchedSkills.join(', ')}.`,
      });
    }

    if (matchedPreferred.length > 0) {
      strengthBullets.push({
        title: 'Preferred Skill Additions',
        text: `Verified bonus preferred capabilities: ${matchedPreferred.join(', ')}.`,
      });
    }

    if (match.experienceRelevanceScore >= 70) {
      strengthBullets.push({
        title: 'Experience Tenure Fit',
        text: `Documented career history meets or exceeds role tenure criteria (${match.experienceRelevanceScore.toFixed(0)}% tenure score).`,
      });
    }

    if (match.semanticSimilarityScore >= 60) {
      strengthBullets.push({
        title: 'Contextual Semantic Relevance',
        text: `Language and role descriptions across resume history exhibit strong contextual alignment (${match.semanticSimilarityScore.toFixed(0)}% semantic score) with the position specifications.`,
      });
    }

    // Evidence Snippets as Clean Bullets
    if (match.evidenceSnippets && Object.keys(match.evidenceSnippets).length > 0) {
      Object.entries(match.evidenceSnippets).forEach(([skill, rawSnippet]) => {
        const cleanSnippet = cleanText(rawSnippet);
        if (cleanSnippet) {
          strengthBullets.push({
            title: `Evidence for ${skill}`,
            text: `"${cleanSnippet}"`,
          });
        }
      });
    }

    // 3. Observations & Consideration Gaps (Bullet Points)
    const gapBullets: { title: string; text: string; isSevere: boolean }[] = [];
    if (missingMandatory.length > 0) {
      gapBullets.push({
        title: 'Missing Mandatory Requirement',
        text: `Candidate profile lacks documented proof for: ${missingMandatory.join(', ')}.`,
        isSevere: true,
      });
    }

    if (match.mandatoryStatus === 'NOT_VERIFIABLE') {
      gapBullets.push({
        title: 'Unverifiable Data',
        text: 'Text analysis was unable to reliably extract certain structured criteria. Treated with neutral uncertainty.',
        isSevere: false,
      });
    }

    if (match.warnings && match.warnings.length > 0) {
      match.warnings.forEach((warn) => {
        const cleanWarn = cleanText(warn);
        if (cleanWarn) {
          gapBullets.push({
            title: 'Extraction / Uncertainty Notice',
            text: cleanWarn,
            isSevere: false,
          });
        }
      });
    }

    return { executiveSummary, strengthBullets, gapBullets };
  };

  const { executiveSummary, strengthBullets, gapBullets } = generateFitReport();

  return (
    <div
      className="drawer-overlay"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        background: 'rgba(15, 23, 42, 0.55)',
        backdropFilter: 'blur(2px)',
        display: 'flex',
        justifyContent: 'flex-end',
        zIndex: 1000,
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="drawer-content"
        style={{
          width: '680px',
          maxWidth: '92vw',
          height: '100%',
          background: '#ffffff',
          boxShadow: '-6px 0 28px rgba(0, 0, 0, 0.16)',
          display: 'flex',
          flexDirection: 'column',
          overflowY: 'auto',
        }}
      >
        {/* Drawer Header */}
        <div
          style={{
            padding: '18px 24px',
            borderBottom: '1px solid #e2e8f0',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-start',
            background: '#f8fafc',
            position: 'sticky',
            top: 0,
            zIndex: 10,
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  background: '#e0e7ff',
                  color: '#4338ca',
                  padding: '2px 8px',
                  borderRadius: '4px',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  textTransform: 'uppercase',
                }}
              >
                Rank #{match.rankPosition}
              </span>
              <span style={{ fontSize: '0.85rem', color: '#64748b' }}>
                Requisition: <strong>{jobTitle}</strong>
              </span>
            </div>
            <h2 style={{ fontSize: '1.35rem', fontWeight: 700, color: '#0f172a', margin: '6px 0 2px 0' }}>
              {match.candidateName || `Candidate #${match.candidateId}`}
            </h2>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '0.82rem', color: '#64748b' }}>
              <span>{match.candidateEmail}</span>
              <span style={{ color: '#cbd5e1' }}>•</span>
              <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>v{algorithmVersion}</span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="btn-icon"
            style={{
              border: 'none',
              background: '#e2e8f0',
              borderRadius: '50%',
              width: '32px',
              height: '32px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              transition: 'background 0.15s ease',
            }}
            title="Close Drawer"
          >
            <X size={18} color="#475569" />
          </button>
        </div>

        {/* Tab Navigation */}
        <div
          style={{
            display: 'flex',
            borderBottom: '1px solid #e2e8f0',
            background: '#ffffff',
            padding: '0 24px',
            position: 'sticky',
            top: '85px',
            zIndex: 9,
          }}
        >
          <button
            onClick={() => setActiveTab('explain')}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '12px 16px',
              border: 'none',
              background: 'transparent',
              fontSize: '0.9rem',
              fontWeight: activeTab === 'explain' ? 700 : 500,
              color: activeTab === 'explain' ? '#4f46e5' : '#64748b',
              borderBottom: activeTab === 'explain' ? '3px solid #4f46e5' : '3px solid transparent',
              cursor: 'pointer',
              transition: 'all 0.15s ease',
            }}
          >
            <Sparkles size={16} />
            <span>AI Match Assessment</span>
            <span
              style={{
                marginLeft: '4px',
                padding: '2px 6px',
                borderRadius: '10px',
                fontSize: '0.75rem',
                background: activeTab === 'explain' ? '#e0e7ff' : '#f1f5f9',
                color: activeTab === 'explain' ? '#4338ca' : '#64748b',
                fontWeight: 700,
              }}
            >
              {match.finalScore.toFixed(0)}%
            </span>
          </button>

          <button
            onClick={() => setActiveTab('resume')}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '12px 16px',
              border: 'none',
              background: 'transparent',
              fontSize: '0.9rem',
              fontWeight: activeTab === 'resume' ? 700 : 500,
              color: activeTab === 'resume' ? '#4f46e5' : '#64748b',
              borderBottom: activeTab === 'resume' ? '3px solid #4f46e5' : '3px solid transparent',
              cursor: 'pointer',
              transition: 'all 0.15s ease',
            }}
          >
            <FileText size={16} />
            <span>Candidate Resume &amp; Profile</span>
          </button>
        </div>

        {/* Tab 1: AI Match Assessment & Explainability */}
        {activeTab === 'explain' && (
          <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '22px' }}>
            {/* Overall Score Banner */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '16px 20px',
                background: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: '10px',
              }}
            >
              <div>
                <span style={{ fontSize: '0.82rem', color: '#64748b', fontWeight: 600, textTransform: 'uppercase' }}>
                  Recommendation Score
                </span>
                <div style={{ fontSize: '2.2rem', fontWeight: 800, color: getScoreColor(match.finalScore) }}>
                  {match.finalScore.toFixed(1)}%
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <span
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '4px',
                    padding: '6px 12px',
                    borderRadius: '6px',
                    fontSize: '0.85rem',
                    fontWeight: 700,
                    background: match.eligible ? '#dcfce7' : '#fee2e2',
                    color: match.eligible ? '#166534' : '#991b1b',
                  }}
                >
                  {match.eligible ? <CheckCircle2 size={15} /> : <XCircle size={15} />}
                  {match.eligible ? 'ELIGIBLE' : 'INELIGIBLE'}
                </span>
                <div style={{ fontSize: '0.78rem', color: '#64748b', marginTop: '6px' }}>
                  Rank #{match.rankPosition} of Candidate Pool
                </div>
              </div>
            </div>

            {/* Section 1: Executive Assessment Paragraph */}
            <div
              style={{
                background: '#f0f9ff',
                border: '1px solid #bae6fd',
                borderRadius: '8px',
                padding: '16px 18px',
              }}
            >
              <h3
                style={{
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  color: '#0369a1',
                  margin: '0 0 8px 0',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                }}
              >
                <Sparkles size={16} />
                Executive Fit Summary
              </h3>
              <p style={{ margin: 0, fontSize: '0.88rem', color: '#0c4a6e', lineHeight: 1.6 }}>
                {executiveSummary}
              </p>
            </div>

            {/* Section 2: Key Strengths & Verified Evidence (Bullet Points) */}
            <div>
              <h3
                style={{
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  color: '#1e293b',
                  marginBottom: '10px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                }}
              >
                <CheckCircle2 size={16} color="#16a34a" />
                Key Alignment Strengths &amp; Evidence
              </h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {strengthBullets.map((item, idx) => (
                  <div
                    key={idx}
                    style={{
                      background: '#ffffff',
                      border: '1px solid #e2e8f0',
                      borderLeft: '4px solid #16a34a',
                      borderRadius: '4px 8px 8px 4px',
                      padding: '10px 14px',
                      fontSize: '0.85rem',
                    }}
                  >
                    <div style={{ fontWeight: 700, color: '#1e293b', marginBottom: '2px' }}>
                      • {item.title}
                    </div>
                    <div style={{ color: '#475569', lineHeight: 1.45 }}>{item.text}</div>
                  </div>
                ))}
              </div>
            </div>

            {/* Section 3: Areas of Attention & Requirements Gap (Bullet Points) */}
            {gapBullets.length > 0 && (
              <div>
                <h3
                  style={{
                    fontSize: '0.95rem',
                    fontWeight: 700,
                    color: '#92400e',
                    marginBottom: '10px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                  }}
                >
                  <AlertTriangle size={16} color="#d97706" />
                  Gaps &amp; Considerations for Recruiter
                </h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {gapBullets.map((item, idx) => (
                    <div
                      key={idx}
                      style={{
                        background: item.isSevere ? '#fef2f2' : '#fffbeb',
                        border: item.isSevere ? '1px solid #fecaca' : '1px solid #fde68a',
                        borderLeft: item.isSevere ? '4px solid #dc2626' : '4px solid #d97706',
                        borderRadius: '4px 8px 8px 4px',
                        padding: '10px 14px',
                        fontSize: '0.85rem',
                      }}
                    >
                      <div
                        style={{
                          fontWeight: 700,
                          color: item.isSevere ? '#991b1b' : '#92400e',
                          marginBottom: '2px',
                        }}
                      >
                        • {item.title}
                      </div>
                      <div style={{ color: item.isSevere ? '#7f1d1d' : '#78350f', lineHeight: 1.45 }}>
                        {item.text}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Section 4: Multi-Factor Component Scores */}
            <div>
              <h3
                style={{
                  fontSize: '0.95rem',
                  fontWeight: 700,
                  color: '#1e293b',
                  marginBottom: '12px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                }}
              >
                <Cpu size={16} color="#4f46e5" />
                Score Factor Breakdown
              </h3>
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '12px',
                  background: '#f8fafc',
                  padding: '16px',
                  borderRadius: '8px',
                  border: '1px solid #e2e8f0',
                }}
              >
                {/* Skill Coverage */}
                <div>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      fontSize: '0.85rem',
                      marginBottom: '4px',
                    }}
                  >
                    <span style={{ color: '#475569', fontWeight: 600 }}>Normalized Skill Coverage</span>
                    <strong>{match.skillCoverageScore.toFixed(1)}%</strong>
                  </div>
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
                        width: `${Math.min(100, match.skillCoverageScore)}%`,
                        height: '100%',
                        background: '#10b981',
                      }}
                    />
                  </div>
                </div>

                {/* Experience Relevance */}
                <div>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      fontSize: '0.85rem',
                      marginBottom: '4px',
                    }}
                  >
                    <span style={{ color: '#475569', fontWeight: 600 }}>Experience &amp; Tenure Relevance</span>
                    <strong>{match.experienceRelevanceScore.toFixed(1)}%</strong>
                  </div>
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
                        width: `${Math.min(100, match.experienceRelevanceScore)}%`,
                        height: '100%',
                        background: '#8b5cf6',
                      }}
                    />
                  </div>
                </div>

                {/* Project Relevance */}
                <div>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      fontSize: '0.85rem',
                      marginBottom: '4px',
                    }}
                  >
                    <span style={{ color: '#475569', fontWeight: 600 }}>Project Portfolio Relevance</span>
                    <strong>{match.projectRelevanceScore.toFixed(1)}%</strong>
                  </div>
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
                        width: `${Math.min(100, match.projectRelevanceScore)}%`,
                        height: '100%',
                        background: '#f59e0b',
                      }}
                    />
                  </div>
                </div>

                {/* Semantic Similarity */}
                <div>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      fontSize: '0.85rem',
                      marginBottom: '4px',
                    }}
                  >
                    <span style={{ color: '#475569', fontWeight: 600 }}>Semantic Domain Alignment</span>
                    <strong>{match.semanticSimilarityScore.toFixed(1)}%</strong>
                  </div>
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
                        width: `${Math.min(100, match.semanticSimilarityScore)}%`,
                        height: '100%',
                        background: '#3b82f6',
                      }}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Full Resume & Candidate Profile */}
        {activeTab === 'resume' && (
          <div style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {loadingCandidate && (
              <div style={{ textAlign: 'center', padding: '40px 0', color: '#64748b' }}>
                <div style={{ fontSize: '0.9rem', fontWeight: 600 }}>Loading full candidate resume data...</div>
              </div>
            )}

            {candidateError && (
              <div
                style={{
                  padding: '14px',
                  background: '#fef2f2',
                  border: '1px solid #fecaca',
                  borderRadius: '6px',
                  color: '#991b1b',
                  fontSize: '0.85rem',
                }}
              >
                {candidateError}
              </div>
            )}

            {!loadingCandidate && candidateDetail && (
              <ProfessionalResumeView candidate={candidateDetail} />
            )}
          </div>
        )}
      </div>
    </div>
  );
};
