<<<<<<< HEAD
# VCD 管理系统

基于 **Spring Boot 3** + **Vue 3** + **SQL Server** 的 VCD 租赁/进销存管理项目。前端为双入口：`login.html`（登录）、`manage.html`（管理后台）。

---

## 环境要求

| 软件 | 说明 |
|------|------|
| JDK | **17**（与 `pom.xml` 中 `java.version` 一致） |
| Maven | 3.6+（或使用本仓库自带的 `maven_bin`，见下文） |
| SQL Server | 本地或远程实例，默认连接 **localhost:1433** |
| Node.js | **18+**（建议 LTS） |
| npm | 随 Node 安装即可 |

---

## 一、准备数据库

1. 启动 **SQL Server** 服务，并启用 **TCP/IP**（默认端口 **1433**）。
2. 使用 SSMS 或命令行连接实例，执行：
   ```sql
   CREATE DATABASE vcddb;
   ```
3. 在数据库 `vcddb` 中执行仓库根目录下的建表与初始化脚本：
   - 文件路径：**`script.sql`**（与 `README.md` 同级）
4. 打开 **`vcd-backend/src/main/resources/application.properties`**，确认与你的环境一致：
   - `spring.datasource.url`（服务器地址、端口、数据库名 `vcddb`）
   - `spring.datasource.username` / `spring.datasource.password`（如 `sa` 密码与文件中不一致，请改成你的密码）

> 说明：项目使用 JPA `ddl-auto=update`，若表结构有变更，启动后端时会自动对齐实体；**首次仍建议先执行 `script.sql`**，保证基础数据与表完整。

---

## 二、启动后端（必须先于前端联调）

```bash
cd vcd-backend
```

### 最省事方式

在**仓库根目录**执行：

```bash
./start-backend-with-db.sh
```

该脚本会自动：

- 启动 `colima`（若未启动）
- 启动本地数据库容器 `vcd-sqlserver`
- 确认数据库 `vcddb` 存在
- 使用本机 JDK 17 启动后端

**方式 A：本机已安装 Maven**

```bash
mvn spring-boot:run
```

**方式 B：使用仓库内自带的 Maven（`maven_bin` 在仓库根目录）**

在**仓库根目录**执行：

```bash
./maven_bin/bin/mvn -f vcd-backend/pom.xml spring-boot:run
```

或在 **`vcd-backend` 目录**下执行：

```bash
../maven_bin/bin/mvn spring-boot:run
```

**方式 C：IDE**

用 IntelliJ IDEA 打开 `vcd-backend`，运行 `src/main/java/top/tatobamail/backend/BackendApplication.java`。

启动成功后，控制台出现 **`Started BackendApplication`**，默认接口地址为：

```text
http://localhost:8080
```

---

## 三、启动前端（新手推荐：开发模式）

开发模式下，Vite 会把以 **`/api`** 开头的请求代理到 `http://localhost:8080`（见 `vcd-frontend/vite.config.js`），因此**无需**先打包前端即可调试。

```bash
cd vcd-frontend
npm install
npm run dev
```

浏览器访问：

```text
http://localhost:5173/login.html
```

登录成功后会进入管理端（开发环境同样走 Vite 端口 **5173**）。

> 请保持**后端 8080 已启动**，否则登录与接口会失败。

---

## 四、另一种用法：只启动后端 + 静态页面

若已将前端**构建**进后端的 `classpath:/dist/`（见下一节），可**只运行后端**，直接访问：

```text
http://localhost:8080/login.html
```

适合接近生产部署的验证。

### 构建前端到后端目录

```bash
cd vcd-frontend
npm install
npm run build
```

构建产物会输出到 **`vcd-backend/src/main/resources/dist/`**（由 `vite.config.js` 配置）。然后重新启动后端即可从 8080 访问上述地址。

---

## 五、账号与权限（简要）

- **内置管理员**：用户名 **`admin`**、密码 **`admin`**（与数据库是否存在无关），登录后后端固定为 **ADMIN**，可访问大屏 `/api/dashboard` 及全部管理接口；**注册时禁止使用 `admin` 作为用户名**。
- 其他账号可在登录页**注册**，默认一般为 **USER**（普通用户）；若需在库中手动提升为管理员，将 `users.role` 设为 `ADMIN`。

---

## 六、常见问题

| 现象 | 处理 |
|------|------|
| 登录报错 `Invalid column name 'role'` | 数据库 `users` 表缺少 `role` 列。在 **vcddb** 中执行仓库根目录的 **`add_users_role_column.sql`**，保存后**重启后端**再登录 |
| 数据库连不上 | 检查 SQL Server 是否运行、TCP 端口、`application.properties` 中的账号密码与库名 `vcddb` |
| 8080 被占用 | 修改 `application.properties` 的 `server.port`，并同步修改前端代理 `vite.config.js` 中的 `target`（若开发联调） |
| 前端能开但接口全失败 | 确认后端已启动；开发模式请使用 **5173** 并走 `/api` 代理，不要只开前端 |
| 页面空白（仅访问 8080） | 确认已执行 `npm run build` 且 `vcd-backend/src/main/resources/dist/` 下存在 `login.html`、`manage.html` |
| Maven 下载依赖慢 | 为国内 Maven 配置镜像（如阿里云），见 `部署文档.md` |

更完整的部署说明与目录结构可参考同目录下的 **`部署文档.md`**。

---

## 七、目录结构（摘要）

```text
ruanjianjiagou/
├── README.md                 # 本说明
├── script.sql                # 数据库脚本（在 vcddb 中执行）
├── vcd-backend/              # Spring Boot 后端
│   └── src/main/resources/
│       ├── application.properties
│       └── dist/             # npm run build 后生成的前端静态资源
└── vcd-frontend/             # Vue 3 + Vite 前端
    ├── login.html
    ├── manage.html
    └── vite.config.js        # 开发代理与 build 输出目录
```

---

## 八、许可证

以项目实际情况为准；若未特别声明，默认遵循仓库内已有约定。
=======
# vcd-management-system-pro
>>>>>>> 1f7cff34433ba398200af979b4e0ce1caa1126de
