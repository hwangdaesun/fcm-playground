CREATE
    USER 'local'@'localhost' IDENTIFIED BY 'local';
CREATE
    USER 'local'@'%' IDENTIFIED BY 'local';

GRANT ALL PRIVILEGES ON *.* TO
    'local'@'localhost';
GRANT ALL PRIVILEGES ON *.* TO
    'local'@'%';

CREATE
DATABASE local_db DEFAULT CHARACTER
SET utf8mb4 COLLATE utf8mb4_unicode_ci;

FLUSH PRIVILEGES;
