import React from 'react';

interface SuggestedQuestionsProps {
    questions: string[];
    onQuestionClick: (question: string) => void;
}

export const SuggestedQuestions: React.FC<SuggestedQuestionsProps> = ({
    questions,
    onQuestionClick,
}) => {
    if (!questions || questions.length === 0) return null;

    return (
        <div className="mb-4">
            <p className="text-xs text-slate-400 mb-2">Suggested questions:</p>
            <div className="flex flex-wrap gap-2">
                {questions.map((question, index) => (
                    <button
                        key={index}
                        onClick={() => onQuestionClick(question)}
                        className="px-3 py-2 text-sm bg-slate-700 text-slate-200 rounded-lg hover:bg-slate-600 transition-colors border border-slate-600"
                    >
                        {question}
                    </button>
                ))}
            </div>
        </div>
    );
};
