package kr.devslab.easypaging.autoconfigure;

import java.util.List;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.support.KeysetRequestArgumentResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.data.web.config.SortHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Servlet-MVC side configuration: registers the argument resolvers that let
 * controller methods accept {@code Pageable} / {@code Sort} parameters out
 * of the box, plus the starter's own
 * {@link KeysetRequestArgumentResolver} for keyset endpoints.
 *
 * <p>Spring Boot 4 dropped the auto-configuration that used to register
 * {@code PageableHandlerMethodArgumentResolver} for Spring MVC, and the
 * companion {@code @EnableSpringDataWebSupport} annotation is also
 * servlet-only legacy — so the starter takes ownership: any consumer that
 * pulls in {@code easy-paging-spring-boot-starter} gets the resolvers
 * registered automatically, just like they used to under SB3's
 * {@code SpringDataWebAutoConfiguration}.
 *
 * <p>Activated only when this is a Servlet web application AND a
 * {@link CursorCodec} bean is already in the context (which the main
 * {@link EasyPagingAutoConfiguration} provides when Jackson is present).
 */
@AutoConfiguration(after = EasyPagingAutoConfiguration.class)
@AutoConfigureAfter(EasyPagingAutoConfiguration.class)
@ConditionalOnClass(WebMvcConfigurer.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(CursorCodec.class)
@ConditionalOnProperty(prefix = "easy-paging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EasyPagingWebMvcConfiguration {

    @Bean
    public WebMvcConfigurer easyPagingWebMvcConfigurer(
            CursorCodec codec,
            EasyPagingProperties properties,
            ObjectProvider<SortHandlerMethodArgumentResolverCustomizer> sortCustomizers,
            ObjectProvider<PageableHandlerMethodArgumentResolverCustomizer> pageableCustomizers) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                // Order matters: Sort must register before Pageable (Pageable
                // delegates to Sort for the `?sort=` parsing).
                SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver();
                sortCustomizers.orderedStream().forEach(c -> c.customize(sortResolver));
                resolvers.add(sortResolver);

                // The pageable customizer chain is how features like
                // `easy-paging.one-indexed-pages=true` reach the resolver —
                // EasyPagingAutoConfiguration registers a customizer bean
                // that flips `setOneIndexedParameters(true)`. Apply each
                // customizer here so our resolver picks up those settings.
                PageableHandlerMethodArgumentResolver pageableResolver =
                        new PageableHandlerMethodArgumentResolver(sortResolver);
                pageableCustomizers.orderedStream().forEach(c -> c.customize(pageableResolver));
                resolvers.add(pageableResolver);

                resolvers.add(new KeysetRequestArgumentResolver(codec, properties));
            }
        };
    }
}
