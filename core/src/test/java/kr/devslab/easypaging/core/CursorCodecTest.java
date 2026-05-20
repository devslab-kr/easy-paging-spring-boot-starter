package kr.devslab.easypaging.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CursorCodecTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void emptyCursorRoundtripsToEmptyToken() {
        CursorCodec codec = new CursorCodec(mapper, "", 2048);
        assertThat(codec.encode(Cursor.empty())).isEmpty();
        assertThat(codec.decode("")).isEqualTo(Cursor.empty());
        assertThat(codec.decode(null)).isEqualTo(Cursor.empty());
    }

    @Test
    void roundtripUnsigned() {
        CursorCodec codec = new CursorCodec(mapper, "", 2048);
        Cursor original = Cursor.of(
                Map.of("time", "2026-05-01T00:00:00Z", "id", 42),
                Cursor.Direction.FORWARD);

        String token = codec.encode(original);
        Cursor decoded = codec.decode(token);

        assertThat(decoded.direction()).isEqualTo(Cursor.Direction.FORWARD);
        assertThat(decoded.keys())
                .containsEntry("time", "2026-05-01T00:00:00Z")
                .containsEntry("id", 42);
    }

    @Test
    void roundtripSigned() {
        CursorCodec codec = new CursorCodec(mapper, "test-secret", 2048);
        Cursor original = Cursor.of(Map.of("id", 99L), Cursor.Direction.BACKWARD);

        String token = codec.encode(original);
        assertThat(token).contains(".");

        Cursor decoded = codec.decode(token);
        assertThat(decoded.direction()).isEqualTo(Cursor.Direction.BACKWARD);
        assertThat(decoded.keys()).containsEntry("id", 99);
    }

    @Test
    void tamperedSignatureRejected() {
        CursorCodec codec = new CursorCodec(mapper, "test-secret", 2048);
        Cursor original = Cursor.of(Map.of("id", 1), Cursor.Direction.FORWARD);
        String token = codec.encode(original);

        // Flip the last char of the signature
        String tampered = token.substring(0, token.length() - 1)
                + (token.charAt(token.length() - 1) == 'A' ? 'B' : 'A');

        assertThatThrownBy(() -> codec.decode(tampered))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void signedTokenRejectedWhenSecretMissing() {
        CursorCodec signing = new CursorCodec(mapper, "test-secret", 2048);
        CursorCodec verifying = new CursorCodec(mapper, "", 2048);
        String token = signing.encode(Cursor.of(Map.of("id", 1), Cursor.Direction.FORWARD));

        assertThatThrownBy(() -> verifying.decode(token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unsignedTokenRejectedWhenSecretRequired() {
        CursorCodec signing = new CursorCodec(mapper, "", 2048);
        CursorCodec verifying = new CursorCodec(mapper, "test-secret", 2048);
        String token = signing.encode(Cursor.of(Map.of("id", 1), Cursor.Direction.FORWARD));

        assertThatThrownBy(() -> verifying.decode(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void garbageTokenFromDecodeOrEmptyFallsBackToEmpty() {
        CursorCodec codec = new CursorCodec(mapper, "secret", 2048);
        assertThat(codec.decodeOrEmpty("!!!not-a-real-cursor!!!"))
                .isEqualTo(Cursor.empty());
    }

    @Test
    void payloadSizeLimitEnforced() {
        CursorCodec codec = new CursorCodec(mapper, "", 16); // tiny limit
        Cursor large = Cursor.of(
                Map.of("k", "a".repeat(100)),
                Cursor.Direction.FORWARD);
        String token = codec.encode(large);
        assertThatThrownBy(() -> codec.decode(token))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
