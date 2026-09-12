-- ============================================================
-- 修复：operator（销售运营）登录后无任何权限，被所有带
--       @RequiresPermission 的接口拦截返回 403
--
-- 原因：sys_role_permission 表建了但从没插入过关联数据，
--       SysPermissionMapper.selectPermissionsByRoleId 的
--       INNER JOIN 必然返回空集
--
-- 用法：直接对已有数据库执行本文件（幂等，可重复执行）
--       mysql -u root -p green_chain < fix_operator_permissions.sql
--
-- ⚠️ 不要直接重跑 schema.sql —— 它开头有 DROP TABLE，会清空你现有数据
-- ============================================================

INSERT IGNORE INTO sys_role_permission (`role_id`, `permission_id`) VALUES
(2, 1),  (2, 46),
(2, 2),  (2, 3),  (2, 4),  (2, 5),
(2, 6),  (2, 7),  (2, 8),  (2, 9),  (2, 10),
(2, 11), (2, 12), (2, 13), (2, 14),
(2, 18), (2, 19), (2, 20), (2, 21),
(2, 22), (2, 23), (2, 24), (2, 25),
(2, 36), (2, 37),
(2, 38),
(2, 42),
(2, 44),
(2, 45);

-- ============================================================
-- 验证：执行后应看到 operator 拿到 29 条权限，
--       且包含 carousel / news / aftersale / upload 四组关键键
-- ============================================================
SELECT a.username, a.real_name, r.role_name, COUNT(rp.permission_id) AS perm_count
FROM sys_admin a
         LEFT JOIN sys_role r ON a.role_id = r.id
         LEFT JOIN sys_role_permission rp ON r.id = rp.role_id
GROUP BY a.id, a.username, a.real_name, r.role_name;

SELECT p.permission_key
FROM sys_role_permission rp
         JOIN sys_permission p ON rp.permission_id = p.id
WHERE rp.role_id = 2
  AND p.permission_key IN ('sys:carousel:list', 'sys:carousel:add', 'sys:carousel:edit', 'sys:carousel:delete',
                           'sys:news:list', 'sys:news:add', 'sys:news:edit', 'sys:news:delete',
                           'sys:aftersale:list', 'sys:aftersale:review',
                           'sys:upload:image')
ORDER BY p.permission_key;
