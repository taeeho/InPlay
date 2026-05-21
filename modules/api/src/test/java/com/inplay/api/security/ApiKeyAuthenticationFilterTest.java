package com.inplay.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.ingest.user.UserDocument;
import com.inplay.ingest.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock UserRepository userRepository;
    @Mock FilterChain chain;

    private final ApiKeyService apiKeyService = new ApiKeyService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private UserDocument userDoc(String userId, String hash) {
        return new UserDocument(
                "oid-1", userId, "name", hash, "HH", Map.of(), null,
                new UserDocument.MuteWindowDoc("00:00", "00:00", "Asia/Seoul"),
                Instant.now());
    }

    @Test
    void missingHeaderProceedsWithoutAuthentication() throws ServletException, IOException {
        var filter = new ApiKeyAuthenticationFilter(apiKeyService, userRepository);
        var req = new MockHttpServletRequest("GET", "/api/me");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        Mockito.verify(chain).doFilter(req, res);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Mockito.verifyNoInteractions(userRepository);
    }

    @Test
    void validKeyAuthenticatesAndSetsContext() throws ServletException, IOException {
        String raw = "raw-key-xyz";
        String hash = apiKeyService.hash(raw);
        Mockito.when(userRepository.findByApiKeyHash(hash))
                .thenReturn(Optional.of(userDoc("u_taeeho", hash)));

        var filter = new ApiKeyAuthenticationFilter(apiKeyService, userRepository);
        var req = new MockHttpServletRequest("GET", "/api/me");
        req.addHeader(ApiKeyAuthenticationFilter.HEADER, raw);
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("u_taeeho");
        assertThat(auth.getAuthorities()).extracting(Object::toString).contains("ROLE_USER");
    }

    @Test
    void unknownKeyLeavesContextEmpty() throws ServletException, IOException {
        Mockito.when(userRepository.findByApiKeyHash(Mockito.anyString())).thenReturn(Optional.empty());

        var filter = new ApiKeyAuthenticationFilter(apiKeyService, userRepository);
        var req = new MockHttpServletRequest("GET", "/api/me");
        req.addHeader(ApiKeyAuthenticationFilter.HEADER, "unknown-key");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Mockito.verify(chain).doFilter(req, res);
    }

    @Test
    void blankHeaderTreatedAsMissing() throws ServletException, IOException {
        var filter = new ApiKeyAuthenticationFilter(apiKeyService, userRepository);
        var req = new MockHttpServletRequest("GET", "/api/me");
        req.addHeader(ApiKeyAuthenticationFilter.HEADER, "  ");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        Mockito.verifyNoInteractions(userRepository);
        Mockito.verify(chain).doFilter(req, res);
    }
}
