import React from 'react';
import { LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  icon: LucideIcon;
  title: string;
  description: string;
  badge?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon,
  title,
  description,
  badge,
  actionLabel,
  onAction,
}) => {
  return (
    <div className="empty-state">
      <div className="empty-icon-wrapper">
        <Icon className="empty-icon" size={32} />
      </div>
      {badge && <span className="empty-badge">{badge}</span>}
      <h3 className="empty-title">{title}</h3>
      <p className="empty-description">{description}</p>
      {actionLabel && onAction && (
        <button onClick={onAction} className="btn-primary mt-3" type="button">
          {actionLabel}
        </button>
      )}
    </div>
  );
};
