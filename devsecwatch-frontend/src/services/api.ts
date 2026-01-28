import axios from 'axios';
import { AuthTokens } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor: Add JWT
api.interceptors.request.use(
    (config) => {
        const token = sessionStorage.getItem('accessToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Response interceptor: Handle token refresh
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            const refreshToken = sessionStorage.getItem('refreshToken');
            if (refreshToken) {
                try {
                    const { data } = await axios.post<AuthTokens>(`${API_BASE_URL}/api/auth/refresh`, {
                        refreshToken,
                    });

                    sessionStorage.setItem('accessToken', data.accessToken);
                    originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;

                    return api(originalRequest);
                } catch (refreshError) {
                    sessionStorage.clear();
                    window.location.href = '/login';
                    return Promise.reject(refreshError);
                }
            } else {
                sessionStorage.clear();
                window.location.href = '/login';
            }
        }

        return Promise.reject(error);
    }
);

export default api;
