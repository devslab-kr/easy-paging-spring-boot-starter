package kr.devslab.easypaging.support;

import jakarta.servlet.http.HttpServletRequest;
import kr.devslab.easypaging.annotation.KeysetPaginate;
import kr.devslab.easypaging.autoconfigure.EasyPagingProperties;
import kr.devslab.easypaging.core.Cursor;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.core.KeysetRequest;
import org.springframework.core.MethodParameter;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link KeysetRequest} arguments for Spring MVC controllers.
 *
 * <p>Reads {@code ?cursor=...&size=...&direction=...} from the request, decodes
 * the cursor via {@link CursorCodec}, and falls back to defaults from the
 * method-level {@link KeysetPaginate} annotation.
 *
 * <p>Parameter names recognised:
 * <ul>
 *   <li>{@code cursor} — opaque token from a previous response's
 *       {@code nextCursor}; empty/missing means "first page"</li>
 *   <li>{@code size} — page size; clamped to annotation's {@code maxSize}
 *       (and globally to {@code easy-paging.max-page-size})</li>
 *   <li>{@code direction} — {@code FORWARD} (default) or {@code BACKWARD}</li>
 * </ul>
 */
public class KeysetRequestArgumentResolver implements HandlerMethodArgumentResolver {

    private final CursorCodec codec;
    private final EasyPagingProperties properties;

    public KeysetRequestArgumentResolver(CursorCodec codec, EasyPagingProperties properties) {
        this.codec = codec;
        this.properties = properties;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return KeysetRequest.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                   @Nullable ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest,
                                   @Nullable WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        KeysetPaginate annotation = parameter.getMethodAnnotation(KeysetPaginate.class);

        int defaultSize = annotation != null ? annotation.defaultSize() : properties.getDefaultPageSize();
        int annotationMax = annotation != null ? annotation.maxSize() : properties.getMaxPageSize();
        int absoluteMax = properties.getMaxPageSize();
        int upper = Math.min(annotationMax, absoluteMax);

        String cursorParam = webRequest.getParameter("cursor");
        String sizeParam = webRequest.getParameter("size");
        String directionParam = webRequest.getParameter("direction");

        Cursor cursor = codec.decodeOrEmpty(cursorParam);
        if (directionParam != null && !directionParam.isBlank()) {
            cursor = Cursor.of(cursor.keys(), Cursor.Direction.parse(directionParam));
        }

        int size;
        if (sizeParam == null || sizeParam.isBlank()) {
            size = defaultSize;
        } else {
            try {
                size = Integer.parseInt(sizeParam);
            } catch (NumberFormatException e) {
                size = defaultSize;
            }
        }
        if (size <= 0) {
            size = defaultSize;
        }
        size = Math.min(size, upper);

        return new KeysetRequest(cursor, size);
    }
}
