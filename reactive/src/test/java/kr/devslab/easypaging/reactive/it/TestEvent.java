package kr.devslab.easypaging.reactive.it;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Test entity for R2DBC integration tests. Schema lives in
 * {@code src/test/resources/schema.sql}, seed data in {@code data.sql}.
 */
@Table("events")
public class TestEvent {

    @Id
    private Long id;
    private String name;
    @Column("created_at")
    private Instant createdAt;

    public TestEvent() {}

    public TestEvent(Long id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
