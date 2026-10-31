import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, AuthResponse, ApiResponse } from '../types';
import { apiClient, extractErrorMessage } from '../api/client';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const savedUser = localStorage.getItem('talentmatch_user');
    return savedUser ? JSON.parse(savedUser) : null;
  });
  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem('talentmatch_token');
  });
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const verifySession = async () => {
      const storedToken = localStorage.getItem('talentmatch_token');
      if (!storedToken) {
        setIsLoading(false);
        return;
      }

      try {
        const response = await apiClient.get<ApiResponse<User>>('/auth/me');
        setUser(response.data.data);
        localStorage.setItem('talentmatch_user', JSON.stringify(response.data.data));
      } catch (err) {
        localStorage.removeItem('talentmatch_token');
        localStorage.removeItem('talentmatch_user');
        setUser(null);
        setToken(null);
      } finally {
        setIsLoading(false);
      }
    };

    verifySession();
  }, []);

  const login = async (email: string, password: string): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', {
        email: email.trim().toLowerCase(),
        password,
      });

      const { token: jwtToken, user: authenticatedUser } = response.data.data;
      setToken(jwtToken);
      setUser(authenticatedUser);

      localStorage.setItem('talentmatch_token', jwtToken);
      localStorage.setItem('talentmatch_user', JSON.stringify(authenticatedUser));
    } catch (err) {
      const message = extractErrorMessage(err);
      setError(message);
      throw new Error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    try {
      await apiClient.post('/auth/logout');
    } catch {
      // Ignore network errors on logout
    } finally {
      localStorage.removeItem('talentmatch_token');
      localStorage.removeItem('talentmatch_user');
      setUser(null);
      setToken(null);
    }
  };

  const clearError = () => setError(null);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        error,
        login,
        logout,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
