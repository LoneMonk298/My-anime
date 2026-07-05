USE anime_record_site;

UPDATE sys_user
SET role = 'SUPER_ADMIN'
WHERE username = 'admin'
  AND deleted = 0;

SELECT id, username, role, status, deleted
FROM sys_user
WHERE username = 'admin';
