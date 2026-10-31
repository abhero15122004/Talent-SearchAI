import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Briefcase,
  FileText,
  Users,
  Sparkles,
  Settings,
  LucideIcon,
} from 'lucide-react';

interface NavItem {
  name: string;
  path: string;
  icon: LucideIcon;
  phaseBadge?: string;
}

const navItems: NavItem[] = [
  { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
  { name: 'Jobs', path: '/jobs', icon: Briefcase, phaseBadge: 'Phase 3' },
  { name: 'Resume Batches', path: '/batches', icon: FileText, phaseBadge: 'Phase 3' },
  { name: 'Candidates', path: '/candidates', icon: Users, phaseBadge: 'Phase 3' },
  { name: 'Matching Engine', path: '/matching', icon: Sparkles, phaseBadge: 'Phase 4' },
  { name: 'Settings', path: '/settings', icon: Settings },
];

export const Sidebar: React.FC = () => {
  return (
    <aside className="app-sidebar" aria-label="Main Navigation">
      <div className="sidebar-section-title">PLATFORM NAVIGATION</div>
      <nav className="sidebar-nav">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `nav-link ${isActive ? 'nav-link-active' : ''}`
              }
            >
              <Icon size={18} className="nav-icon" />
              <span className="nav-label">{item.name}</span>
              {item.phaseBadge && (
                <span className="nav-phase-badge">{item.phaseBadge}</span>
              )}
            </NavLink>
          );
        })}
      </nav>

      <div className="sidebar-footer">
        <div className="engine-status">
          <span className="status-indicator-dot"></span>
          <span className="engine-label">API Gateway Online</span>
        </div>
        <div className="version-tag">TalentMatch AI v0.1.0</div>
      </div>
    </aside>
  );
};
