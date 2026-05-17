package kr.devslab.easypaging.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import kr.devslab.easypaging.aspect.AutoPaginateAspect;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.spi.PageResponseFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Main auto-configuration. Activates when both PageHelper and Spring Data
 * {@link Pageable} are on the classpath.
 *
 * <p>Disable globally by setting {@code easy-paging.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnClass({PageHelper.class, Pageable.class})
@ConditionalOnProperty(prefix = "easy-paging", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EasyPagingProperties.class)
public class EasyPagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AutoPaginateAspect autoPaginateAspect(EasyPagingProperties properties,
                                                  ObjectProvider<PageResponseFactory> responseFactory) {
        return new AutoPaginateAspect(properties, responseFactory.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ObjectMapper.class)
    public CursorCodec cursorCodec(EasyPagingProperties properties,
                                    ObjectProvider<ObjectMapper> objectMapper) {
        ObjectMapper mapper = objectMapper.getIfAvailable(ObjectMapper::new);
        EasyPagingProperties.Keyset keyset = properties.getKeyset();
        return new CursorCodec(mapper, keyset.getCursorSecret(), keyset.getMaxCursorBytes());
    }

    /**
     * Wires the input half of the one-indexed-pages contract: Spring Data Web's
     * {@code PageableHandlerMethodArgumentResolver} is told to interpret
     * {@code ?page=1} as the first page (translating to a 0-based {@code Pageable}
     * internally), instead of the default where {@code ?page=0} is the first page.
     *
     * <p>The output half (response {@code page} field shifted by {@code +1}) is
     * applied by {@link AutoPaginateAspect}.
     *
     * <p>Registered only when {@code easy-paging.one-indexed-pages=true}, so
     * default 0-based behavior is unchanged for consumers who don't opt in.
     */
    @Bean
    @ConditionalOnProperty(prefix = "easy-paging", name = "one-indexed-pages", havingValue = "true")
    public PageableHandlerMethodArgumentResolverCustomizer easyPagingOneIndexedPageableCustomizer() {
        return resolver -> resolver.setOneIndexedParameters(true);
    }
}
