import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useScanStore } from '../store/scanStore';
import Layout from '../components/Layout';

export default function ScanDetail() {
    const { scanId } = useParams<{ scanId: string }>();
    const { currentScan, fetchScanById, pollScanStatus } = useScanStore();
    const [expandedVuln, setExpandedVuln] = useState<number | null>(null);

    useEffect(() => {
        if (scanId) {
            const id = parseInt(scanId, 10);
            fetchScanById(id);
            pollScanStatus(id, () => {
                fetchScanById(id);
            });
        }
    }, [scanId]);

    if (!currentScan) {
        return (
            <Layout>
                <div className="flex justify-center items-center min-h-[calc(100vh-4rem)]">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                </div>
            </Layout>
        );
    }

    const isProcessing = currentScan.status === 'QUEUED' || currentScan.status === 'PROCESSING';

    const getSeverityColor = (severity: string) => {
        switch (severity) {
            case 'CRITICAL': return 'bg-danger/10 text-danger border-danger/20';
            case 'HIGH': return 'bg-warning/10 text-warning border-warning/20';
            case 'MEDIUM': return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
            case 'LOW': return 'bg-surface text-text-muted border-border';
            default: return 'bg-surface text-text-muted border-border';
        }
    };

    return (
        <Layout>
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="mb-8 flex justify-between items-center">
                    <div>
                        <Link to="/dashboard" className="text-primary hover:text-primary-hover mb-2 inline-flex items-center gap-2 font-medium transition-colors">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m15 18-6-6 6-6" /></svg>
                            Back to Dashboard
                        </Link>
                        <h1 className="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-primary to-secondary mt-2">Scan Results</h1>
                        <p className="mt-2 text-text-muted font-mono bg-surface inline-block px-2 py-1 rounded border border-border">{currentScan.repoUrl}</p>
                    </div>
                    <div>
                        <span className="text-sm text-text-muted font-mono">ID: {currentScan.id}</span>
                    </div>
                </div>

                {isProcessing ? (
                    <div className="card text-center py-12">
                        <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-primary mx-auto mb-6"></div>
                        <p className="text-xl font-medium animate-pulse">Scanning repository...</p>
                        <p className="text-text-muted mt-2">Status: <span className="text-primary font-bold">{currentScan.status}</span></p>
                    </div>
                ) : currentScan.status === 'FAILED' ? (
                    <div className="bg-danger/10 border border-danger/20 text-danger px-6 py-4 rounded-xl">
                        <p className="font-bold text-lg flex items-center gap-2">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10" /><line x1="15" y1="9" x2="9" y2="15" /><line x1="9" y1="9" x2="15" y2="15" /></svg>
                            Scan Failed
                        </p>
                        <p className="mt-2 text-danger/80">{currentScan.errorMessage || 'An unexpected error occurred'}</p>
                    </div>
                ) : (
                    <div>
                        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
                            <div className="card">
                                <p className="text-sm text-text-muted uppercase tracking-wider font-semibold">Total Issues</p>
                                <p className="text-4xl font-bold mt-2">{currentScan.totalVulnerabilities}</p>
                            </div>
                            <div className="card border-danger/30 bg-danger/5">
                                <p className="text-sm text-danger uppercase tracking-wider font-semibold">Critical</p>
                                <p className="text-4xl font-bold text-danger mt-2">{currentScan.criticalCount}</p>
                            </div>
                            <div className="card border-warning/30 bg-warning/5">
                                <p className="text-sm text-warning uppercase tracking-wider font-semibold">High</p>
                                <p className="text-4xl font-bold text-warning mt-2">{currentScan.highCount}</p>
                            </div>
                            <div className="card border-blue-500/30 bg-blue-500/5">
                                <p className="text-sm text-blue-400 uppercase tracking-wider font-semibold">Medium</p>
                                <p className="text-4xl font-bold text-blue-400 mt-2">{currentScan.mediumCount}</p>
                            </div>
                        </div>

                        {currentScan.vulnerabilities && currentScan.vulnerabilities.length > 0 ? (
                            <div className="card overflow-hidden p-0">
                                <div className="px-6 py-4 border-b border-border bg-surface/50">
                                    <h2 className="text-lg font-semibold">Vulnerabilities Found</h2>
                                </div>
                                <div className="divide-y divide-border">
                                    {currentScan.vulnerabilities.map((vuln) => (
                                        <div key={vuln.id} className="p-6 hover:bg-surface/30 transition-colors">
                                            <div className="flex justify-between items-start mb-4">
                                                <div className="flex-1">
                                                    <h3 className="text-lg font-semibold text-text flex items-center gap-2">
                                                        {vuln.vulnerabilityType.replace(/_/g, ' ')}
                                                    </h3>
                                                    <p className="text-sm text-text-muted mt-1 font-mono">
                                                        {vuln.filePath}:{vuln.lineNumber}
                                                    </p>
                                                </div>
                                                <div className="flex gap-2">
                                                    <span className={`px-3 py-1 text-xs font-semibold rounded-full border ${getSeverityColor(vuln.severity)}`}>
                                                        {vuln.severity}
                                                    </span>
                                                    <span className="px-3 py-1 text-xs font-semibold rounded-full bg-surface text-text-muted border border-border">
                                                        {vuln.confidence} Confidence
                                                    </span>
                                                </div>
                                            </div>

                                            <p className="text-text-muted mb-4 leading-relaxed">{vuln.description}</p>

                                            <button
                                                onClick={() => setExpandedVuln(expandedVuln === vuln.id ? null : vuln.id)}
                                                className="text-primary hover:text-primary-hover text-sm font-medium flex items-center gap-1 transition-colors"
                                            >
                                                {expandedVuln === vuln.id ? 'Hide Details' : 'Show Details'}
                                            </button>

                                            {expandedVuln === vuln.id && (
                                                <div className="mt-4 space-y-4 animate-in fade-in slide-in-from-top-2 duration-200">
                                                    <div>
                                                        <p className="text-sm font-medium text-text-muted mb-2 uppercase tracking-wide">Code Snippet:</p>
                                                        <pre className="bg-black/30 border border-border text-text-muted p-4 rounded-lg overflow-x-auto text-sm font-mono leading-relaxed">
                                                            <code>{vuln.codeSnippet}</code>
                                                        </pre>
                                                    </div>
                                                    <div>
                                                        <p className="text-sm font-medium text-text-muted mb-2 uppercase tracking-wide">AI Recommendation:</p>
                                                        <div className="bg-primary/5 border border-primary/20 p-4 rounded-lg">
                                                            <div className="flex items-start gap-3">
                                                                <div className="mt-0.5 text-primary">
                                                                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z" /><path d="M19 10v2a7 7 0 0 1-14 0v-2" /><line x1="12" y1="19" x2="12" y2="22" /><line x1="8" y1="22" x2="16" y2="22" /></svg>
                                                                </div>
                                                                <p className="text-text leading-relaxed">{vuln.fixSuggestion}</p>
                                                            </div>
                                                        </div>
                                                    </div>
                                                    {vuln.cveId && (
                                                        <p className="text-sm text-text-muted">
                                                            Related CVE: <a href={`https://cve.mitre.org/cgi-bin/cvename.cgi?name=${vuln.cveId}`} target="_blank" rel="noopener noreferrer" className="text-primary hover:underline font-mono">{vuln.cveId}</a>
                                                        </p>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        ) : (
                            <div className="bg-success/10 border border-success/20 text-success px-6 py-8 rounded-xl text-center">
                                <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-success/20 mb-4">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /></svg>
                                </div>
                                <h3 className="text-xl font-bold">No Vulnerabilities Found!</h3>
                                <p className="mt-2 text-success/80">Excellent work. Your code appears to be secure based on our checks.</p>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </Layout>
    );
}
