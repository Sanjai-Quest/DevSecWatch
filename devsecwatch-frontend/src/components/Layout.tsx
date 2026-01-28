import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { useThemeStore } from '../store/themeStore';
import { useWebSocket } from '../hooks/useWebSocket';
import NotificationCenter from './NotificationCenter';
import ThemeToggle from './ThemeToggle';

interface LayoutProps {
    children: React.ReactNode;
}

export default function Layout({ children }: LayoutProps) {
    const { theme } = useThemeStore();
    const { logout } = useAuthStore();
    const location = useLocation();
    const [username, setUsername] = useState<string | null>(null);

    // Effect to apply theme class to html/body
    useEffect(() => {
        const root = window.document.documentElement;
        root.classList.remove('light', 'dark');
        root.classList.add(theme);
    }, [theme]);

    useEffect(() => {
        console.log('🔍 [Layout] Extracting username from token...');
        const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
        console.log('🔍 [Layout] Token found:', token ? 'YES' : 'NO');

        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                console.log('🔍 [Layout] Token payload:', payload);
                console.log('🔍 [Layout] Extracted username (sub):', payload.sub);
                setUsername(payload.sub);
            } catch (e) {
                console.error('❌ [Layout] Failed to parse token', e);
            }
        } else {
            console.warn('⚠️ [Layout] No token found in storage');
        }
    }, [location.pathname]); // Re-check on nav change if needed, primarily once

    console.log('🔍 [Layout] Current username state:', username);
    useWebSocket(username);

    return (
        <div className="min-h-screen bg-background text-text font-sans transition-colors duration-300">
            {/* Navigation Bar */}
            <nav className="border-b border-border bg-surface/50 backdrop-blur-md sticky top-0 z-50">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between h-16 items-center">
                        <Link to="/dashboard" className="flex items-center gap-3 group">
                            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-secondary flex items-center justify-center shadow-lg group-hover:shadow-primary/50 transition-all">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /></svg>
                            </div>
                            <span className="font-bold text-xl tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-primary to-secondary">DevSecWatch</span>
                        </Link>

                        <div className="flex items-center gap-4">
                            <NotificationCenter />
                            <ThemeToggle />

                            <Link
                                to="/dashboard"
                                className="hidden sm:flex items-center gap-2 px-3 py-2 text-sm font-medium text-text-muted hover:text-text hover:bg-surface rounded-lg transition-colors border border-transparent hover:border-border"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
                                <span>Dashboard</span>
                            </Link>

                            <Link
                                to="/analytics"
                                className="hidden sm:flex items-center gap-2 px-3 py-2 text-sm font-medium text-text-muted hover:text-text hover:bg-surface rounded-lg transition-colors border border-transparent hover:border-border"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
                                <span>Analytics</span>
                            </Link>

                            <Link
                                to="/chat"
                                className="hidden sm:flex items-center gap-2 px-3 py-2 text-sm font-medium text-text-muted hover:text-text hover:bg-surface rounded-lg transition-colors border border-transparent hover:border-border"
                            >
                                <span>🤖</span>
                                <span>AI Assistant</span>
                            </Link>

                            {location.pathname !== '/scan/new' && (
                                <Link
                                    to="/scan/new"
                                    className="hidden sm:flex items-center gap-2 btn-primary text-sm py-2 shadow-lg shadow-primary/25 hover:shadow-primary/40"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                                    <span>New Scan</span>
                                </Link>
                            )}

                            <button
                                onClick={logout}
                                className="text-sm font-medium text-text-muted hover:text-text hover:bg-surface px-3 py-2 rounded-lg transition-colors border border-transparent hover:border-border"
                            >
                                Sign Out
                            </button>
                        </div>
                    </div>
                </div>
            </nav>

            {/* Main Content */}
            <main>
                {children}
            </main>
        </div>
    );
}
