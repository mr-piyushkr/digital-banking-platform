-- BankFlow — one-time local database setup.
-- Run once as root:
--   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < db\init\01_create_database_and_user.sql
-- or open this file in MySQL Workbench (root connection) and execute it.
--
-- The application never connects as root. It uses bankflow_app, which is
-- granted privileges on the two BankFlow schemas and nothing else.
-- The password below must match DB_PASSWORD in .env (gitignored).

CREATE DATABASE IF NOT EXISTS bankflow
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS bankflow_test
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'bankflow_app'@'localhost'
  IDENTIFIED WITH caching_sha2_password BY '7i8yLjKd5AEBSEqKjWNWN4RW';

-- Schema-scoped only. No GRANT ALL ON *.*, no SUPER, no FILE.
GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, DROP, ALTER, INDEX, REFERENCES,
      CREATE VIEW, SHOW VIEW,
      EXECUTE, CREATE ROUTINE, ALTER ROUTINE,
      CREATE TEMPORARY TABLES, LOCK TABLES
  ON bankflow.* TO 'bankflow_app'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, DROP, ALTER, INDEX, REFERENCES,
      CREATE VIEW, SHOW VIEW,
      EXECUTE, CREATE ROUTINE, ALTER ROUTINE,
      CREATE TEMPORARY TABLES, LOCK TABLES
  ON bankflow_test.* TO 'bankflow_app'@'localhost';

FLUSH PRIVILEGES;

-- Verify
SELECT user, host, plugin FROM mysql.user WHERE user = 'bankflow_app';
SHOW GRANTS FOR 'bankflow_app'@'localhost';
