package kr.devslab.easypaging.it;

import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin HTTP wrapper around {@link TestUserService} so the MockMvc-based
 * integration tests can exercise the aspect through a real
 * {@code DispatcherServlet} pipeline (argument resolution, sort parsing,
 * exception → status mapping, etc.).
 */
@RestController
@RequestMapping("/test/auto")
class AutoPaginateTestController {

    private final TestUserService service;

    AutoPaginateTestController(TestUserService service) {
        this.service = service;
    }

    @GetMapping("/users")
    PageResponse<TestUser> list(Pageable pageable) {
        return service.list(pageable);
    }
}
