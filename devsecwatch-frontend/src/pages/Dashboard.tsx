import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useScanStore } from '../store/scanStore';

import Layout from '../components/Layout';

export default function Dashboard() {
    const { scans, isLoading, fetchScans, deleteScan } = useScanStore();
    useEffect(() => {
        fetchScans();
    }, []);

    const handleDelete = async (id: number, e: React.MouseEvent) => {
        e.preventDefault();
        if (confirm('Are you sure you want to delete this scan?')) {
            await deleteScan(id);
        }
    };

    const getStatusColor = (status: string) => {
        switch (status) {
            case 'COMPLETED': return 'text-success bg-success/10 border-success/20';
            case 'FAILED': return 'text-danger bg-danger/10 border-danger/20';
            case 'PROCESSING': return 'text-primary bg-primary/10 border-primary/20 animate-pulse';
            default: return 'text-warning bg-warning/10 border-warning/20';
        }
    };

    return (
        <Layout>
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-text">Security Dashboard</h1>
                    <p className="mt-2 text-text-muted text-lg">Manage and monitor your repository security scans.</p>
                </div>

                {/* Content */}
                {isLoading && scans.length === 0 ? (
                    <div className="flex justify-center py-20">
                        <div className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                    </div>
                ) : scans.length === 0 ? (
                    <div className="text-center py-20 card bg-surface/30 border-dashed border-2 border-border/50">
                        <div className="mb-4 text-text-muted opacity-50">
                            <svg className="mx-auto w-16 h-16" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>
                        <h3 className="text-xl font-medium text-text">No scans yet</h3>
                        <p className="text-text-muted mt-2">Start your first security analysis to see results here.</p>
                        <Link to="/scan/new" className="mt-6 inline-block text-primary hover:text-primary-hover font-medium">Create Scan &rarr;</Link>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
                        {scans.map((scan) => (
                            <Link
                                key={scan.id}
                                to={`/scan/${scan.id}`}
                                className="card group hover:border-primary/30 transition-all duration-300 hover:shadow-2xl hover:bg-surface/60 relative flex flex-col h-full border-border/50 bg-surface/40"
                            >
                                <div className="flex justify-between items-start mb-5">
                                    <div className="flex-1 min-w-0 pr-4">
                                        <h3 className="text-lg font-semibold text-text truncate group-hover:text-primary transition-colors" title={scan.repoUrl}>
                                            {scan.repoUrl.replace('https://github.com/', '').split('/')[1] || scan.repoUrl.split('/').pop()}
                                        </h3>
                                        <div className="flex items-center gap-2 mt-2">
                                            <span className="text-xs px-2 py-0.5 rounded bg-surface border border-border text-text-muted font-mono">
                                                {scan.branch}
                                            </span>
                                        </div>
                                    </div>
                                    <span className={`px-3 py-1 text-xs font-semibold rounded-full border ${getStatusColor(scan.status)}`}>
                                        {scan.status}
                                    </span>
                                </div>

                                <div className="space-y-4 flex-grow">
                                    {scan.status === 'COMPLETED' ? (
                                        <div className="grid grid-cols-3 gap-2 text-center py-2">
                                            <div className="bg-danger/10 rounded-lg py-2 border border-danger/10">
                                                <span className="text-danger font-bold text-xl block">{scan.criticalCount}</span>
                                                <span className="text-text-muted text-[10px] uppercase tracking-wider font-semibold">Critical</span>
                                            </div>
                                            <div className="bg-warning/10 rounded-lg py-2 border border-warning/10">
                                                <span className="text-warning font-bold text-xl block">{scan.highCount}</span>
                                                <span className="text-text-muted text-[10px] uppercase tracking-wider font-semibold">High</span>
                                            </div>
                                            <div className="bg-blue-500/10 rounded-lg py-2 border border-blue-500/10">
                                                <span className="text-blue-400 font-bold text-xl block">{scan.mediumCount}</span>
                                                <span className="text-text-muted text-[10px] uppercase tracking-wider font-semibold">Med</span>
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="flex flex-col items-center justify-center h-20 text-text-muted/60 text-sm italic bg-surface/30 rounded-lg border border-border/30">
                                            {scan.status === 'PROCESSING' ? (
                                                <>
                                                    <span className="animate-pulse">Analysing repository...</span>
                                                    <div className="w-1/2 h-1 bg-border rounded-full mt-2 overflow-hidden">
                                                        <div className="h-full bg-primary animate-[shimmer_2s_infinite]"></div>
                                                    </div>
                                                </>
                                            ) : (
                                                <span>Analysis pending or failed</span>
                                            )}
                                        </div>
                                    )}
                                </div>

                                <div className="mt-6 flex justify-between items-center pt-4 border-t border-border/40">
                                    <span className="text-xs text-text-muted font-mono">
                                        {new Date(scan.createdAt).toLocaleDateString()}
                                    </span>

                                    {(scan.status === 'FAILED' || scan.status === 'QUEUED') && (
                                        <button
                                            onClick={(e) => handleDelete(scan.id, e)}
                                            className="p-2 -mr-2 text-text-muted/60 hover:text-danger hover:bg-danger/10 rounded-lg transition-all z-10 opacity-0 group-hover:opacity-100"
                                            title="Delete Scan"
                                        >
                                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                <path d="M3 6h18"></path>
                                                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                            </svg>
                                        </button>
                                    )}
                                </div>
                            </Link>
                        ))}
                    </div>
                )}
            </div>
        </Layout>
    );
}
