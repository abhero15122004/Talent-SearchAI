import React from 'react';

interface WeightMeterProps {
  skillWeight: number;
  expWeight: number;
  projectWeight: number;
  semanticWeight: number;
}

export const WeightMeter: React.FC<WeightMeterProps> = ({
  skillWeight,
  expWeight,
  projectWeight,
  semanticWeight,
}) => {
  const total = Number((skillWeight + expWeight + projectWeight + semanticWeight).toFixed(2));
  const isValid = Math.abs(total - 100) < 0.01;

  return (
    <div className="weight-meter-container">
      <div className="weight-meter-header">
        <span className="weight-meter-title">AI Matching Weight Distribution</span>
        <span className={`weight-meter-total ${isValid ? 'total-valid' : 'total-invalid'}`}>
          Total: {total}% {isValid ? '✓ (Balanced)' : '✗ (Must equal 100%)'}
        </span>
      </div>

      <div className="weight-bar-track">
        <div
          className="weight-bar-segment segment-skill"
          style={{ width: `${Math.max(0, skillWeight)}%` }}
          title={`Skill Coverage: ${skillWeight}%`}
        />
        <div
          className="weight-bar-segment segment-exp"
          style={{ width: `${Math.max(0, expWeight)}%` }}
          title={`Experience Relevance: ${expWeight}%`}
        />
        <div
          className="weight-bar-segment segment-project"
          style={{ width: `${Math.max(0, projectWeight)}%` }}
          title={`Project Relevance: ${projectWeight}%`}
        />
        <div
          className="weight-bar-segment segment-semantic"
          style={{ width: `${Math.max(0, semanticWeight)}%` }}
          title={`Semantic Similarity: ${semanticWeight}%`}
        />
      </div>

      <div className="weight-legend-grid">
        <div className="legend-item">
          <span className="legend-indicator dot-skill"></span>
          <span className="legend-label">Skills:</span>
          <span className="legend-value">{skillWeight}%</span>
        </div>
        <div className="legend-item">
          <span className="legend-indicator dot-exp"></span>
          <span className="legend-label">Experience:</span>
          <span className="legend-value">{expWeight}%</span>
        </div>
        <div className="legend-item">
          <span className="legend-indicator dot-project"></span>
          <span className="legend-label">Projects:</span>
          <span className="legend-value">{projectWeight}%</span>
        </div>
        <div className="legend-item">
          <span className="legend-indicator dot-semantic"></span>
          <span className="legend-label">Semantic AI:</span>
          <span className="legend-value">{semanticWeight}%</span>
        </div>
      </div>
    </div>
  );
};
