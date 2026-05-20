-- TRUNCATE … RESTART IDENTITY resets the IDENTITY sequence so IDs are 1..N
-- on every (Spring) context startup, even though H2's mem DB outlives the
-- context (DB_CLOSE_DELAY=-1).  Without this, sequential @SpringBootTest
-- classes see IDs of 13..24, 25..36, etc.
TRUNCATE TABLE test_users RESTART IDENTITY;

INSERT INTO test_users (name, created_at) VALUES
  ('alice',   '2026-05-01 10:00:00'),
  ('bob',     '2026-05-01 10:01:00'),
  ('charlie', '2026-05-01 10:02:00'),
  ('dave',    '2026-05-01 10:03:00'),
  ('eve',     '2026-05-01 10:04:00'),
  ('frank',   '2026-05-01 10:05:00'),
  ('grace',   '2026-05-01 10:06:00'),
  ('heidi',   '2026-05-01 10:07:00'),
  ('ivan',    '2026-05-01 10:08:00'),
  ('judy',    '2026-05-01 10:09:00'),
  ('mallory', '2026-05-01 10:10:00'),
  ('niaj',    '2026-05-01 10:11:00');
