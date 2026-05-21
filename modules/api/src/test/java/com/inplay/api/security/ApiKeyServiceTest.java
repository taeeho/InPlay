package com.inplay.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ApiKeyServiceTest {

    private final ApiKeyService service = new ApiKeyService();

    @Test
    void generateRawKeyProducesUrlSafeBase64() {
        String key = service.generateRawKey();
        assertThat(key).matches("[A-Za-z0-9_-]+");
        assertThat(key.length()).isBetween(40, 48); // 32 bytes → 43 chars
    }

    @Test
    void hashIs64CharHex() {
        String hash = service.hash("some-raw-key");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void hashIsDeterministic() {
        String h1 = service.hash("same-key");
        String h2 = service.hash("same-key");
        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void hashDiffersForDifferentInputs() {
        assertThat(service.hash("a")).isNotEqualTo(service.hash("b"));
    }

    @Test
    void verifyMatchesCorrectKey() {
        String raw = service.generateRawKey();
        String hash = service.hash(raw);
        assertThat(service.verify(raw, hash)).isTrue();
    }

    @Test
    void verifyRejectsWrongKey() {
        String hash = service.hash("correct");
        assertThat(service.verify("wrong", hash)).isFalse();
    }

    @Test
    void verifyRejectsNullsAndBadHash() {
        assertThat(service.verify(null, "x".repeat(64))).isFalse();
        assertThat(service.verify("k", null)).isFalse();
        assertThat(service.verify("k", "tooShort")).isFalse();
    }

    @Test
    void rejectsBlankRawOnHash() {
        assertThatThrownBy(() -> service.hash(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
