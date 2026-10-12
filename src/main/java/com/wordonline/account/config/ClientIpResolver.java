package com.wordonline.account.config;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * Finds the address a sign-up request came from, for the rate limit and the Turnstile remoteip.
 *
 * <p>The account server is reachable only through Nginx Proxy Manager, which overwrites
 * X-Real-IP with the real peer address on every request. That header is therefore trusted.
 * X-Forwarded-For is deliberately not read: the proxy appends to a value the client sent
 * ($proxy_add_x_forwarded_for), so its leftmost entry is client-controlled and would let a
 * caller dodge the limit. Without a valid X-Real-IP the socket's remote address is used.
 */
@Component
public class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F:.]+");

    private final String headerName;

    public ClientIpResolver(SignupRateLimitProperties properties) {
        this.headerName = properties.clientIpHeader();
    }

    public String resolve(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(headerName);
        if (header != null) {
            String candidate = header.trim();
            String literal = asIpLiteral(candidate);
            if (literal != null) {
                return literal;
            }
        }
        InetSocketAddress address = request.getRemoteAddress();
        if (address == null || address.getAddress() == null) {
            return "unknown";
        }
        return address.getAddress().getHostAddress();
    }

    // Only literals reach InetAddress, so no DNS lookup can happen here.
    private static String asIpLiteral(String candidate) {
        boolean v4 = IPV4.matcher(candidate).matches();
        boolean v6 = candidate.indexOf(':') >= 0 && IPV6.matcher(candidate).matches();
        if (!v4 && !v6) {
            return null;
        }
        try {
            return InetAddress.getByName(candidate).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
