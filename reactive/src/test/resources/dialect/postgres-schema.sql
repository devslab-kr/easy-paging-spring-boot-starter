-- PostgreSQL schema + seed for the reactive dialect-compat tests. Uses
-- TIMESTAMP WITH TIME ZONE (PostgreSQL's "timestamptz") so the R2DBC driver
-- has to roundtrip Instants through the OffsetDateTime <-> Instant boundary
-- — that's the failure mode H2 didn't catch and that we want explicit
-- coverage for.
CREATE TABLE IF NOT EXISTS events (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

INSERT INTO events (id, name, created_at) VALUES (1,  'event-01', TIMESTAMP '2026-01-01 00:00:00+00');
INSERT INTO events (id, name, created_at) VALUES (2,  'event-02', TIMESTAMP '2026-01-02 00:00:00+00');
INSERT INTO events (id, name, created_at) VALUES (3,  'event-03', TIMESTAMP '2026-01-03 00:00:00+00');
INSERT INTO events (id, name, created_at) VALUES (4,  'event-04', TIMESTAMP '2026-01-04 00:00:00+00');
INSERT INTO events (id, name, created_at) VALUES (5,  'event-05', TIMESTAMP '2026-01-05 00:00:00+00');
INSERT INTO events (id, name, created_at) VALUES (6,  'event-06', TIMESTAMP '2026-01-06 00:00:00+00');
INSERT INTO events (id, name, created_at) VALUES (7,  'event-07', TIMESTAMP '2026-01-07 00:00:00+00');
INSERT INTO events (id, name, created_at) VALUES (8,  'event-08', TIMESTAMP '2026-01-08 00:00:00+00');
INSERT INTO events (id, name, created_at) VALUES (9,  'event-09', TIMESTAMP '2026-01-09 00:00:00+00');
INSERT INTO events (id, name, created_at) VALUES (10, 'event-10', TIMESTAMP '2026-01-10 00:00:00+00');
