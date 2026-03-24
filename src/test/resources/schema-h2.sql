-- =======================================
-- Test Database Schema for H2
-- =======================================
-- This script is used for testing purposes only

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    nickname VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    avatar VARCHAR(500),
    role VARCHAR(20) DEFAULT 'user',
    is_deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 博客文章表
CREATE TABLE IF NOT EXISTS sys_blog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    author_id BIGINT,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content TEXT,
    cover_image VARCHAR(500),
    tags VARCHAR(500),
    comment_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    is_top BOOLEAN DEFAULT FALSE,
    hidden BOOLEAN DEFAULT FALSE,
    is_deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 分类表
CREATE TABLE IF NOT EXISTS sys_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    sort INT DEFAULT 0,
    hidden BOOLEAN DEFAULT FALSE,
    is_deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 评论表
CREATE TABLE IF NOT EXISTS sys_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    blog_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    user_id BIGINT,
    username VARCHAR(50),
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    website VARCHAR(200),
    content TEXT NOT NULL,
    status TINYINT DEFAULT 0,
    like_count INT DEFAULT 0,
    device_info VARCHAR(500),
    ip_address VARCHAR(50),
    is_admin BOOLEAN DEFAULT FALSE,
    is_deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 友链表
CREATE TABLE IF NOT EXISTS sys_friend_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    logo VARCHAR(500),
    description VARCHAR(200),
    sort INT DEFAULT 0,
    hidden BOOLEAN DEFAULT FALSE,
    is_deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
