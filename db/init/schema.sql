-- Schema initialization
-- WARNING: This is executed only on first container initialization (empty data dir)

-- Drop existing tables if re-running manually
DROP TABLE IF EXISTS members;

CREATE TABLE members (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(255) UNIQUE
);
