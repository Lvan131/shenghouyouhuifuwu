# 生活优惠服务平台

一个包含管理后台、后端服务、微信小程序和数据库脚本的校园优惠服务项目。

## 项目结构

```text
admin-web/   管理后台，Vue 3 + Vite + Element Plus
backend/     后端服务，Spring Boot 3 + MyBatis-Plus + MySQL
miniapp/     微信小程序
sql/         数据库初始化与升级脚本
```

## 技术栈

- 管理后台：Vue 3、Vite、Pinia、Vue Router、Element Plus、Axios
- 后端：Java 17、Spring Boot 3.3.5、MyBatis-Plus、MySQL、JWT
- 小程序：微信原生小程序

## 环境要求

- Node.js 18+
- Java 17
- Maven 3.9+
- MySQL 8.x
- 微信开发者工具

## 数据库初始化

1. 创建数据库 `youhuifuwu`
2. 依次执行 `sql` 目录中的脚本：

```text
sql/01_schema.sql
sql/02_init_data.sql
sql/03_upgrade_distance.sql
sql/04_upgrade_daily_quota_and_join_date.sql
sql/05_upgrade_merchant_rating.sql
```

## 后端启动

后端默认配置文件位于 [application.yml](D:/wechatbishe/youhuifuwu/backend/src/main/resources/application.yml:1)。

仓库中提交的是可公开的默认配置，敏感值改为环境变量读取。

默认变量：

- 端口：`8080`
- 数据库：`jdbc:mysql://127.0.0.1:3306/youhuifuwu`
- 用户名：`root`
- 密码：`DB_PASSWORD`，默认占位值为 `change-me`
- 允许跨域来源：`http://localhost:5173`
- JWT 密钥：`JWT_SECRET`，默认占位值为 `change-me-to-a-long-random-string`

推荐做法：

1. 复制 [application-local.example.yml](D:/wechatbishe/youhuifuwu/backend/src/main/resources/application-local.example.yml:1) 为 `backend/src/main/resources/application-local.yml`
2. 把你自己的数据库密码和 JWT 密钥填进去
3. 这个本地文件已加入 `.gitignore`，不会被提交

也可以直接通过环境变量覆盖：

```bash
DB_PASSWORD=你的数据库密码
JWT_SECRET=你的本地JWT密钥
```

启动命令：

```bash
cd backend
mvn spring-boot:run
```

打包命令：

```bash
cd backend
mvn clean package
```

## 管理后台启动

可选：先复制 [admin-web/.env.example](D:/wechatbishe/youhuifuwu/admin-web/.env.example:1) 为 `admin-web/.env.local`，再按需修改接口地址。

启动开发环境：

```bash
cd admin-web
npm install
npm run dev
```

构建生产包：

```bash
cd admin-web
npm run build
```

默认开发地址一般为：

- `http://localhost:5173`

## 微信小程序运行

1. 打开微信开发者工具
2. 导入目录 `miniapp/`
3. AppID 使用 [project.config.json](D:/wechatbishe/youhuifuwu/miniapp/project.config.json:1) 中的配置
4. 可选：复制 [miniapp/config.example.js](D:/wechatbishe/youhuifuwu/miniapp/config.example.js:1) 为 `miniapp/config.js`，按需修改 `baseUrl`
5. 根据本地后端地址检查并调整小程序请求配置

## Git 说明

仓库已配置基础忽略规则，默认不会提交以下内容：

- IDE 配置目录
- `node_modules`
- 前端构建产物
- Java 构建产物
- 本地日志文件
- 微信开发者工具私有配置

如果你需要提交部署产物或其他本地文件，请按实际情况调整 [`.gitignore`](D:/wechatbishe/youhuifuwu/.gitignore:1)。
