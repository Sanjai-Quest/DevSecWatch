import { useState, useCallback } from 'react';
import { chatService, ChatMessage, ChatResponse } from '../services/chatService';
import { v4 as uuidv4 } from 'uuid';

export const useChat = (scanId?: number, includeContext: boolean = true) => {
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [suggestedQuestions, setSuggestedQuestions] = useState<string[]>([]);

    const sendMessage = useCallback(
        async (content: string) => {
            if (!content.trim()) return;

            const userMessage: ChatMessage = {
                role: 'user',
                content: content.trim(),
                timestamp: new Date().toISOString(),
                messageId: uuidv4(),
            };

            // Add user message immediately
            setMessages((prev) => [...prev, userMessage]);
            setIsLoading(true);
            setError(null);

            try {
                const response: ChatResponse = await chatService.sendMessage({
                    message: content.trim(),
                    conversationHistory: messages,
                    scanId,
                    includeContext,
                });

                const assistantMessage: ChatMessage = {
                    role: 'assistant',
                    content: response.response,
                    timestamp: response.timestamp,
                    messageId: response.messageId,
                };

                setMessages((prev) => [...prev, assistantMessage]);
                setSuggestedQuestions(response.suggestedQuestions || []);
            } catch (err: any) {
                console.error('Chat error:', err);
                setError(err.response?.data?.error || 'Failed to get response. Please try again.');

                // Remove user message on error
                setMessages((prev) => prev.slice(0, -1));
            } finally {
                setIsLoading(false);
            }
        },
        [messages, scanId, includeContext]
    );

    const clearChat = useCallback(() => {
        setMessages([]);
        setSuggestedQuestions([]);
        setError(null);
    }, []);

    return {
        messages,
        isLoading,
        error,
        suggestedQuestions,
        sendMessage,
        clearChat,
    };
};
