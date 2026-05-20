package kr.devslab.easypaging.reactive.it;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import kr.devslab.easypaging.annotation.KeysetPaginate;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.core.KeysetPage;
import kr.devslab.easypaging.core.KeysetRequest;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport.KeyColumn;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport.SortDirection;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Test WebFlux controller used by {@link ReactiveKeysetWebFluxIntegrationTest}.
 * Exercises both the WebFlux argument resolver and {@code R2dbcKeysetSupport}
 * in a single endpoint, which is the production-shaped composition.
 */
@RestController
@RequestMapping("/test/events")
class ReactiveKeysetTestController {

    private static final List<KeyColumn> KEYS = List.of(
            new KeyColumn("created_at", "createdAt", Instant.class, SortDirection.DESC),
            new KeyColumn("id", "id", Long.class, SortDirection.DESC));

    private final R2dbcEntityTemplate template;
    private final CursorCodec codec;

    ReactiveKeysetTestController(R2dbcEntityTemplate template, CursorCodec codec) {
        this.template = template;
        this.codec = codec;
    }

    @GetMapping("/keyset")
    @KeysetPaginate(keys = {"createdAt", "id"}, direction = "DESC", defaultSize = 3, maxSize = 20)
    Mono<KeysetPage<TestEvent>> keyset(KeysetRequest request) {
        return R2dbcKeysetSupport.paginate(
                template,
                TestEvent.class,
                Criteria.empty(),
                KEYS,
                request,
                e -> Map.of("createdAt", e.getCreatedAt(), "id", e.getId()),
                codec);
    }
}
