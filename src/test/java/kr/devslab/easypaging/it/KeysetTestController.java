package kr.devslab.easypaging.it;

import java.util.List;
import java.util.Map;
import kr.devslab.easypaging.annotation.KeysetPaginate;
import kr.devslab.easypaging.core.Cursor;
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
        // Dispatch to the right mapper query based on scan direction.
        // BACKWARD scans flip both the WHERE comparison and the ORDER BY.
        List<TestUser> rows = (request.direction() == Cursor.Direction.BACKWARD)
                ? mapper.findBefore(request.keyAsInstant("createdAt"), request.keyAsLong("id"))
                : mapper.findAfter(request.keyAsInstant("createdAt"), request.keyAsLong("id"));

        // Trim to size+1 for the +1 trick (the mapper currently returns all
        // rows; in real code you'd LIMIT in SQL).
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
