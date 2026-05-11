package kr.devslab.easypaging.it;

import java.util.List;
import java.util.Map;
import kr.devslab.easypaging.annotation.KeysetPaginate;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.core.KeysetPage;
import kr.devslab.easypaging.core.KeysetRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/users")
class KeysetTestController {

    private final TestUserMapper mapper;
    private final CursorCodec codec;

    KeysetTestController(TestUserMapper mapper, CursorCodec codec) {
        this.mapper = mapper;
        this.codec = codec;
    }

    @GetMapping("/keyset")
    @KeysetPaginate(keys = {"createdAt", "id"}, direction = "DESC", defaultSize = 3, maxSize = 50)
    KeysetPage<TestUser> keyset(KeysetRequest request) {
        // mapper queries `size + 1` rows to detect the next page.
        List<TestUser> rows = mapper.findAfter(
                request.keyAsInstant("createdAt"),
                request.keyAsLong("id"));

        // Trim to the requested page size for the build() helper (it would
        // also trim internally, but doing it here keeps the SQL self-explanatory).
        int upper = Math.min(rows.size(), request.size() + 1);
        return KeysetPage.build(
                rows.subList(0, upper),
                request,
                r -> Map.of(
                        "createdAt", r.getCreatedAt().toString(),
                        "id", r.getId()),
                codec);
    }
}
