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

当前默认配置：

- 端口：`8080`
- 数据库：`jdbc:mysql://127.0.0.1:3306/youhuifuwu`
- 允许跨域来源：`http://localhost:5173`

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
4. 根据本地后端地址检查并调整小程序请求配置

## Git 说明

仓库已配置基础忽略规则，默认不会提交以下内容：

- IDE 配置目录
- `node_modules`
- 前端构建产物
- Java 构建产物
- 本地日志文件
- 微信开发者工具私有配置

如果你需要提交部署产物或其他本地文件，请按实际情况调整 [`.gitignore`](D:/wechatbishe/youhuifuwu/.gitignore:1)。
