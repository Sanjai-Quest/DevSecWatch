import api from './api';
import { AuthTokens } from '../types';

export const authService = {
    async register(username: string, email: string, password: string): Promise<AuthTokens> {
        const { data } = await api.post<AuthTokens>('/api/auth/register', {
            username,
            email,
            password,
        });
        this.setTokens(data);
        return data;
    },

    async login(email: string, password: string): Promise<AuthTokens> {
        const { data } = await api.post<AuthTokens>('/api/auth/login', {
            email,
            password,
        });
        this.setTokens(data);
        return data;
    },

    logout() {
        sessionStorage.clear();
        window.location.href = '/login';
    },

    setTokens(tokens: AuthTokens) {
        sessionStorage.setItem('accessToken', tokens.accessToken);
        sessionStorage.setItem('refreshToken', tokens.refreshToken);
    },

    isAuthenticated(): boolean {
        return !!sessionStorage.getItem('accessToken');
    },
};
