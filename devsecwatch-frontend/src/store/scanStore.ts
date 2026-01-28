import { create } from 'zustand';
import api from '../services/api';
import { Scan, ScanWithVulnerabilities } from '../types';

interface ScanState {
    scans: Scan[];
    currentScan: ScanWithVulnerabilities | null;
    isLoading: boolean;
    error: string | null;

    fetchScans: () => Promise<void>;
    createScan: (repoUrl: string, branch: string) => Promise<number>;
    fetchScanById: (scanId: number) => Promise<void>;
    deleteScan: (scanId: number) => Promise<void>;
    pollScanStatus: (scanId: number, onComplete: () => void) => void;
}

export const useScanStore = create<ScanState>((set) => ({
    scans: [],
    currentScan: null,
    isLoading: false,
    error: null,

    fetchScans: async () => {
        set({ isLoading: true, error: null });
        try {
            const { data } = await api.get('/api/scans');
            // Backend returns Page object usually { content: [], ... }
            // Assuming backend API returns standard PageImpl JSON
            set({ scans: data.content || data, isLoading: false });
        } catch (error: any) {
            set({ error: error.response?.data?.message || 'Failed to fetch scans', isLoading: false });
        }
    },

    createScan: async (repoUrl, branch) => {
        set({ isLoading: true, error: null });
        try {
            const { data } = await api.post('/api/scans', { repoUrl, branch });
            set({ isLoading: false });
            return data.id;
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || 'Failed to create scan';
            set({ error: errorMessage, isLoading: false });
            throw new Error(errorMessage);
        }
    },

    fetchScanById: async (scanId) => {
        set({ isLoading: true, error: null });
        try {
            const { data } = await api.get(`/api/scans/${scanId}`);
            set({ currentScan: data, isLoading: false });
        } catch (error: any) {
            set({ error: error.response?.data?.message || 'Failed to fetch scan', isLoading: false });
        }
    },

    deleteScan: async (scanId) => {
        set({ isLoading: true, error: null });
        try {
            await api.delete(`/api/scans/${scanId}`);
            set((state) => ({
                scans: state.scans.filter((s) => s.id !== scanId),
                isLoading: false,
            }));
        } catch (error: any) {
            set({ error: error.response?.data?.message || 'Failed to delete scan', isLoading: false });
            throw error;
        }
    },

    pollScanStatus: (scanId, onComplete) => {
        const POLL_INTERVALS = [5000, 10000, 20000, 30000];
        let attempt = 0;

        const poll = async () => {
            try {
                const { data } = await api.get(`/api/scans/${scanId}`);

                if (data.status === 'COMPLETED' || data.status === 'FAILED') {
                    set({ currentScan: data });
                    onComplete();
                } else {
                    // If still processing, schedule next poll
                    const interval = POLL_INTERVALS[Math.min(attempt, POLL_INTERVALS.length - 1)];
                    attempt++;
                    // Only continue polling if we are still "viewing" or interested?
                    // For simplicity, we just recurse. In React effect we might need cleanup.
                    setTimeout(poll, interval);
                }
            } catch (error) {
                console.error('Polling failed:', error);
                // Retry logic on error? Slow down.
                setTimeout(poll, 10000);
            }
        };

        poll();
    },
}));
