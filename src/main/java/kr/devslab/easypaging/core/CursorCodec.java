package kr.devslab.easypaging.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes/decodes cursor tokens.
 *
 * <p>Wire format: {@code Base64Url( JSON(payload) ) [ "." Base64Url( HMAC-SHA256 ) ]}
 *
 * <p>When a non-empty signing secret is configured the HMAC suffix is required
 * on decode — this prevents clients from forging cursors that target rows they
 * shouldn't see (e.g. via tenant key tampering). When no secret is set, the
 * token is still Base64-encoded but accepted unsigned, which is fine for
 * internal/dev use but should NOT be used in multi-tenant production.
 */
public class CursorCodec {

    private static final Logger log = LoggerFactory.getLogger(CursorCodec.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final int maxBytes;
    private final Base64.Encoder b64Encoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder b64Decoder = Base64.getUrlDecoder();

    public CursorCodec(ObjectMapper objectMapper, String secret, int maxBytes) {
        this.objectMapper = objectMapper;
        this.secret = (secret == null || secret.isEmpty())
                ? new byte[0]
                : secret.getBytes(StandardCharsets.UTF_8);
        this.maxBytes = maxBytes;
    }

    public String encode(Cursor cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return "";
        }
        try {
            Map<String, Object> payload = Map.of(
                    "k", cursor.keys(),
                    "d", cursor.direction().name());
            byte[] json = objectMapper.writeValueAsBytes(payload);
            String body = b64Encoder.encodeToString(json);
            if (secret.length == 0) {
                return body;
            }
            String sig = b64Encoder.encodeToString(hmac(json));
            return body + "." + sig;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode cursor: " + e.getMessage(), e);
        }
    }

    public Cursor decode(String token) {
        if (token == null || token.isBlank()) {
            return Cursor.empty();
        }
        if (token.length() > maxBytes * 2) {
            throw new IllegalArgumentException("Cursor exceeds maximum length");
        }

        String body;
        String signature;
        int dot = token.indexOf('.');
        if (dot >= 0) {
            body = token.substring(0, dot);
            signature = token.substring(dot + 1);
        } else {
            body = token;
            signature = null;
        }

        byte[] json;
        try {
            json = b64Decoder.decode(body);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cursor is not valid Base64-URL", e);
        }
        if (json.length > maxBytes) {
            throw new IllegalArgumentException("Cursor payload exceeds maximum size");
        }

        if (secret.length > 0) {
            if (signature == null) {
                throw new IllegalArgumentException("Cursor signature missing");
            }
            byte[] expected = hmac(json);
            byte[] provided;
            try {
                provided = b64Decoder.decode(signature);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Cursor signature is not valid Base64-URL", e);
            }
            if (!MessageDigest.isEqual(expected, provided)) {
                throw new IllegalArgumentException("Cursor signature does not verify");
            }
        } else if (signature != null) {
            // Token claims to be signed but we have no secret. Reject — don't silently accept.
            log.warn("Received signed cursor but no secret is configured; rejecting");
            throw new IllegalArgumentException("Cursor is signed but verification is disabled");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(json, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> keys = (Map<String, Object>) payload.getOrDefault("k", Map.of());
            String direction = String.valueOf(payload.getOrDefault("d", "FORWARD"));
            return Cursor.of(keys, Cursor.Direction.parse(direction));
        } catch (Exception e) {
            throw new IllegalArgumentException("Cursor payload is not valid JSON: " + e.getMessage(), e);
        }
    }

    /** Returns {@link Cursor#empty()} for {@code null}/blank/invalid input. */
    public Cursor decodeOrEmpty(String token) {
        if (token == null || token.isBlank()) {
            return Cursor.empty();
        }
        try {
            return decode(token);
        } catch (IllegalArgumentException e) {
            log.debug("Cursor decode failed, treating as empty: {}", e.getMessage());
            return Cursor.empty();
        }
    }

    private byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC initialization failed", e);
        }
    }
}
