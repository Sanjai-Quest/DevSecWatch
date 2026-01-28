import api from './api';
import { Scan } from '../types';

export interface AnalyticsData {
    totalScans: number;
    totalVulnerabilities: number;
    avgVulnerabilitiesPerScan: number;
    criticalCount: number;
    highCount: number;
    mediumCount: number;
    lowCount: number;
    scanTrend: Array<{ date: string; count: number }>;
    severityDistribution: Array<{ name: string; value: number; color: string }>;
    topVulnerabilityTypes: Array<{ type: string; count: number }>;
    recentScans: Scan[];
}

export const getAnalyticsData = async (): Promise<AnalyticsData> => {
    // Fetch all user scans
    const { data } = await api.get('/api/scans?size=100');
    const scans: Scan[] = data.content || [];

    // Calculate statistics
    const totalScans = scans.length;
    const totalVulnerabilities = scans.reduce((sum, scan) => sum + (scan.totalVulnerabilities || 0), 0);
    const avgVulnerabilitiesPerScan = totalScans > 0 ? Math.round(totalVulnerabilities / totalScans) : 0;

    // Aggregate severity counts
    let criticalCount = 0;
    let highCount = 0;
    let mediumCount = 0;
    let lowCount = 0;

    scans.forEach(scan => {
        criticalCount += scan.criticalCount || 0;
        highCount += scan.highCount || 0;
        mediumCount += scan.mediumCount || 0;
        lowCount += scan.lowCount || 0;
    });

    // Group scans by date for trend
    const scansByDate = scans.reduce((acc, scan) => {
        // Basic date formatting, can be improved or use a library if needed
        const date = new Date(scan.createdAt || new Date()).toLocaleDateString();
        acc[date] = (acc[date] || 0) + 1;
        return acc;
    }, {} as Record<string, number>);

    const scanTrend = Object.entries(scansByDate)
        .map(([date, count]) => ({ date, count }))
        .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
        .slice(-7); // Last 7 days

    // Severity distribution for pie chart
    const severityDistribution = [
        { name: 'Critical', value: criticalCount, color: 'rgb(var(--color-danger))' },
        { name: 'High', value: highCount, color: 'rgb(var(--color-warning))' },
        { name: 'Medium', value: mediumCount, color: '#3b82f6' }, // Blue 500
        { name: 'Low', value: lowCount, color: 'rgb(var(--color-text-muted))' },
    ].filter(item => item.value > 0);

    // Mock vulnerability types (would need backend endpoint for real data)
    const topVulnerabilityTypes = [
        { type: 'SQL Injection', count: Math.floor(criticalCount * 0.3) + 1 },
        { type: 'XSS', count: Math.floor(highCount * 0.4) + 2 },
        { type: 'Hardcoded Secrets', count: Math.floor(criticalCount * 0.2) + 1 },
        { type: 'CORS Misconfiguration', count: Math.floor(highCount * 0.3) + 1 },
        { type: 'Path Traversal', count: Math.floor(mediumCount * 0.2) + 1 },
    ].filter(item => item.count > 0).slice(0, 10);

    // Recent scans (last 5)
    const recentScans = scans.slice(0, 5);

    return {
        totalScans,
        totalVulnerabilities,
        avgVulnerabilitiesPerScan,
        criticalCount,
        highCount,
        mediumCount,
        lowCount,
        scanTrend,
        severityDistribution,
        topVulnerabilityTypes,
        recentScans,
    };
};
