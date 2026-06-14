import { type ReactNode, createContext, useContext, useState, useEffect } from 'react';

interface User {
  username: string;
  email: string;
  role?: string;
  avatarUrl?: string | null;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (username: string, accessToken: string, role?: string, avatarUrl?: string | null) => void;
  updateUserAvatar: (avatarUrl: string | null) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    const token = sessionStorage.getItem('access_token');
    const storedUser = sessionStorage.getItem('user');

    if (token && storedUser) {
      try {
        const userData = JSON.parse(storedUser);
        setUser(userData);
        setIsAuthenticated(true);
        setIsAdmin(userData.role === 'ADMIN');
      } catch {
        sessionStorage.removeItem('user');
        sessionStorage.removeItem('access_token');
      }
    }
  }, []);

  const login = (username: string, accessToken: string, role: string = 'USER', avatarUrl?: string | null) => {
    const userData: User = { username, email: '', role, avatarUrl };
    setUser(userData);
    setIsAuthenticated(true);
    setIsAdmin(role === 'ADMIN');
    sessionStorage.setItem('access_token', accessToken);
    sessionStorage.setItem('user', JSON.stringify(userData));
  };

  const updateUserAvatar = (avatarUrl: string | null) => {
    setUser(prev => {
      if (!prev) return prev;
      return { ...prev, avatarUrl };
    });
  };

  useEffect(() => {
    if (user) {
      sessionStorage.setItem('user', JSON.stringify(user));
    }
  }, [user]);

  const logout = () => {
    setUser(null);
    setIsAuthenticated(false);
    setIsAdmin(false);
    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('refresh_token');
    sessionStorage.removeItem('user');
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isAdmin, login, logout, updateUserAvatar }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
