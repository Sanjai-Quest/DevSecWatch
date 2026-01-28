import api from './api';

export interface ChatMessage {
    role: 'user' | 'assistant';
    content: string;
    timestamp: string;
    messageId?: string;
}

export interface ChatRequest {
    message: string;
    conversationHistory: ChatMessage[];
    scanId?: number;
    includeContext?: boolean;
}

export interface ChatResponse {
    response: string;
    messageId: string;
    timestamp: string;
    suggestedQuestions: string[];
    model: string;
}

export const chatService = {
    sendMessage: async (request: ChatRequest): Promise<ChatResponse> => {
        const response = await api.post('/api/chat/message', request);
        return response.data;
    },

    healthCheck: async (): Promise<boolean> => {
        try {
            await api.get('/api/chat/health');
            return true;
        } catch {
            return false;
        }
    },
};
