-- 数据库迁移脚本：添加本周事项手动填写字段
-- 执行前请先备份数据库

ALTER TABLE weekly_report 
ADD COLUMN this_week_tasks TEXT COMMENT '本周事项(手动填写)' AFTER status,
ADD COLUMN next_week_plans TEXT COMMENT '下周预计事项' AFTER this_week_tasks,
ADD COLUMN notes TEXT COMMENT '补充说明' AFTER next_week_plans;
