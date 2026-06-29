package com.devsecwatch.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class RepoUrlValidator implements ConstraintValidator<ValidRepoUrl, String> {

    private static final List<String> ALLOWED_HOSTS = Arrays.asList(
            "github.com", "gitlab.com", "bitbucket.org",
            "www.github.com", "www.gitlab.com"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            String scheme = uri.getScheme();

            // Scheme check
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                setCustomMessage(context, "Invalid scheme. Only http and https are allowed.");
                return false;
            }

            // Host allowlist check
            if (host == null || !ALLOWED_HOSTS.contains(host.toLowerCase())) {
                setCustomMessage(context, "Repository URL must be from github.com, gitlab.com, or bitbucket.org");
                return false;
            }

            // DNS Resolution and IP check
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isInternalOrLoopback(address) || host.equalsIgnoreCase("localhost")) {
                    setCustomMessage(context, "Resolved IP points to an internal or loopback address.");
                    return false;
                }
            }

            return true;
        } catch (UnknownHostException e) {
            setCustomMessage(context, "Unresolvable host.");
            return false;
        } catch (Exception e) {
            setCustomMessage(context, "Invalid URL format.");
            return false;
        }
    }

    private boolean isInternalOrLoopback(InetAddress address) {
        if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
            return true;
        }
        
        byte[] ip = address.getAddress();
        if (ip.length == 4) { // IPv4
            int firstOctet = ip[0] & 0xFF;
            int secondOctet = ip[1] & 0xFF;
            
            if (firstOctet == 10) return true;
            if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) return true;
            if (firstOctet == 192 && secondOctet == 168) return true;
            if (firstOctet == 169 && secondOctet == 254) return true;
            if (firstOctet == 127) return true;
        }
        return false;
    }

    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
