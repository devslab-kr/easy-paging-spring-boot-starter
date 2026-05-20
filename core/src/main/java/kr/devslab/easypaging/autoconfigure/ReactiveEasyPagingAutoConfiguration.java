package kr.devslab.easypaging.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import reactor.core.publisher.Mono;

/**
 * Reactive (Project Reactor) side configuration.
 *
 * <p>Currently a marker that activates only when {@code reactor-core} is on
 * the classpath. {@link kr.devslab.easypaging.reactive.ReactivePagingSupport}
 * is a stateless utility class — no beans are required.
 *
 * <p>If a future release introduces stateful reactive components (e.g. a
 * cursor-aware {@code WebFlux} argument resolver), they will be registered here.
 */
@AutoConfiguration
@AutoConfigureAfter(EasyPagingAutoConfiguration.class)
@ConditionalOnClass(Mono.class)
@ConditionalOnProperty(prefix = "easy-paging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReactiveEasyPagingAutoConfiguration {
}
