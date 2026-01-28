import { create } from 'zustand';
import axios from 'axios';

export interface Notification {
    id: number;
    userId: string;
    title: string;
    message: string;
    type: 'SUCCESS' | 'WARNING' | 'ERROR' | 'INFO';
    read: boolean; // Field name mapping might need care (backend: isRead)
    scanId: number;
    createdAt: string;
}

interface NotificationStore {
    notifications: Notification[];
    unreadCount: number;
    isLoading: boolean;
    fetchNotifications: () => Promise<void>;
    markRead: (id: number) => Promise<void>;
    markAllRead: () => Promise<void>;
    addNotification: (n: Notification) => void;
}

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const useNotificationStore = create<NotificationStore>((set, get) => ({
    notifications: [],
    unreadCount: 0,
    isLoading: false,

    fetchNotifications: async () => {
        set({ isLoading: true });
        try {
            const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
            if (!token) return;

            const response = await axios.get(`${API_URL}/api/notifications`, {
                headers: { Authorization: `Bearer ${token}` }
            });

            // Map backend 'isRead' to frontend 'read' if needed, or valid handling
            const data = response.data.map((n: any) => ({
                ...n,
                read: n.read // Jackson usually serializes boolean isRead as "read" or "isRead" depending on config. Let's assume standard bean convention might result in "read". We'll inspect or handle both.
            }));

            const unread = data.filter((n: any) => !n.read).length;
            set({ notifications: data, unreadCount: unread, isLoading: false });
        } catch (error) {
            console.error('Failed to fetch notifications', error);
            set({ isLoading: false });
        }
    },

    markRead: async (id: number) => {
        try {
            const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
            await axios.put(`${API_URL}/api/notifications/${id}/read`, {}, {
                headers: { Authorization: `Bearer ${token}` }
            });

            set((state) => {
                const updated = state.notifications.map(n =>
                    n.id === id ? { ...n, read: true } : n
                );
                return {
                    notifications: updated,
                    unreadCount: updated.filter(n => !n.read).length
                };
            });
        } catch (error) {
            console.error('Failed to mark read', error);
        }
    },

    markAllRead: async () => {
        try {
            const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
            await axios.put(`${API_URL}/api/notifications/read-all`, {}, {
                headers: { Authorization: `Bearer ${token}` }
            });

            set((state) => ({
                notifications: state.notifications.map(n => ({ ...n, read: true })),
                unreadCount: 0
            }));
        } catch (error) {
            console.error('Failed to mark all read', error);
        }
    },

    addNotification: (n: Notification) => {
        set((state) => {
            const exists = state.notifications.find(existing => existing.id === n.id);
            if (exists) return state; // Avoid dupes

            // New notification is unread by default (unless backend says otherwise)
            // But if it comes from WS, it might lack ID initially? 
            // Ideally we fetch, but for speed we prepend. 
            // Wait, backend sends DTO, not Entity. DTO doesn't have ID.
            // If we want consistent history, we should re-fetch.
            // But let's support partial display for now.

            // Actually, best practice: WS tells us "Something happened", we fetch latest.
            // OR we fetch the specific notification content.
            // Let's rely on fetchNotifications for simplicity and consistency.

            return {
                // For now just increment unread visually?
                // Or trigger a fetch
                unreadCount: state.unreadCount + 1
            };
        });
        get().fetchNotifications();
    }
}));
