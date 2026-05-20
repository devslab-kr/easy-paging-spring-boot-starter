package kr.devslab.easypaging.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Opaque-from-the-client side representation of a keyset cursor.
 *
 * <p>Internally a small ordered map of column → last-seen value plus the scan
 * direction. Encoded into a URL-safe token by {@link CursorCodec}.
 *
 * <p>The empty cursor represents "start of stream" (first page).
 */
public record Cursor(Map<String, Object> keys, Direction direction) {

    public enum Direction {
        FORWARD, BACKWARD;

        public static Direction parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return FORWARD;
            }
            return switch (raw.trim().toUpperCase()) {
                case "FORWARD", "NEXT", "F" -> FORWARD;
                case "BACKWARD", "PREV", "PREVIOUS", "B" -> BACKWARD;
                default -> throw new IllegalArgumentException("Unknown cursor direction: " + raw);
            };
        }
    }

    public Cursor {
        Objects.requireNonNull(direction, "direction");
        keys = keys == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(keys));
    }

    public static Cursor empty() {
        return new Cursor(Map.of(), Direction.FORWARD);
    }

    public static Cursor of(Map<String, Object> keys, Direction direction) {
        return new Cursor(keys, direction);
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }
}
