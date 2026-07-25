import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import api from '../services/api';

interface AuthState {
  token: string | null;
  email: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (token: string, email: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'));
  const [email, setEmail] = useState<string | null>(() => localStorage.getItem('email'));
  const [isLoading, setIsLoading] = useState(() => !!localStorage.getItem('token'));

  useEffect(() => {
    if (!token) return;
    api.get('/documents')
      .then(() => setIsLoading(false))
      .catch(() => {
        localStorage.removeItem('token');
        localStorage.removeItem('email');
        setToken(null);
        setEmail(null);
        setIsLoading(false);
      });
  }, []);

  const login = useCallback((token: string, email: string) => {
    localStorage.setItem('token', token);
    localStorage.setItem('email', email);
    setToken(token);
    setEmail(email);
    setIsLoading(false);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('email');
    setToken(null);
    setEmail(null);
  }, []);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin" />
          <p className="text-gray-500 dark:text-gray-400">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <AuthContext.Provider value={{ token, email, isAuthenticated: !!token, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
