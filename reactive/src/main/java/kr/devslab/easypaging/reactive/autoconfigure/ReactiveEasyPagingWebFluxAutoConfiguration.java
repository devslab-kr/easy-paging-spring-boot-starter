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
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.ServerWebExchange;

/**
 * WebFlux-side configuration. Registers two families of argument resolvers:
 *
 * <ol>
 *   <li>Spring Data Web's {@link ReactivePageableHandlerMethodArgumentResolver}
 *       and {@link ReactiveSortHandlerMethodArgumentResolver}, so reactive
 *       controllers can declare {@code Pageable} / {@code Sort} parameters
 *       without manual wiring. Spring Boot never auto-configured these for
 *       WebFlux (the SB3 {@code SpringDataWebAutoConfiguration} was
 *       servlet-only); registering them here means consumers no longer need
 *       their own {@code WebFluxConfigurer} bean to get pagination on
 *       reactive endpoints.</li>
 *   <li>The starter's own {@link ReactiveKeysetRequestArgumentResolver} for
 *       {@code @KeysetPaginate} handler methods that declare a
 *       {@link kr.devslab.easypaging.core.KeysetRequest} parameter.</li>
 * </ol>
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
    public WebFluxConfigurer easyPagingReactiveWebFluxConfigurer(
            ReactiveKeysetRequestArgumentResolver keysetResolver) {
        return new WebFluxConfigurer() {
            @Override
            public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
                // Sort before Pageable — Pageable's parsing delegates to Sort.
                configurer.addCustomResolver(new ReactiveSortHandlerMethodArgumentResolver());
                configurer.addCustomResolver(new ReactivePageableHandlerMethodArgumentResolver());
                configurer.addCustomResolver(keysetResolver);
            }
        };
    }
}
