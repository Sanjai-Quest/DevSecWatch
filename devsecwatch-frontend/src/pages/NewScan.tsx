import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useScanStore } from '../store/scanStore';
import Layout from '../components/Layout';

export default function NewScan() {
    const [repoUrl, setRepoUrl] = useState('');
    const [branch, setBranch] = useState('main');
    const [errors, setErrors] = useState<string[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const createScan = useScanStore((state) => state.createScan);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setErrors([]);
        setIsSubmitting(true);
        try {
            const scanId = await createScan(repoUrl, branch);
            navigate(`/scan/${scanId}`);
        } catch (err: any) {
            console.error('Create scan failed:', err.response?.data || err);
            const responseErrors = err.response?.data?.errors;
            if (Array.isArray(responseErrors)) {
                setErrors(responseErrors);
            } else {
                setErrors([err.message]);
            }
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <Layout>
            <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-text">New Security Scan</h1>
                    <p className="text-text-muted mt-2">Enter your repository details to start analyzing vulnerabilities.</p>
                </div>

                <div className="card">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {errors.length > 0 && (
                            <div className="bg-danger/10 border border-danger/20 text-danger px-4 py-3 rounded-lg text-sm">
                                <ul className="list-disc list-inside">
                                    {errors.map((err, i) => (
                                        <li key={i}>{err}</li>
                                    ))}
                                </ul>
                            </div>
                        )}

                        <div>
                            <label className="block text-sm font-medium text-text-muted mb-1">
                                GitHub Repository URL
                            </label>
                            <input
                                type="url"
                                value={repoUrl}
                                onChange={(e) => setRepoUrl(e.target.value)}
                                placeholder="https://github.com/username/repository"
                                required
                                className="input-field"
                            />
                            <p className="mt-2 text-xs text-text-muted">
                                Must be a public GitHub repository (e.g. https://github.com/Sanjai-Quest/OccasionAI_hackathon.git)
                            </p>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-text-muted mb-1">
                                Branch
                            </label>
                            <input
                                type="text"
                                value={branch}
                                onChange={(e) => setBranch(e.target.value)}
                                placeholder="main"
                                required
                                className="input-field"
                            />
                        </div>

                        <div className="flex gap-4 pt-4">
                            <button
                                type="submit"
                                disabled={isSubmitting}
                                className="flex-1 btn-primary shadow-lg shadow-primary/20"
                            >
                                {isSubmitting ? (
                                    <div className="flex items-center justify-center gap-2">
                                        <div className="w-4 h-4 border-2 border-white/50 border-t-white rounded-full animate-spin"></div>
                                        <span>Starting...</span>
                                    </div>
                                ) : 'Start Scan'}
                            </button>
                            <button
                                type="button"
                                onClick={() => navigate('/dashboard')}
                                className="px-4 py-2 border border-border rounded-lg hover:bg-surface text-text-muted hover:text-text transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </Layout>
    );
}
