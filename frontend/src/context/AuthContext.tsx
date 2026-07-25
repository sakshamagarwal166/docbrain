import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';

interface AuthState {
  token: string | null;
  email: string | null;
  isAuthenticated: boolean;
  login: (token: string, email: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'));
  const [email, setEmail] = useState<string | null>(() => localStorage.getItem('email'));

  const login = useCallback((token: string, email: string) => {
    localStorage.setItem('token', token);
    localStorage.setItem('email', email);
    setToken(token);
    setEmail(email);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('email');
    setToken(null);
    setEmail(null);
  }, []);

  return (
    <AuthContext.Provider value={{ token, email, isAuthenticated: !!token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
