import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Building2, LogOut, ShieldCheck, User as UserIcon } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <header className="top-navbar">
      <div className="nav-left">
        <div className="brand-logo">
          <span className="brand-dot"></span>
          <span className="brand-name">TalentMatch <span className="brand-ai">AI</span></span>
        </div>
        {user?.organizationName && (
          <div className="tenant-pill" title={`Active Tenant ID: ${user.organizationId}`}>
            <Building2 size={15} />
            <span>{user.organizationName}</span>
          </div>
        )}
      </div>

      <div className="nav-right">
        <div className="user-profile">
          <div className="user-avatar" aria-hidden="true">
            {user?.role === 'ADMIN' ? <ShieldCheck size={18} /> : <UserIcon size={18} />}
          </div>
          <div className="user-details">
            <span className="user-name">{user?.fullName || 'Recruiter'}</span>
            <span className="user-role-badge">{user?.role}</span>
          </div>
        </div>

        <button
          onClick={() => logout()}
          className="btn-logout"
          title="Sign out of TalentMatch AI"
          aria-label="Sign out"
        >
          <LogOut size={16} />
          <span>Logout</span>
        </button>
      </div>
    </header>
  );
};
