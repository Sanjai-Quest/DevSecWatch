import React, { useEffect, useRef } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useChat } from '../hooks/useChat';
import { ChatMessage } from '../components/chat/ChatMessage';
import { ChatInput } from '../components/chat/ChatInput';
import { TypingIndicator } from '../components/chat/TypingIndicator';
import { SuggestedQuestions } from '../components/chat/SuggestedQuestions';

const ChatAssistant: React.FC = () => {
    const [searchParams] = useSearchParams();
    const scanId = searchParams.get('scanId');

    const {
        messages,
        isLoading,
        error,
        suggestedQuestions,
        sendMessage,
        clearChat,
    } = useChat(scanId ? parseInt(scanId) : undefined, true);

    const messagesEndRef = useRef<HTMLDivElement>(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages, isLoading]);

    const showWelcomeMessage = messages.length === 0;

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 text-slate-100">
            <div className="max-w-5xl mx-auto h-screen flex flex-col">
                {/* Header */}
                <div className="border-b border-slate-700 px-6 py-4 bg-slate-800/50 backdrop-blur-sm">
                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-2xl font-bold text-slate-100 flex items-center gap-2">
                                🤖 AI Security Assistant
                            </h1>
                            <p className="text-sm text-slate-400 mt-1">
                                Ask me anything about vulnerabilities, security best practices, or remediation
                            </p>
                        </div>
                        <div className="flex items-center gap-3">
                            {messages.length > 0 && (
                                <button
                                    onClick={clearChat}
                                    className="px-4 py-2 text-sm bg-slate-700 text-slate-200 rounded-lg hover:bg-slate-600 transition-colors"
                                >
                                    🗑️ Clear Chat
                                </button>
                            )}
                            <Link
                                to="/dashboard"
                                className="px-4 py-2 text-sm bg-slate-700 text-slate-200 rounded-lg hover:bg-slate-600 transition-colors"
                            >
                                ← Dashboard
                            </Link>
                        </div>
                    </div>
                </div>

                {/* Messages Container */}
                <div className="flex-1 overflow-y-auto px-6 py-6">
                    {showWelcomeMessage ? (
                        <div className="text-center mt-16">
                            <div className="text-6xl mb-4">🤖</div>
                            <h2 className="text-2xl font-semibold mb-2">Welcome to AI Security Assistant</h2>
                            <p className="text-slate-400 mb-8 max-w-2xl mx-auto">
                                I'm here to help you understand security vulnerabilities, provide remediation guidance,
                                and answer your security questions. I have access to your scan results for personalized advice.
                            </p>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 max-w-3xl mx-auto">
                                <button
                                    onClick={() => sendMessage('What are the most critical vulnerabilities in my scans?')}
                                    className="p-4 bg-slate-800 rounded-lg hover:bg-slate-700 transition-colors text-left border border-slate-700"
                                >
                                    <div className="text-2xl mb-2">🔍</div>
                                    <h3 className="font-semibold mb-1">Analyze My Scans</h3>
                                    <p className="text-sm text-slate-400">Get insights on your vulnerabilities</p>
                                </button>
                                <button
                                    onClick={() => sendMessage('How do I fix SQL injection vulnerabilities?')}
                                    className="p-4 bg-slate-800 rounded-lg hover:bg-slate-700 transition-colors text-left border border-slate-700"
                                >
                                    <div className="text-2xl mb-2">🛠️</div>
                                    <h3 className="font-semibold mb-1">Fix Vulnerabilities</h3>
                                    <p className="text-sm text-slate-400">Get step-by-step remediation</p>
                                </button>
                                <button
                                    onClick={() => sendMessage('Explain the OWASP Top 10 vulnerabilities')}
                                    className="p-4 bg-slate-800 rounded-lg hover:bg-slate-700 transition-colors text-left border border-slate-700"
                                >
                                    <div className="text-2xl mb-2">📚</div>
                                    <h3 className="font-semibold mb-1">Learn Security</h3>
                                    <p className="text-sm text-slate-400">Understand security concepts</p>
                                </button>
                                <button
                                    onClick={() => sendMessage('What are security best practices for Spring Boot?')}
                                    className="p-4 bg-slate-800 rounded-lg hover:bg-slate-700 transition-colors text-left border border-slate-700"
                                >
                                    <div className="text-2xl mb-2">✨</div>
                                    <h3 className="font-semibold mb-1">Best Practices</h3>
                                    <p className="text-sm text-slate-400">Framework-specific guidance</p>
                                </button>
                            </div>
                        </div>
                    ) : (
                        <>
                            {messages.map((message, index) => (
                                <ChatMessage
                                    key={message.messageId || index}
                                    role={message.role}
                                    content={message.content}
                                    timestamp={message.timestamp}
                                />
                            ))}
                            {isLoading && <TypingIndicator />}
                            {error && (
                                <div className="bg-red-500/10 border border-red-500 text-red-400 px-4 py-3 rounded-lg mb-4">
                                    {error}
                                </div>
                            )}
                            <div ref={messagesEndRef} />
                        </>
                    )}
                </div>

                {/* Suggested Questions */}
                {suggestedQuestions.length > 0 && !isLoading && (
                    <div className="px-6">
                        <SuggestedQuestions
                            questions={suggestedQuestions}
                            onQuestionClick={sendMessage}
                        />
                    </div>
                )}

                {/* Input */}
                <ChatInput
                    onSendMessage={sendMessage}
                    isLoading={isLoading}
                    placeholder="Ask about security vulnerabilities, best practices, or get help fixing issues..."
                />
            </div>
        </div>
    );
};

export default ChatAssistant;
