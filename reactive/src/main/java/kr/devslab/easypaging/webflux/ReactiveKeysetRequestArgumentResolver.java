package kr.devslab.easypaging.webflux;

import kr.devslab.easypaging.annotation.KeysetPaginate;
import kr.devslab.easypaging.autoconfigure.EasyPagingProperties;
import kr.devslab.easypaging.core.Cursor;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.core.KeysetRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * WebFlux equivalent of {@code KeysetRequestArgumentResolver}.
 *
 * <p>Resolves a {@link KeysetRequest} parameter for a {@code @KeysetPaginate}-
 * annotated WebFlux handler by reading the same query parameters as the servlet
 * resolver: {@code cursor}, {@code size}, {@code direction}. The behavior is
 * byte-identical to the servlet side — only the wiring differs because WebFlux's
 * {@link HandlerMethodArgumentResolver} interface returns a {@link Mono} instead
 * of a plain object.
 *
 * <p>Registered automatically by
 * {@code ReactiveEasyPagingWebFluxAutoConfiguration} when both {@code spring-
 * webflux} and {@code reactor-core} are on the classpath.
 */
public class ReactiveKeysetRequestArgumentResolver implements HandlerMethodArgumentResolver {

    private final CursorCodec codec;
    private final EasyPagingProperties properties;

    public ReactiveKeysetRequestArgumentResolver(CursorCodec codec, EasyPagingProperties properties) {
        this.codec = codec;
        this.properties = properties;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return KeysetRequest.class.equals(parameter.getParameterType());
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter,
                                         BindingContext bindingContext,
                                         ServerWebExchange exchange) {
        KeysetPaginate annotation = parameter.getMethodAnnotation(KeysetPaginate.class);

        int defaultSize = annotation != null ? annotation.defaultSize() : properties.getDefaultPageSize();
        int annotationMax = annotation != null ? annotation.maxSize() : properties.getMaxPageSize();
        int absoluteMax = properties.getMaxPageSize();
        int upper = Math.min(annotationMax, absoluteMax);

        String cursorParam = exchange.getRequest().getQueryParams().getFirst("cursor");
        String sizeParam = exchange.getRequest().getQueryParams().getFirst("size");
        String directionParam = exchange.getRequest().getQueryParams().getFirst("direction");

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

        return Mono.just(new KeysetRequest(cursor, size));
    }
}
