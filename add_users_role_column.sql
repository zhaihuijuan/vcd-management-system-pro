/*
  为 users 表增加 role 列（与后端 User 实体 UserRole 枚举一致：USER / ADMIN）
  在 SSMS 中选中数据库 vcddb_pro 后执行本脚本；若列已存在会跳过。
*/
USE [vcddb_pro];
GO

IF OBJECT_ID('dbo.users', 'U') IS NULL
BEGIN
    PRINT N'错误：表 dbo.users 不存在。请先在同一个数据库中执行 script.sql 创建表。';
END
ELSE IF NOT EXISTS (
    SELECT 1
    FROM sys.columns c
    INNER JOIN sys.tables t ON c.object_id = t.object_id
    WHERE t.name = 'users'
      AND SCHEMA_NAME(t.schema_id) = 'dbo'
      AND c.name = 'role'
)
BEGIN
    ALTER TABLE [dbo].[users]
    ADD [role] VARCHAR(20) NOT NULL
        CONSTRAINT [DF_users_role] DEFAULT ('USER');
    PRINT N'已添加列 users.role，默认值为 USER。';
END
ELSE
BEGIN
    PRINT N'列 users.role 已存在，跳过。';a
END
GO

/* 将管理员账号设为 ADMIN（按实际用户名修改） */
IF OBJECT_ID('dbo.users', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.users', 'role') IS NOT NULL
    BEGIN
        UPDATE [dbo].[users]
        SET [role] = 'ADMIN'
        WHERE [username] = N'admin';
        PRINT N'完成。请重启后端后再尝试登录。';
    END
    ELSE
    BEGIN
        PRINT N'未检测到 role 列，未执行管理员角色更新。';
    END
END
ELSE
BEGIN
    PRINT N'错误：表 dbo.users 不存在，跳过管理员角色更新。';
END
GO
