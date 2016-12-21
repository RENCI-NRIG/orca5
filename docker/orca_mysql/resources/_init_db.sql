-- Emulation settings
CREATE DATABASE orca12080;
CREATE DATABASE orca14080;
CREATE USER 'orca'@'%';
GRANT ALL PRIVILEGES ON orca12080.* TO 'orca'@'%';
GRANT ALL PRIVILEGES ON orca14080.* TO 'orca'@'%';

-- Test settings
CREATE DATABASE orca_test;
CREATE USER 'orca_test'@'%';
GRANT ALL PRIVILEGES ON orca_test.* TO 'orca_test'@'%';

