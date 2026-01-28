import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { toast } from 'react-toastify';
import { useNotificationStore } from '../store/notificationStore';

interface ScanNotification {
    scanId: number;
    repoUrl: string;
    status: string;
    totalVulnerabilities: number;
    criticalCount: number;
    highCount: number;
    message: string;
    timestamp: string;
}

export const useWebSocket = (username: string | null, onScanUpdate?: () => void) => {
    const clientRef = useRef<Client | null>(null);
    const isConnected = useRef(false);
    const { fetchNotifications } = useNotificationStore();

    const connect = useCallback(() => {
        console.log('🔍 [WS] Connect called with username:', username);
        console.log('🔍 [WS] isConnected.current:', isConnected.current);

        // Prevent duplicate connections - check both flags AND client state
        if (!username || isConnected.current || clientRef.current?.connected) {
            console.log('⚠️ [WS] Skipping connection - No username or already connected', {
                username,
                isConnected: isConnected.current,
                clientConnected: clientRef.current?.connected
            });
            return;
        }

        console.log(`🔌 [WS] Initializing WebSocket for user: ${username}`);
        // toast.info(`Connecting to notifications as ${username}...`, { autoClose: 2000 });

        const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';

        const client = new Client({
            // Use SockJS fallback
            webSocketFactory: () => {
                console.log('🏭 [WS] Creating SockJS connection to:', `${apiUrl}/ws`);
                return new SockJS(`${apiUrl}/ws`);
            },

            onConnect: () => {
                console.log('✅ [WS] WebSocket connected successfully');
                toast.success(`🟢 Real-time updates active for ${username}`, { autoClose: 3000 });
                isConnected.current = true;

                // Subscribe to user-specific notifications
                console.log(`📡 [WS] Subscribing to: /queue/notifications/${username.toLowerCase()}`);
                client.subscribe(`/queue/notifications/${username.toLowerCase()}`, (message) => {
                    console.log('📬 Received raw message:', message.body);
                    const notification: ScanNotification = JSON.parse(message.body);

                    console.log('📬 Parsed notification:', notification);

                    // Show toast notification
                    if (notification.status === 'COMPLETED') {
                        if (notification.totalVulnerabilities === 0) {
                            toast.success(
                                `✅ ${notification.repoUrl.split('/').pop()} scan complete - No vulnerabilities!`,
                                { autoClose: 5000 }
                            );
                        } else {
                            toast.warning(
                                `⚠️ ${notification.repoUrl.split('/').pop()} scan complete - ${notification.totalVulnerabilities} issues found`,
                                { autoClose: 5000 }
                            );
                        }
                    } else if (notification.status === 'FAILED') {
                        toast.error(
                            `❌ ${notification.repoUrl.split('/').pop()} scan failed`,
                            { autoClose: 5000 }
                        );
                    } else {
                        // For generic messages (like from Smoke Test)
                        toast.info(`🔔 ${notification.message}`, { autoClose: 5000 });
                    }

                    // Trigger callback to refresh data
                    if (onScanUpdate) {
                        onScanUpdate();
                    }

                    // Refresh notifications list
                    fetchNotifications();
                });
            },

            onStompError: (frame) => {
                console.error('❌ STOMP error:', frame);
                toast.error('❌ Notification connection error', { autoClose: 5000 });
                isConnected.current = false;
            },

            onWebSocketClose: () => {
                console.log('🔌 WebSocket connection closed');
                isConnected.current = false;
            },

            // Reconnection settings
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
        });

        try {
            console.log('🚀 [WS] Activating STOMP client...');
            client.activate();
            clientRef.current = client;
            console.log('✅ [WS] Client activation initiated');
        } catch (error) {
            console.error('❌ [WS] Failed to activate client:', error);
            toast.error('Failed to connect to notification service');
        }
    }, [username, onScanUpdate, fetchNotifications]);

    const disconnect = useCallback(() => {
        if (clientRef.current) {
            clientRef.current.deactivate();
            clientRef.current = null;
            isConnected.current = false;
            console.log('🔌 WebSocket disconnected');
        }
    }, []);

    useEffect(() => {
        if (username) {
            connect();
        }

        return () => {
            disconnect();
        };
    }, [username, connect, disconnect]);

    return { connect, disconnect, isConnected: isConnected.current };
};
