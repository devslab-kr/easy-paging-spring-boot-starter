package kr.devslab.easypaging.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for {@code easy-paging}. All values can be overridden in
 * {@code application.yml} under the {@code easy-paging} prefix.
 *
 * <pre>{@code
 * easy-paging:
 *   default-page-size: 20
 *   max-page-size: 100
 *   keyset:
 *     cursor-secret: "${EASY_PAGING_CURSOR_SECRET:}"  # optional HMAC for cursor signing
 * }</pre>
 */
@ConfigurationProperties(prefix = "easy-paging")
public class EasyPagingProperties {

    /** Default page size when caller omits {@code size} query parameter. */
    private int defaultPageSize = 20;

    /** Absolute upper bound; per-annotation {@code maxSize} cannot exceed this. */
    private int maxPageSize = 500;

    /** Whether the aspect should auto-wrap returned {@code List} values into {@code PageResponse}. */
    private boolean autoWrapList = true;

    private final Keyset keyset = new Keyset();

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public boolean isAutoWrapList() {
        return autoWrapList;
    }

    public void setAutoWrapList(boolean autoWrapList) {
        this.autoWrapList = autoWrapList;
    }

    public Keyset getKeyset() {
        return keyset;
    }

    /** Keyset (cursor) pagination settings. */
    public static class Keyset {

        /**
         * HMAC-SHA256 secret used to sign cursor tokens. If left empty the cursor
         * is still Base64-encoded but unsigned; clients can forge cursors. Set in
         * production via {@code EASY_PAGING_CURSOR_SECRET} env var.
         */
        private String cursorSecret = "";

        /** Maximum decoded cursor payload size in bytes (anti-DoS). */
        private int maxCursorBytes = 2048;

        public String getCursorSecret() {
            return cursorSecret;
        }

        public void setCursorSecret(String cursorSecret) {
            this.cursorSecret = cursorSecret;
        }

        public int getMaxCursorBytes() {
            return maxCursorBytes;
        }

        public void setMaxCursorBytes(int maxCursorBytes) {
            this.maxCursorBytes = maxCursorBytes;
        }
    }
}
