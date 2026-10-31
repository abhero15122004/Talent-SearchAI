import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { SectionPlaceholderPage } from './pages/SectionPlaceholderPage';
import { JobsListPage } from './pages/jobs/JobsListPage';
import { JobCreateEditPage } from './pages/jobs/JobCreateEditPage';
import { JobDetailPage } from './pages/jobs/JobDetailPage';
import { ResumeIngestionPage } from './pages/resumes/ResumeIngestionPage';
import { CandidatesListPage } from './pages/candidates/CandidatesListPage';
import { CandidateDetailPage } from './pages/candidates/CandidateDetailPage';
import { Sparkles, Settings } from 'lucide-react';

const AppLayout: React.FC = () => {
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="main-wrapper">
        <Navbar />
        <main className="page-container" id="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Authentication Route */}
          <Route path="/login" element={<LoginPage />} />

          {/* Protected Recruiter Routes */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              
              {/* Job Requisition Management Routes */}
              <Route path="/jobs" element={<JobsListPage />} />
              <Route path="/jobs/new" element={<JobCreateEditPage />} />
              <Route path="/jobs/:id" element={<JobDetailPage />} />
              <Route path="/jobs/:id/edit" element={<JobCreateEditPage />} />

              {/* Resume Ingestion Pipeline */}
              <Route path="/batches" element={<ResumeIngestionPage />} />
              <Route path="/resumes" element={<ResumeIngestionPage />} />

              {/* Candidate Talent Pool */}
              <Route path="/candidates" element={<CandidatesListPage />} />
              <Route path="/candidates/:id" element={<CandidateDetailPage />} />

              <Route
                path="/matching"
                element={
                  <SectionPlaceholderPage
                    title="AI Semantic Matching &amp; Ranking Engine"
                    subtitle="Transparent, explainable talent-to-job recommendation algorithms"
                    icon={Sparkles}
                    targetPhase="Phase 4 Intelligence"
                    description="Dual-tower semantic retrieval with Sentence Transformers (all-MiniLM-L6-v2), cosine similarity scoring, and transparent recruiter explainability."
                    architectureDetails={[
                      'Dense vector embeddings via FastAPI Python microservice with all-MiniLM-L6-v2',
                      'Hybrid scoring combining semantic similarity, required skill overlap, and experience',
                      'Transparent score breakdowns (Skills Match %, Experience Match, Semantic Score)',
                      'Zero-fake rankings: strict evidence citations without ungrounded AI hallucinations',
                    ]}
                  />
                }
              />

              <Route
                path="/settings"
                element={
                  <SectionPlaceholderPage
                    title="Organization &amp; Recruiter Settings"
                    subtitle="Tenant preferences, security policies, and team member management"
                    icon={Settings}
                    targetPhase="Phase 1 Foundation"
                    description="Manage organization details, recruiter team accounts, password rotation, and audit retention policies."
                    architectureDetails={[
                      'Organization name and tenant ID verification',
                      'Recruiter team roster with role-based access control (ADMIN / RECRUITER)',
                      'Audit trail export and compliance logging',
                      'Session management and JWT secret lifecycle settings',
                    ]}
                  />
                }
              />

              {/* Redirect root and unmatched routes to dashboard */}
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Route>
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
