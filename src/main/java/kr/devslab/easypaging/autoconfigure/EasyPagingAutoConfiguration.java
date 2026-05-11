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
}
