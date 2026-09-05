-- ============================================================
-- 膨胀 benchmark 数据：1000 个虚拟产品 × 14 道工序 = 14000 行 product_process
-- 这样 MySQL JOIN 的基数大了，Neo4j 的图遍历优势才能体现
-- ============================================================

-- 先清理旧的膨胀数据
DELETE FROM product_process WHERE product_id >= 'PRD1001';
DELETE FROM graph_product WHERE id >= 'PRD1001';

-- 生成 1001~2000 的数字序列（三角 cross join）
INSERT INTO graph_product(id, name, category)
SELECT CONCAT('PRD', LPAD(num, 4, '0')), CONCAT('批量测试产品-', num), '测试产品'
FROM (
  SELECT (a.n + b.n*10 + c.n*100 + d.n*1000) AS num
  FROM (SELECT 1 AS n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 0) a
  CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b
  CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c
  CROSS JOIN (SELECT 0 AS n UNION SELECT 1) d
) seq
WHERE num BETWEEN 1001 AND 2000;

-- 每个虚拟产品复制 PRD002 的 14 道工序
INSERT INTO product_process(product_id, process_id)
SELECT gp.id, pp.process_id
FROM graph_product gp
CROSS JOIN product_process pp
WHERE pp.product_id = 'PRD002' AND gp.id >= 'PRD1001';

-- 验证规模
SELECT 'graph_product' AS t, COUNT(*) AS c FROM graph_product
UNION ALL SELECT 'product_process', COUNT(*) FROM product_process;
