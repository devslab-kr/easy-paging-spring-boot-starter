package kr.devslab.easypaging.core;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Resolved keyset-pagination request. Produced by the argument resolver from
 * the {@code cursor} / {@code size} / {@code direction} query parameters and
 * the annotation's defaults.
 */
public record KeysetRequest(Cursor cursor, int size) {

    public KeysetRequest {
        Objects.requireNonNull(cursor, "cursor");
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive: " + size);
        }
    }

    public boolean isFirstPage() {
        return cursor.isEmpty();
    }

    public Cursor.Direction direction() {
        return cursor.direction();
    }

    public Map<String, Object> keys() {
        return cursor.keys();
    }

    /** Returns the cursor key as-is, or {@code null} if not present. */
    public Object key(String name) {
        return cursor.keys().get(name);
    }

    public Long keyAsLong(String name) {
        Object v = key(name);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    public Integer keyAsInt(String name) {
        Object v = key(name);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }

    public String keyAsString(String name) {
        Object v = key(name);
        return v == null ? null : v.toString();
    }

    /** Parses an ISO-8601 string or millis epoch into {@link Instant}. */
    public Instant keyAsInstant(String name) {
        Object v = key(name);
        if (v == null) return null;
        if (v instanceof Number n) return Instant.ofEpochMilli(n.longValue());
        return Instant.parse(v.toString());
    }
}
