import React, { useState } from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

interface CodeBlockProps {
    language: string;
    value: string;
}

export const CodeBlock: React.FC<CodeBlockProps> = ({ language, value }) => {
    const [copied, setCopied] = useState(false);

    const handleCopy = () => {
        navigator.clipboard.writeText(value);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <div className="relative group my-4">
            <div className="flex items-center justify-between bg-slate-700 px-4 py-2 rounded-t-lg">
                <span className="text-xs text-slate-300 font-mono">{language}</span>
                <button
                    onClick={handleCopy}
                    className="text-xs px-3 py-1 bg-slate-600 hover:bg-slate-500 text-slate-200 rounded transition-colors"
                >
                    {copied ? '✓ Copied!' : 'Copy'}
                </button>
            </div>
            <SyntaxHighlighter
                language={language}
                style={vscDarkPlus}
                customStyle={{
                    margin: 0,
                    borderRadius: '0 0 0.5rem 0.5rem',
                    fontSize: '0.875rem',
                }}
            >
                {value}
            </SyntaxHighlighter>
        </div>
    );
};
