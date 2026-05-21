package com.inplay.api.security;

import com.inplay.ingest.user.UserDocument;
import com.inplay.ingest.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code X-Api-Key} 헤더 기반 인증.
 *
 * <p>헤더 없으면 다음 필터로 패스 (anonymous). 헤더 있으면 sha256 hash 후 user 컬렉션 lookup.
 * 미매치는 SecurityContext 비워두고 다음 필터로 — 401 처리는 SecurityFilterChain의
 * exceptionHandling이 담당.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Api-Key";

    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService, UserRepository userRepository) {
        this.apiKeyService = Objects.requireNonNull(apiKeyService);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String rawKey = request.getHeader(HEADER);
        if (rawKey != null && !rawKey.isBlank()) {
            String hash = apiKeyService.hash(rawKey);
            Optional<UserDocument> found = userRepository.findByApiKeyHash(hash);
            if (found.isPresent()) {
                var auth = new UsernamePasswordAuthenticationToken(
                        found.get().userId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
