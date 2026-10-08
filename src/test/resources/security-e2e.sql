-- =======================================
-- Spring Security 端到端测试数据（H2）
-- 创建鉴权所需的最小表结构与两个用户：超级管理员与普通用户
-- =======================================

-- schema-h2.sql 中已存在结构不兼容的同名表，先删除再按实体结构重建
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_role_permission_group;
DROP TABLE IF EXISTS sys_permission_group_item;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_permission_group;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    nickname VARCHAR(50),
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    bio VARCHAR(500),
    status INT DEFAULT 1,
    is_deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(50),
    description VARCHAR(200),
    is_super_admin BOOLEAN DEFAULT FALSE,
    is_system BOOLEAN DEFAULT FALSE,
    is_deleted INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(100),
    description VARCHAR(200),
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_permission_group (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100),
    description VARCHAR(200),
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    is_system BOOLEAN DEFAULT FALSE,
    is_deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_role_permission_group (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS sys_permission_group_item (
    id BIGINT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    sort_order INT DEFAULT 0
);

DELETE FROM sys_user_role;
DELETE FROM sys_role_permission;
DELETE FROM sys_role_permission_group;
DELETE FROM sys_permission_group_item;
DELETE FROM sys_user;
DELETE FROM sys_role;
DELETE FROM sys_permission;

INSERT INTO sys_user (id, username, nickname, password, status, is_deleted) VALUES
    (1, 'root', 'Root', 'not-used-in-test', 1, 0),
    (2, 'bobuser', 'Bob', 'not-used-in-test', 1, 0);

INSERT INTO sys_role (id, code, name, is_super_admin, status, is_deleted) VALUES
    (1, 'SUPER_ADMIN', '超级管理员', TRUE, 1, 0),
    (2, 'USER', '普通用户', FALSE, 1, 0);

INSERT INTO sys_user_role (id, user_id, role_id) VALUES (1, 1, 1), (2, 2, 2);

INSERT INTO sys_permission (id, code, name) VALUES
    (1, 'article:list', '文章列表'),
    (2, 'system:article:list', '系统文章列表');

-- 普通用户角色直接授权 article:list（供权限列表接口断言）
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES (1, 2, 1);
