export enum ScanStatus {
    QUEUED = 'QUEUED',
    PROCESSING = 'PROCESSING',
    COMPLETED = 'COMPLETED',
    FAILED = 'FAILED'
}

export enum Severity {
    CRITICAL = 'CRITICAL',
    HIGH = 'HIGH',
    MEDIUM = 'MEDIUM',
    LOW = 'LOW'
}

export enum ConfidenceLevel {
    HIGH = 'HIGH',
    MEDIUM = 'MEDIUM',
    LOW = 'LOW'
}

export interface User {
    id: number;
    username: string;
    email: string;
}

export interface AuthTokens {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
}

export interface Scan {
    id: number;
    repoUrl: string;
    branch: string;
    status: ScanStatus;
    totalFiles: number;
    totalVulnerabilities: number;
    criticalCount: number;
    highCount: number;
    mediumCount: number;
    lowCount: number;
    createdAt: string;
    completedAt?: string;
    errorMessage?: string;
}

export interface Vulnerability {
    id: number;
    filePath: string;
    lineNumber: number;
    vulnerabilityType: string;
    severity: Severity;
    confidence: ConfidenceLevel;
    description: string;
    codeSnippet: string;
    fixSuggestion: string;
    cveId?: string;
    isTemplateExplanation: boolean;
}

export interface ScanWithVulnerabilities extends Scan {
    vulnerabilities: Vulnerability[];
}

export interface ScanRequest {
    repoUrl: string;
    branch?: string;
}

export type ScanResponse = Scan;

export interface PaginatedResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
    empty: boolean;
}
