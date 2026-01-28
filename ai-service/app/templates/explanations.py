TEMPLATES = {
    "SQL_INJECTION": {
        "description": "This code constructs SQL queries by concatenating user input directly into the query string. An attacker can manipulate the input to inject malicious SQL commands, potentially reading sensitive data, modifying records, or even dropping tables.",
        "fix": "Use parameterized queries with PreparedStatement. Replace string concatenation with placeholders (?), then set parameters using stmt.setString() or appropriate setters. This ensures user input is treated as data, not executable SQL."
    },
    "HARDCODED_CREDENTIALS": {
        "description": "Credentials are embedded directly in source code, making them visible to anyone with access to the codebase or decompiled bytecode. If this code is committed to version control, credentials are permanently in history even if later removed.",
        "fix": "Store credentials in environment variables or secure vaults like AWS Secrets Manager. Access them using System.getenv() in Java or @Value annotation in Spring. Never commit credentials to version control."
    },
    "PATH_TRAVERSAL": {
        "description": "User input is used to construct file paths without validation. An attacker can use sequences like '../' to navigate outside the intended directory and access sensitive files like /etc/passwd or application config files.",
        "fix": "Validate file paths against a whitelist of allowed directories. Use Path.normalize() to resolve '..' sequences, then verify the result starts with your allowed base path. Consider using UUID-based filename system instead of user input."
    },
    "CROSS_SITE_SCRIPTING": {
        "description": "User input is rendered in HTML without proper encoding, allowing attackers to inject malicious JavaScript that executes in other users' browsers. This can steal session tokens, redirect users, or deface the application.",
        "fix": "Always encode user input before rendering in HTML. Use framework-provided encoding functions like JSTL's <c:out> in JSP or Thymeleaf's default escaping. For rich text, use a sanitization library like OWASP Java HTML Sanitizer."
    },
    "XML_EXTERNAL_ENTITY": {
        "description": "XML parser is configured to process external entities, allowing attackers to read local files, perform SSRF attacks, or cause denial of service through billion laughs attack.",
        "fix": "Disable external entity processing in XML parsers. For Java: factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) and factory.setFeature('http://apache.org/xml/features/disallow-doctype-decl', true)."
    },
    "INSECURE_DESERIALIZATION": {
        "description": "Untrusted data is deserialized without validation, allowing attackers to execute arbitrary code by crafting malicious serialized objects. This is one of the most dangerous vulnerabilities.",
        "fix": "Avoid deserializing untrusted data. If necessary, use allowlisting with ObjectInputStream.setObjectInputFilter() in Java 9+, or use safer formats like JSON. Implement integrity checks on serialized data."
    },
    "COMMAND_INJECTION": {
        "description": "User input is passed to system command execution without sanitization, allowing attackers to execute arbitrary shell commands on the server.",
        "fix": "Never pass user input directly to Runtime.exec() or ProcessBuilder. If unavoidable, use strict allowlisting of allowed commands and escape all special shell characters. Better: use language-native libraries instead of shell commands."
    },
    "DEFAULT": {
        "description": "A potential security vulnerability has been detected in this code. This pattern may allow attackers to compromise the application's security.",
        "fix": "Review this code carefully with security best practices in mind. Consult OWASP guidelines for the specific vulnerability type. Consider security code review or penetration testing."
    }
}

def get_template(vulnerability_type: str) -> dict:
    return TEMPLATES.get(vulnerability_type, TEMPLATES["DEFAULT"])
