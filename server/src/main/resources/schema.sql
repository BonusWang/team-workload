DROP TABLE IF EXISTS daily_report;
DROP TABLE IF EXISTS weekly_report;
DROP TABLE IF EXISTS monthly_summary;
DROP TABLE IF EXISTS leave_request;
DROP TABLE IF EXISTS work_day;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名(登录账号)',
    password VARCHAR(200) NOT NULL COMMENT '密码',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    dept VARCHAR(100) COMMENT '部门',
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '角色: ADMIN-管理员, LEADER-负责人, MEMBER-成员',
    level VARCHAR(20) COMMENT '级别: JUNIOR-初级, MIDDLE-中级, SENIOR-高级',
    unit_price DECIMAL(10,2) COMMENT '单价(元/人天)',
    leader_id BIGINT COMMENT '直属负责人ID',
    email VARCHAR(100) COMMENT '邮箱',
    annual_leave_balance DECIMAL(5,1) DEFAULT 10.0 COMMENT '年假余额(天)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='系统用户表';

CREATE TABLE project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '项目/产品名称',
    category VARCHAR(50) COMMENT '项目分类',
    description VARCHAR(500) COMMENT '项目描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-进行中, 0-已归档',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='项目信息表';

CREATE TABLE daily_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '提交人用户ID',
    work_date DATE NOT NULL COMMENT '工作日期',
    project_name VARCHAR(200) NOT NULL COMMENT '所属项目/产品名称',
    task_no VARCHAR(100) COMMENT '任务编号',
    task_name VARCHAR(500) COMMENT '任务名称',
    hours DECIMAL(5,1) NOT NULL COMMENT '实际工时(小时)',
    source VARCHAR(20) NOT NULL DEFAULT 'QUANKAI' COMMENT '数据来源: QUANKAI-全开系统, OTHER-其他投入清单',
    import_batch_no VARCHAR(50) COMMENT '导入批次号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_date (user_id, work_date),
    INDEX idx_batch (import_batch_no)
) COMMENT='日报明细表';

CREATE TABLE weekly_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    week_start_date DATE NOT NULL COMMENT '周开始日期(周四)',
    week_end_date DATE NOT NULL COMMENT '周结束日期(周三)',
    total_hours DECIMAL(6,1) DEFAULT 0 COMMENT '本周总工时(小时)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT-草稿, SUBMITTED-已提交',
    this_week_tasks TEXT COMMENT '本周事项(手动填写)',
    next_week_plans TEXT COMMENT '下周预计事项',
    notes TEXT COMMENT '补充说明',
    submitted_at DATETIME COMMENT '提交时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX idx_user_week (user_id, week_end_date)
) COMMENT='周报汇总表';

CREATE TABLE monthly_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    `year_month` VARCHAR(7) NOT NULL COMMENT '年月(格式: yyyy-MM)',
    total_hours DECIMAL(7,1) DEFAULT 0 COMMENT '月度总工时(小时)',
    work_days INT DEFAULT 0 COMMENT '应出勤天数',
    leave_days DECIMAL(5,1) DEFAULT 0 COMMENT '请假天数',
    daily_avg DECIMAL(5,1) DEFAULT 0 COMMENT '日均投入工时(小时)',
    task_type_dist TEXT COMMENT '任务类型分布(JSON)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX idx_user_month (user_id, `year_month`)
) COMMENT='月度汇总表';

CREATE TABLE leave_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '申请人用户ID',
    leave_type VARCHAR(20) NOT NULL COMMENT '请假类型: ANNUAL-年假, SICK-病假, PERSONAL-事假, MARRIAGE-婚假, MATERNITY-产假, OTHER-其他',
    start_date DATE NOT NULL COMMENT '请假开始日期',
    start_time VARCHAR(5) DEFAULT '09:00' COMMENT '请假开始时间(HH:mm)',
    end_date DATE NOT NULL COMMENT '请假结束日期',
    end_time VARCHAR(5) DEFAULT '18:00' COMMENT '请假结束时间(HH:mm)',
    days DECIMAL(5,1) NOT NULL COMMENT '请假天数',
    reason VARCHAR(500) COMMENT '请假原因',
    approver_id BIGINT COMMENT '审批人用户ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待审批, APPROVED-已通过, REJECTED-已驳回',
    reject_reason VARCHAR(500) COMMENT '驳回原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    approve_time DATETIME COMMENT '审批时间',
    INDEX idx_user (user_id),
    INDEX idx_approver (approver_id)
) COMMENT='请假申请表';

CREATE TABLE work_day (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `date` DATE NOT NULL UNIQUE COMMENT '日期',
    is_workday TINYINT NOT NULL DEFAULT 1 COMMENT '是否工作日: 1-是, 0-否',
    type VARCHAR(20) NOT NULL DEFAULT 'WORKDAY' COMMENT '类型: WORKDAY-工作日, WEEKEND-周末, HOLIDAY-节假日, MAKEUP-调休补班',
    description VARCHAR(200) COMMENT '描述(如: 春节、国庆等)',
    year INT NOT NULL COMMENT '所属年份',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_year (year),
    UNIQUE INDEX idx_date (`date`)
) COMMENT='工作日历表';

INSERT INTO sys_user (username, password, name, role, status) VALUES ('admin', 'admin123', '管理员', 'ADMIN', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('陈铭辛', '123456', '陈铭辛', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('袁嘉键', '123456', '袁嘉键', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('杨江萍', '123456', '杨江萍', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('杨晓林', '123456', '杨晓林', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('罗文博', '123456', '罗文博', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('赵立莹', '123456', '赵立莹', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('胡建悦', '123456', '胡建悦', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('王景涛', '123456', '王景涛', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('彭朝林', '123456', '彭朝林', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('刘远航', '123456', '刘远航', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('娄梦凡', '123456', '娄梦凡', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('姚竞涛', '123456', '姚竞涛', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('鲍翔', '123456', '鲍翔', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('梁万顺', '123456', '梁万顺', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('李成硕', '123456', '李成硕', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('姚磊', '123456', '姚磊', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('吕帅奇', '123456', '吕帅奇', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('王国伟', '123456', '王国伟', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('吴辉', '123456', '吴辉', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('赵启岸', '123456', '赵启岸', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('王婷婷', '123456', '王婷婷', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('秦浩', '123456', '秦浩', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('王珞淳', '123456', '王珞淳', 'MEMBER', 1);
INSERT INTO sys_user (username, password, name, role, status) VALUES ('苏雯', '123456', '苏雯', 'MEMBER', 1);

-- 项目分类数据 (码值表)
INSERT INTO project (name, category, description, status, create_time, update_time) VALUES ('监管集市缺陷问题', '监管集市缺陷问题', null, 1, '2026-04-22 07:28:20', '2026-04-22 07:31:40');
INSERT INTO project (name, category, description, status, create_time, update_time) VALUES ('CDP项目', 'CDP项目', null, 1, '2026-04-22 07:28:20', '2026-04-22 07:31:40');
INSERT INTO project (name, category, description, status, create_time, update_time) VALUES ('POC项目实施', 'POC项目实施', null, 1, '2026-04-22 07:28:20', '2026-04-22 07:31:40');

