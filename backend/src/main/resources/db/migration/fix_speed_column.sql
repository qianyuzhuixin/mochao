-- 修复 speed 列溢出问题
-- 原因：DECIMAL(5,2) 最大值 999.99，短时间练习时速度计算值可超过此上限
-- 修复：扩展为 DECIMAL(7,2)，最大值 99999.99

ALTER TABLE t_practice_session MODIFY COLUMN speed DECIMAL(7,2) DEFAULT 0.00 COMMENT '速度(字/分)';
ALTER TABLE t_daily_statistics MODIFY COLUMN avg_speed DECIMAL(7,2) DEFAULT 0.00 COMMENT '平均速度';
