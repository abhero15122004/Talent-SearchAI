import React from 'react';

interface LoadingSpinnerProps {
  size?: 'small' | 'medium' | 'large';
  className?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ size = 'medium', className = '' }) => {
  const sizeMap = {
    small: 'spinner-sm',
    medium: 'spinner-md',
    large: 'spinner-lg',
  };

  return (
    <div className={`spinner ${sizeMap[size]} ${className}`} aria-label="Loading" role="status">
      <span className="sr-only">Loading...</span>
    </div>
  );
};
