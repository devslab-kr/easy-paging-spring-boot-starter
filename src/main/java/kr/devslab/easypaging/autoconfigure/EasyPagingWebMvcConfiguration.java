package kr.devslab.easypaging.autoconfigure;

import java.util.List;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.support.KeysetRequestArgumentResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Servlet-MVC side configuration: registers the
 * {@link KeysetRequestArgumentResolver} that injects
 * {@link kr.devslab.easypaging.core.KeysetRequest} into controller methods.
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
    public WebMvcConfigurer easyPagingWebMvcConfigurer(CursorCodec codec, EasyPagingProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new KeysetRequestArgumentResolver(codec, properties));
            }
        };
    }
}
