import React from 'react';

export const TypingIndicator: React.FC = () => {
    return (
        <div className="flex justify-start mb-4">
            <div className="bg-slate-700 rounded-lg px-4 py-3">
                <div className="flex space-x-2">
                    <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce"></div>
                    <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                    <div className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></div>
                </div>
            </div>
        </div>
    );
};
