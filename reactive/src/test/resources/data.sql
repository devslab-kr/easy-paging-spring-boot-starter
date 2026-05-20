-- 10 events, ascending ids, monotonically increasing timestamps.
-- Cursor tests rely on the natural ordering to verify forward/backward scans.
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
