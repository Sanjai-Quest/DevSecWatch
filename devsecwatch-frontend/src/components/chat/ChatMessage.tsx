import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { CodeBlock } from './CodeBlock';

interface ChatMessageProps {
    role: 'user' | 'assistant';
    content: string;
    timestamp: string;
}

export const ChatMessage: React.FC<ChatMessageProps> = ({ role, content, timestamp }) => {
    const isUser = role === 'user';

    return (
        <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`}>
            <div
                className={`max-w-[80%] rounded-lg px-4 py-3 ${isUser
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-700 text-slate-100'
                    }`}
            >
                {isUser ? (
                    <p className="whitespace-pre-wrap">{content}</p>
                ) : (
                    <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        components={{
                            code({ node, inline, className, children, ...props }: any) {
                                const match = /language-(\w+)/.exec(className || '');
                                return !inline && match ? (
                                    <CodeBlock
                                        language={match[1]}
                                        value={String(children).replace(/\n$/, '')}
                                    />
                                ) : (
                                    <code
                                        className="bg-slate-600 px-1 py-0.5 rounded text-sm font-mono"
                                        {...props}
                                    >
                                        {children}
                                    </code>
                                );
                            },
                            p({ children }) {
                                return <p className="mb-2 last:mb-0">{children}</p>;
                            },
                            ul({ children }) {
                                return <ul className="list-disc list-inside mb-2 space-y-1">{children}</ul>;
                            },
                            ol({ children }) {
                                return <ol className="list-decimal list-inside mb-2 space-y-1">{children}</ol>;
                            },
                            strong({ children }) {
                                return <strong className="font-semibold text-blue-300">{children}</strong>;
                            },
                            a({ href, children }: any) {
                                return (
                                    <a
                                        href={href}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="text-blue-400 hover:text-blue-300 underline"
                                    >
                                        {children}
                                    </a>
                                );
                            },
                        }}
                    >
                        {content}
                    </ReactMarkdown>
                )}
                <div className={`text-xs mt-2 ${isUser ? 'text-blue-200' : 'text-slate-400'}`}>
                    {new Date(timestamp).toLocaleTimeString()}
                </div>
            </div>
        </div>
    );
};
