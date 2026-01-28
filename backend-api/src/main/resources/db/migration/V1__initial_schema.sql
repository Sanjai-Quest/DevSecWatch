CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

CREATE TABLE scans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    repo_url VARCHAR(500) NOT NULL,
    branch VARCHAR(100) DEFAULT 'main',
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    total_files INT DEFAULT 0,
    lines_of_code INT DEFAULT 0,
    total_vulnerabilities INT DEFAULT 0,
    critical_count INT DEFAULT 0,
    high_count INT DEFAULT 0,
    medium_count INT DEFAULT 0,
    low_count INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_scans_user_id ON scans(user_id);
CREATE INDEX idx_scans_status ON scans(status);
CREATE INDEX idx_scans_created_at ON scans(created_at DESC);

CREATE TABLE vulnerabilities (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT REFERENCES scans(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    line_number INT NOT NULL,
    vulnerability_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    confidence VARCHAR(20) NOT NULL CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    description TEXT NOT NULL,
    code_snippet TEXT NOT NULL,
    fix_suggestion TEXT NOT NULL,
    cve_id VARCHAR(50),
    is_template_explanation BOOLEAN DEFAULT FALSE,
    semgrep_rule_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vulns_scan_id ON vulnerabilities(scan_id);
CREATE INDEX idx_vulns_severity ON vulnerabilities(severity);
CREATE INDEX idx_vulns_confidence ON vulnerabilities(confidence);

CREATE TABLE scan_metrics (
    id BIGSERIAL PRIMARY KEY,
    scan_id BIGINT REFERENCES scans(id) ON DELETE CASCADE,
    files_scanned INT NOT NULL,
    lines_of_code INT NOT NULL,
    git_clone_duration_ms BIGINT,
    semgrep_duration_ms BIGINT,
    ai_call_duration_ms BIGINT,
    total_duration_ms BIGINT NOT NULL,
    cache_hit_rate DECIMAL(3,2),
    api_calls_made INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_metrics_scan_id ON scan_metrics(scan_id);
