/*
  为 users 表增加 role 列（USER / ADMIN）

  说明：
  - 使用动态 SQL 包裹 ALTER/UPDATE，避免 SSMS 在某些情况下对对象/列做“编译期解析”时报错。
  - 如果 dbo.users 或 role 列不存在，会按需创建/更新。
*/
USE [vcddb_pro];
GO

IF OBJECT_ID('dbo.users', 'U') IS NULL
BEGIN
    PRINT N'错误：表 dbo.users 不存在。请先确保已在 vcddb_pro 中执行 script.sql。';
END
ELSE
BEGIN
    IF COL_LENGTH('dbo.users', 'role') IS NULL
    BEGIN
        DECLARE @sql_add NVARCHAR(MAX) =
            N'ALTER TABLE [dbo].[users] ' +
            N'ADD [role] VARCHAR(20) NOT NULL ' +
            N'CONSTRAINT [DF_users_role] DEFAULT (''USER'');';
        EXEC (@sql_add);
        PRINT N'已添加列 users.role，默认值为 USER。';
    END
    ELSE
    BEGIN
        PRINT N'列 users.role 已存在，跳过新增。';
    END

    -- 设置内置管理员为 ADMIN（admin 行可能不存在；不存在时 update 不影响整体流程）
    DECLARE @sql_update NVARCHAR(MAX) =
        N'UPDATE [dbo].[users] ' +
        N'SET [role] = ''ADMIN'' ' +
        N'WHERE [username] = ''admin'';';
    EXEC (@sql_update);

    PRINT N'role 更新完成。请重启后端后再尝试登录。';
END
GO

