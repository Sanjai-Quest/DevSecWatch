import api from './api';
import { Scan, ScanRequest, ScanResponse, PaginatedResponse } from '../types';

export const scanService = {
    async createScan(repoUrl: string, branch: string = 'main'): Promise<ScanResponse> {
        const { data } = await api.post<ScanResponse>('/api/scans', {
            repoUrl,
            branch,
        } as ScanRequest);
        return data;
    },

    async getScan(id: number): Promise<ScanResponse> {
        const { data } = await api.get<ScanResponse>(`/api/scans/${id}`);
        return data;
    },

    async getUserScans(page: number = 0, size: number = 20): Promise<PaginatedResponse<ScanResponse>> {
        const { data } = await api.get<PaginatedResponse<ScanResponse>>('/api/scans', {
            params: { page, size },
        });
        return data;
    },

    async deleteScan(id: number): Promise<void> {
        await api.delete(`/api/scans/${id}`);
    }
};
