import React from 'react';
import { JobStatus } from '../../types';

interface JobStatusBadgeProps {
  status: JobStatus;
}

export const JobStatusBadge: React.FC<JobStatusBadgeProps> = ({ status }) => {
  const getBadgeClass = (s: JobStatus): string => {
    switch (s) {
      case 'OPEN':
        return 'badge-status-open';
      case 'DRAFT':
        return 'badge-status-draft';
      case 'PAUSED':
        return 'badge-status-paused';
      case 'CLOSED':
        return 'badge-status-closed';
      case 'ARCHIVED':
        return 'badge-status-archived';
      default:
        return 'badge-status-draft';
    }
  };

  return (
    <span className={`job-status-badge ${getBadgeClass(status)}`}>
      <span className="status-dot"></span>
      <span>{status}</span>
    </span>
  );
};
