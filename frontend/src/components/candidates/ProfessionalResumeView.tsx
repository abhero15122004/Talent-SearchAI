import React, { useState } from 'react';
import { CandidateDetail } from '../../types';
import {
  Briefcase,
  GraduationCap,
  Award,
  Globe,
  Linkedin,
  Github,
  ChevronDown,
  ChevronUp,
  Mail,
  Phone,
  FileText,
  Building,
  Calendar,
  Sparkles,
  ExternalLink,
  Users,
} from 'lucide-react';
import {
  parseWorkExperiences,
  parseEducations,
  extractAdditionalResumeSections,
  sanitizeText,
} from '../../utils/resumeParser';

interface ProfessionalResumeViewProps {
  candidate: CandidateDetail;
  hideHeaderCard?: boolean;
}

export const ProfessionalResumeView: React.FC<ProfessionalResumeViewProps> = ({
  candidate,
  hideHeaderCard = false,
}) => {
  const [showRawText, setShowRawText] = useState(false);

  const cleanSummary = sanitizeText(candidate.summary);
  const experiences = parseWorkExperiences(candidate.experienceHistory || candidate.rawText);
  const educations = parseEducations(candidate.educationHistory || candidate.rawText);

  // Check full text (including rawText and educationHistory) for additional sections
  const combinedText = `${candidate.educationHistory || ''}\n${candidate.projectHistory || ''}\n${candidate.rawText || ''}`;
  const additional = extractAdditionalResumeSections(combinedText);

  const skillsList = candidate.primarySkills
    ? candidate.primarySkills
        .split(',')
        .map((s) => sanitizeText(s))
        .filter(Boolean)
    : [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
      {/* 1. Header Card (Optional, when not rendered by parent) */}
      {!hideHeaderCard && (
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '24px 28px',
            borderRadius: '12px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '16px',
          }}
        >
          <div style={{ display: 'flex', gap: '18px', alignItems: 'center' }}>
            <div
              style={{
                width: '60px',
                height: '60px',
                borderRadius: '50%',
                backgroundColor: '#e0e7ff',
                color: '#4338ca',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '20px',
                fontWeight: 700,
              }}
            >
              {candidate.fullName
                ? candidate.fullName
                    .split(' ')
                    .map((n) => n[0])
                    .join('')
                    .substring(0, 2)
                    .toUpperCase()
                : '??'}
            </div>
            <div>
              <h1 style={{ fontSize: '1.4rem', fontWeight: 700, color: '#0f172a', margin: '0 0 6px 0' }}>
                {candidate.fullName || 'Candidate Profile'}
              </h1>
              <div style={{ display: 'flex', gap: '16px', color: '#64748b', fontSize: '0.85rem', flexWrap: 'wrap' }}>
                {candidate.email && (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
                    <Mail size={14} /> {candidate.email}
                  </span>
                )}
                {candidate.phone && (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
                    <Phone size={14} /> {candidate.phone}
                  </span>
                )}
                {candidate.sourceFilename && (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px' }}>
                    <FileText size={14} /> {candidate.sourceFilename}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Social / Portfolio Links Header Pills */}
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {additional.links.website && (
              <a
                href={additional.links.website.startsWith('http') ? additional.links.website : `https://${additional.links.website}`}
                target="_blank"
                rel="noreferrer"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '5px',
                  padding: '6px 12px',
                  borderRadius: '6px',
                  border: '1px solid #cbd5e1',
                  background: '#f8fafc',
                  color: '#0369a1',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                  textDecoration: 'none',
                }}
              >
                <Globe size={14} />
                <span>Website</span>
                <ExternalLink size={12} />
              </a>
            )}
            {additional.links.linkedin && (
              <a
                href={additional.links.linkedin.startsWith('http') ? additional.links.linkedin : `https://${additional.links.linkedin}`}
                target="_blank"
                rel="noreferrer"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '5px',
                  padding: '6px 12px',
                  borderRadius: '6px',
                  border: '1px solid #bfdbfe',
                  background: '#eff6ff',
                  color: '#1d4ed8',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                  textDecoration: 'none',
                }}
              >
                <Linkedin size={14} />
                <span>LinkedIn</span>
                <ExternalLink size={12} />
              </a>
            )}
            {additional.links.github && (
              <a
                href={additional.links.github.startsWith('http') ? additional.links.github : `https://${additional.links.github}`}
                target="_blank"
                rel="noreferrer"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '5px',
                  padding: '6px 12px',
                  borderRadius: '6px',
                  border: '1px solid #e2e8f0',
                  background: '#0f172a',
                  color: '#ffffff',
                  fontSize: '0.8rem',
                  fontWeight: 600,
                  textDecoration: 'none',
                }}
              >
                <Github size={14} />
                <span>GitHub</span>
                <ExternalLink size={12} />
              </a>
            )}
          </div>
        </div>
      )}

      {/* 2. Professional Summary */}
      {cleanSummary && (
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '20px 24px',
            borderRadius: '10px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px rgba(0, 0, 0, 0.03)',
          }}
        >
          <h2
            style={{
              fontSize: '1rem',
              fontWeight: 700,
              color: '#1e293b',
              margin: '0 0 10px 0',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <Sparkles size={17} color="#4f46e5" />
            Professional Summary
          </h2>
          <p
            style={{
              color: '#334155',
              lineHeight: 1.65,
              margin: 0,
              fontSize: '0.9rem',
              background: '#f8fafc',
              padding: '14px 18px',
              borderRadius: '8px',
              border: '1px solid #f1f5f9',
            }}
          >
            {cleanSummary}
          </p>
        </div>
      )}

      {/* 3. Core Technical Competencies & Skills */}
      {skillsList.length > 0 && (
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '20px 24px',
            borderRadius: '10px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px rgba(0, 0, 0, 0.03)',
          }}
        >
          <h2
            style={{
              fontSize: '1rem',
              fontWeight: 700,
              color: '#1e293b',
              margin: '0 0 12px 0',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <Sparkles size={17} color="#10b981" />
            Verified Skills &amp; Core Competencies ({skillsList.length})
          </h2>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {skillsList.map((skill) => (
              <span
                key={skill}
                style={{
                  padding: '5px 12px',
                  borderRadius: '6px',
                  backgroundColor: '#e0f2fe',
                  color: '#0369a1',
                  fontSize: '0.82rem',
                  fontWeight: 600,
                  border: '1px solid #bae6fd',
                }}
              >
                {skill}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* 4. Structured Work Experience Cards */}
      <div
        style={{
          backgroundColor: '#ffffff',
          padding: '22px 24px',
          borderRadius: '10px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.03)',
        }}
      >
        <h2
          style={{
            fontSize: '1.05rem',
            fontWeight: 700,
            color: '#1e293b',
            margin: '0 0 16px 0',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <Briefcase size={18} color="#8b5cf6" />
          Work Experience
        </h2>

        {experiences.length === 0 ? (
          <p style={{ color: '#94a3b8', fontSize: '0.88rem', margin: 0, fontStyle: 'italic' }}>
            No verified work experience section identified.
          </p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '18px' }}>
            {experiences.map((exp, idx) => (
              <div
                key={idx}
                style={{
                  border: '1px solid #e2e8f0',
                  borderRadius: '8px',
                  padding: '16px 20px',
                  backgroundColor: '#f8fafc',
                  borderLeft: '4px solid #8b5cf6',
                }}
              >
                {/* Job Card Header */}
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                    flexWrap: 'wrap',
                    gap: '10px',
                    marginBottom: '10px',
                  }}
                >
                  <div>
                    <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#0f172a', margin: '0 0 4px 0' }}>
                      {exp.roleTitle}
                    </h3>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.83rem', color: '#64748b' }}>
                      {exp.companyName && (
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}>
                          <Building size={13} /> {exp.companyName}
                        </span>
                      )}
                      {exp.location && <span>• {exp.location}</span>}
                    </div>
                  </div>

                  {exp.dateRange && (
                    <span
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '5px',
                        background: '#f1f5f9',
                        color: '#475569',
                        padding: '4px 10px',
                        borderRadius: '6px',
                        fontSize: '0.8rem',
                        fontWeight: 600,
                        border: '1px solid #e2e8f0',
                      }}
                    >
                      <Calendar size={13} />
                      {exp.dateRange}
                    </span>
                  )}
                </div>

                {/* Bullet Points */}
                <ul
                  style={{
                    margin: 0,
                    paddingLeft: '20px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '6px',
                    color: '#334155',
                    fontSize: '0.86rem',
                    lineHeight: 1.5,
                  }}
                >
                  {exp.bullets.map((bullet, bIdx) => (
                    <li key={bIdx} style={{ listStyleType: 'disc' }}>
                      {bullet}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 5. Structured Education & Qualifications */}
      <div
        style={{
          backgroundColor: '#ffffff',
          padding: '22px 24px',
          borderRadius: '10px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.03)',
        }}
      >
        <h2
          style={{
            fontSize: '1.05rem',
            fontWeight: 700,
            color: '#1e293b',
            margin: '0 0 16px 0',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <GraduationCap size={18} color="#06b6d4" />
          Education &amp; Academic Qualifications
        </h2>

        {educations.length === 0 ? (
          <p style={{ color: '#94a3b8', fontSize: '0.88rem', margin: 0, fontStyle: 'italic' }}>
            No formal education section identified.
          </p>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px' }}>
            {educations.map((edu, idx) => (
              <div
                key={idx}
                style={{
                  border: '1px solid #e2e8f0',
                  borderRadius: '8px',
                  padding: '14px 18px',
                  backgroundColor: '#f8fafc',
                  borderLeft: '4px solid #06b6d4',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                }}
              >
                <div>
                  <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#0f172a', margin: '0 0 4px 0' }}>
                    {edu.degree}
                  </h3>
                  {edu.institution && (
                    <div style={{ fontSize: '0.83rem', color: '#475569', marginBottom: '8px', fontWeight: 500 }}>
                      {edu.institution} {edu.location ? `• ${edu.location}` : ''}
                    </div>
                  )}
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '6px' }}>
                  {edu.year && (
                    <span
                      style={{
                        background: '#e0f2fe',
                        color: '#0369a1',
                        padding: '2px 8px',
                        borderRadius: '4px',
                        fontSize: '0.76rem',
                        fontWeight: 700,
                      }}
                    >
                      {edu.year}
                    </span>
                  )}
                  {edu.gpa && (
                    <span
                      style={{
                        background: '#dcfce7',
                        color: '#166534',
                        padding: '2px 8px',
                        borderRadius: '4px',
                        fontSize: '0.76rem',
                        fontWeight: 700,
                      }}
                    >
                      GPA: {edu.gpa}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* 6. Honours, Awards & Distinctions (If extracted) */}
      {additional.honors.length > 0 && (
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '20px 24px',
            borderRadius: '10px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px rgba(0, 0, 0, 0.03)',
          }}
        >
          <h2
            style={{
              fontSize: '1rem',
              fontWeight: 700,
              color: '#1e293b',
              margin: '0 0 12px 0',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <Award size={18} color="#f59e0b" />
            Honours &amp; Distinctions
          </h2>
          <ul
            style={{
              margin: 0,
              paddingLeft: '20px',
              display: 'flex',
              flexDirection: 'column',
              gap: '6px',
              color: '#334155',
              fontSize: '0.86rem',
              lineHeight: 1.5,
            }}
          >
            {additional.honors.map((honor, idx) => (
              <li key={idx} style={{ listStyleType: 'disc' }}>
                {honor}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* 7. Extra-Curricular Activities & Leadership (If extracted) */}
      {additional.activities.length > 0 && (
        <div
          style={{
            backgroundColor: '#ffffff',
            padding: '20px 24px',
            borderRadius: '10px',
            border: '1px solid #e2e8f0',
            boxShadow: '0 1px 3px rgba(0, 0, 0, 0.03)',
          }}
        >
          <h2
            style={{
              fontSize: '1rem',
              fontWeight: 700,
              color: '#1e293b',
              margin: '0 0 12px 0',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <Users size={18} color="#3b82f6" />
            Extra-Curricular Activities &amp; Leadership
          </h2>
          <ul
            style={{
              margin: 0,
              paddingLeft: '20px',
              display: 'flex',
              flexDirection: 'column',
              gap: '6px',
              color: '#334155',
              fontSize: '0.86rem',
              lineHeight: 1.5,
            }}
          >
            {additional.activities.map((act, idx) => (
              <li key={idx} style={{ listStyleType: 'disc' }}>
                {act}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* 8. Collapsible Verbatim Raw Extracted Text */}
      {candidate.rawText && (
        <div style={{ marginTop: '4px' }}>
          <button
            type="button"
            onClick={() => setShowRawText(!showRawText)}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              background: 'transparent',
              border: 'none',
              color: '#64748b',
              fontSize: '0.82rem',
              fontWeight: 600,
              cursor: 'pointer',
              padding: 0,
            }}
          >
            {showRawText ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
            <span>{showRawText ? 'Hide Verbatim Extracted Text' : 'Inspect Verbatim Extracted Text'}</span>
          </button>
          {showRawText && (
            <div
              style={{
                marginTop: '10px',
                background: '#0f172a',
                color: '#f8fafc',
                padding: '16px',
                borderRadius: '8px',
                fontSize: '0.78rem',
                fontFamily: 'monospace',
                maxHeight: '320px',
                overflowY: 'auto',
                whiteSpace: 'pre-wrap',
                lineHeight: 1.55,
              }}
            >
              {sanitizeText(candidate.rawText)}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
