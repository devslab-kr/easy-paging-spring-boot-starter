package kr.devslab.easypaging.it;

import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TestUserMapper {

    @Select("SELECT id, name, created_at AS createdAt FROM test_users")
    List<TestUser> findAll();

    @Select("""
            SELECT id, name, created_at AS createdAt
            FROM test_users
            WHERE (#{createdAt} IS NULL OR created_at < #{createdAt})
               OR (created_at = #{createdAt} AND id < #{id})
            ORDER BY created_at DESC, id DESC
            """)
    List<TestUser> findAfter(
            @Param("createdAt") Instant createdAt,
            @Param("id") Long id);
}
