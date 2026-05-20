package kr.devslab.easypaging.it;

import java.util.List;
import kr.devslab.easypaging.annotation.AutoPaginate;
import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TestUserService {

    private final TestUserMapper mapper;

    public TestUserService(TestUserMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Recommended pattern: declared {@link PageResponse} return; method body
     * explicitly wraps the mapper output via {@link PageResponse#from}.
     */
    @AutoPaginate(maxSize = 50)
    public PageResponse<TestUser> list(Pageable pageable) {
        return PageResponse.from(mapper.findAll(), pageable);
    }

    @AutoPaginate(maxSize = 50, count = false)
    public PageResponse<TestUser> listWithoutCount(Pageable pageable) {
        return PageResponse.from(mapper.findAll(), pageable);
    }

    /** Pass-through: declared {@link List} return — aspect does NOT wrap. */
    @AutoPaginate(maxSize = 50)
    public List<TestUser> listAsList(Pageable pageable) {
        return mapper.findAll();
    }

    /** Auto-wrap path: declared {@link Object} return lets the aspect wrap. */
    @AutoPaginate(maxSize = 50)
    public Object listAsObject(Pageable pageable) {
        return mapper.findAll();
    }
}
