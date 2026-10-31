import React from 'react';
import { LucideIcon } from 'lucide-react';
import { EmptyState } from '../components/EmptyState';

interface SectionPlaceholderProps {
  title: string;
  subtitle: string;
  icon: LucideIcon;
  targetPhase: string;
  description: string;
  architectureDetails: string[];
}

export const SectionPlaceholderPage: React.FC<SectionPlaceholderProps> = ({
  title,
  subtitle,
  icon: Icon,
  targetPhase,
  description,
  architectureDetails,
}) => {
  return (
    <div className="section-placeholder-page">
      <div className="section-header">
        <div className="section-title-group">
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>
        <span className="badge-phase-target">{targetPhase}</span>
      </div>

      <div className="content-card">
        <div className="card-body">
          <EmptyState
            icon={Icon}
            title={`${title} Module`}
            description={description}
            badge={targetPhase}
          />

          <div className="module-blueprint">
            <h3>Architectural Blueprint &bull; {targetPhase}</h3>
            <ul className="blueprint-list">
              {architectureDetails.map((detail, idx) => (
                <li key={idx}>
                  <span className="blueprint-check">&bull;</span>
                  <span>{detail}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};
