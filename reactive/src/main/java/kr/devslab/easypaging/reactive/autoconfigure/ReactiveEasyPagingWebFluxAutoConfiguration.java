package kr.devslab.easypaging.reactive.autoconfigure;

import kr.devslab.easypaging.autoconfigure.EasyPagingAutoConfiguration;
import kr.devslab.easypaging.autoconfigure.EasyPagingProperties;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.webflux.ReactiveKeysetRequestArgumentResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.ServerWebExchange;

/**
 * Auto-registers the WebFlux-shaped {@link ReactiveKeysetRequestArgumentResolver}
 * so {@code @KeysetPaginate} handler methods can declare a
 * {@link kr.devslab.easypaging.core.KeysetRequest} parameter on a reactive
 * controller without manual wiring.
 *
 * <p>Activates only when WebFlux (i.e. {@link ServerWebExchange}) is on the
 * classpath. Servlet-only apps remain untouched.
 */
@AutoConfiguration
@AutoConfigureAfter(EasyPagingAutoConfiguration.class)
@ConditionalOnClass(ServerWebExchange.class)
@ConditionalOnProperty(prefix = "easy-paging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReactiveEasyPagingWebFluxAutoConfiguration {

    @Bean
    public ReactiveKeysetRequestArgumentResolver easyPagingReactiveKeysetRequestArgumentResolver(
            CursorCodec codec, EasyPagingProperties properties) {
        return new ReactiveKeysetRequestArgumentResolver(codec, properties);
    }

    @Bean
    public WebFluxConfigurer easyPagingReactiveKeysetWebFluxConfigurer(
            ReactiveKeysetRequestArgumentResolver resolver) {
        return new WebFluxConfigurer() {
            @Override
            public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
                configurer.addCustomResolver(resolver);
            }
        };
    }
}
