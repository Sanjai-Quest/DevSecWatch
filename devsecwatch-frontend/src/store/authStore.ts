import { create } from 'zustand';
import { authService } from '../services/auth';

interface AuthState {
    isAuthenticated: boolean;
    login: (email: string, password: string) => Promise<void>;
    register: (username: string, email: string, password: string) => Promise<void>;
    logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    isAuthenticated: authService.isAuthenticated(),
    login: async (email, password) => {
        await authService.login(email, password);
        set({ isAuthenticated: true });
    },
    register: async (username, email, password) => {
        await authService.register(username, email, password);
        set({ isAuthenticated: true });
    },
    logout: () => {
        authService.logout();
        set({ isAuthenticated: false });
    },
}));
