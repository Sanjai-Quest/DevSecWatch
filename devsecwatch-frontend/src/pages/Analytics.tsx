import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
    LineChart, Line, BarChart, Bar, PieChart, Pie, Cell,
    XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';
import { getAnalyticsData, AnalyticsData } from '../services/analytics';
import Layout from '../components/Layout';

export default function Analytics() {
    const [data, setData] = useState<AnalyticsData | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            try {
                const analyticsData = await getAnalyticsData();
                setData(analyticsData);
            } catch (error) {
                console.error('Failed to fetch analytics', error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchData();
    }, []);

    if (isLoading) {
        return (
            <Layout>
                <div className="flex justify-center items-center min-h-[calc(100vh-4rem)]">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                </div>
            </Layout>
        );
    }

    if (!data) {
        return (
            <Layout>
                <div className="flex justify-center items-center min-h-[calc(100vh-4rem)]">
                    <p className="text-text-muted">Failed to load analytics data</p>
                </div>
            </Layout>
        );
    }

    return (
        <Layout>
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-text">Security Analytics</h1>
                    <p className="mt-2 text-text-muted">Insights from your vulnerability scans</p>
                </div>

                {/* Summary Cards */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                    <div className="card">
                        <p className="text-text-muted text-sm uppercase font-semibold tracking-wider">Total Scans</p>
                        <p className="text-3xl font-bold text-primary mt-2">{data.totalScans}</p>
                    </div>
                    <div className="card border-danger/20 bg-danger/5">
                        <p className="text-danger text-sm uppercase font-semibold tracking-wider">Total Vulnerabilities</p>
                        <p className="text-3xl font-bold text-danger mt-2">{data.totalVulnerabilities}</p>
                    </div>
                    <div className="card border-warning/20 bg-warning/5">
                        <p className="text-warning text-sm uppercase font-semibold tracking-wider">Avg per Scan</p>
                        <p className="text-3xl font-bold text-warning mt-2">{data.avgVulnerabilitiesPerScan}</p>
                    </div>
                    <div className="card border-red-500/20 bg-red-500/5">
                        <p className="text-red-500 text-sm uppercase font-semibold tracking-wider">Critical Issues</p>
                        <p className="text-3xl font-bold text-red-500 mt-2">{data.criticalCount}</p>
                    </div>
                </div>

                {/* Charts Row 1 */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                    {/* Scan Trend Chart */}
                    <div className="card">
                        <h2 className="text-xl font-semibold mb-6 text-text">Scan Activity (Last 7 Days)</h2>
                        <div className="h-[300px] w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <LineChart data={data.scanTrend}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(156, 163, 175, 0.2)" />
                                    <XAxis dataKey="date" stroke="rgb(var(--color-text-muted))" fontSize={12} tickLine={false} axisLine={false} />
                                    <YAxis stroke="rgb(var(--color-text-muted))" fontSize={12} tickLine={false} axisLine={false} />
                                    <Tooltip
                                        contentStyle={{
                                            backgroundColor: 'rgb(var(--color-surface))',
                                            border: '1px solid rgb(var(--color-border))',
                                            borderRadius: '8px',
                                            color: 'rgb(var(--color-text))'
                                        }}
                                        itemStyle={{ color: 'rgb(var(--color-text))' }}
                                        labelStyle={{ color: 'rgb(var(--color-text-muted))', marginBottom: '0.25rem' }}
                                    />
                                    <Legend wrapperStyle={{ paddingTop: '10px' }} />
                                    <Line type="monotone" dataKey="count" stroke="rgb(var(--color-primary))" strokeWidth={3} dot={{ r: 4, strokeWidth: 2 }} activeDot={{ r: 6 }} name="Scans" />
                                </LineChart>
                            </ResponsiveContainer>
                        </div>
                    </div>

                    {/* Severity Distribution */}
                    <div className="card">
                        <h2 className="text-xl font-semibold mb-6 text-text">Severity Distribution</h2>
                        <div className="h-[300px] w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Pie
                                        data={data.severityDistribution}
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={60}
                                        outerRadius={100}
                                        fill="#8884d8"
                                        dataKey="value"
                                        paddingAngle={5}
                                        label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                                        labelLine={false}
                                    >
                                        {data.severityDistribution.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={entry.color} stroke="rgb(var(--color-surface))" strokeWidth={2} />
                                        ))}
                                    </Pie>
                                    <Tooltip
                                        contentStyle={{
                                            backgroundColor: 'rgb(var(--color-surface))',
                                            border: '1px solid rgb(var(--color-border))',
                                            borderRadius: '8px',
                                            color: 'rgb(var(--color-text))'
                                        }}
                                        itemStyle={{ color: 'rgb(var(--color-text))' }}
                                    />
                                    <Legend />
                                </PieChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                </div>

                {/* Charts Row 2 */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
                    {/* Top Vulnerability Types */}
                    <div className="card">
                        <h2 className="text-xl font-semibold mb-6 text-text">Most Common Vulnerabilities</h2>
                        <div className="h-[300px] w-full">
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart data={data.topVulnerabilityTypes} layout="vertical" margin={{ left: 40 }}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(156, 163, 175, 0.2)" horizontal={false} />
                                    <XAxis type="number" stroke="rgb(var(--color-text-muted))" fontSize={12} tickLine={false} axisLine={false} />
                                    <YAxis dataKey="type" type="category" stroke="rgb(var(--color-text-muted))" width={120} fontSize={12} tickLine={false} axisLine={false} />
                                    <Tooltip
                                        cursor={{ fill: 'rgba(156, 163, 175, 0.1)' }}
                                        contentStyle={{
                                            backgroundColor: 'rgb(var(--color-surface))',
                                            border: '1px solid rgb(var(--color-border))',
                                            borderRadius: '8px',
                                            color: 'rgb(var(--color-text))'
                                        }}
                                    />
                                    <Bar dataKey="count" fill="rgb(var(--color-primary))" name="Count" radius={[0, 4, 4, 0]} barSize={20} />
                                </BarChart>
                            </ResponsiveContainer>
                        </div>
                    </div>

                    {/* Recent Activity */}
                    <div className="card">
                        <h2 className="text-xl font-semibold mb-6 text-text">Recent Scans</h2>
                        <div className="space-y-3">
                            {data.recentScans.map(scan => (
                                <Link
                                    key={scan.id}
                                    to={`/scan/${scan.id}`}
                                    className="block p-4 rounded-lg bg-surface/50 border border-border hover:border-primary/50 hover:bg-surface transition-all duration-200 group"
                                >
                                    <div className="flex justify-between items-center">
                                        <div className="flex-1 min-w-0 mr-4">
                                            <p className="text-sm font-medium text-text truncate group-hover:text-primary transition-colors">
                                                {scan.repoUrl.replace('https://github.com/', '')}
                                            </p>
                                            <p className="text-xs text-text-muted mt-1 font-mono">
                                                {new Date(scan.createdAt).toLocaleDateString()} • {new Date(scan.createdAt).toLocaleTimeString()}
                                            </p>
                                        </div>
                                        <div className="text-right flex flex-col items-end">
                                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-surface border border-border text-text-muted group-hover:border-primary/30 group-hover:text-primary transition-colors">
                                                {scan.totalVulnerabilities} issues
                                            </span>
                                            {scan.criticalCount > 0 && (
                                                <span className="text-xs text-danger mt-1 font-medium flex items-center gap-1">
                                                    <span className="w-1.5 h-1.5 rounded-full bg-danger"></span>
                                                    {scan.criticalCount} critical
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                </Link>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </Layout>
    );
}
