# 智能租房合同审查与租房全流程管理平台

面向租客的租房辅助平台，提供合同上传与 AI 审查、房源管理、费用管理、维修报备、退租管理、AI 法律咨询等功能，并内置管理员后台（用户管理、全局数据查看、平台统计）。

## 项目结构

```text
rental-platform-backend/    Spring Boot 3 后端（MyBatis-Plus + JWT + MinIO + DeepSeek）
rental-platform-frontend/   Vue 3 前端（Vite + Element Plus + Pinia）
deploy/                     部署示例配置（Nginx、生产配置）
docs/code_review.md         代码审查清单
docs/部署文档.md             局域网/内网穿透/云服务器部署指南
docs/接口测试清单.md         后端接口测试清单（24 个用例）
docs/学习清单.md             从零开始掌握本项目的学习路线
docs/项目说明.md             项目功能与技术实现说明
_archive/                   归档区：scripts/（历史脚本）、ppt_build/（答辩 PPT 构建脚本）、working/（开题报告制作脚本）及材料提取文件，保留备查
```

## 技术栈

- 后端：Java 21、Spring Boot 3.4、MyBatis-Plus、MySQL、MinIO、JWT、Knife4j
- 前端：Vue 3、Vite、Element Plus、Pinia、Axios
- AI：DeepSeek API（模型 `deepseek-v4-flash`）
- 可视化与导出：ECharts 图表、PDFBox 审查报告导出 PDF
- 工程化：AOP 接口耗时日志、后端分页、合同文件预览/下载、AI 历史会话管理

## 角色说明

- **普通用户**：使用合同审查、房源、费用、维修、退租、AI 咨询等全部业务功能；
- **管理员**（`admin`，角色字段 `role=admin`）：额外拥有管理后台——用户管理（启用/禁用、改角色、重置密码）、全局数据只读查看（合同/房源/费用/报修/退租）、平台数据统计。

## 环境要求

- JDK 21+、Maven 3.8+
- Node.js 20+
- MySQL 8.x、MinIO

## 后端配置

敏感配置通过环境变量注入，不写入代码仓库：

| 环境变量 | 说明 |
| --- | --- |
| `DB_USERNAME` | 数据库用户名（默认 `root`） |
| `DB_PASSWORD` | 数据库密码（必填） |
| `DEEPSEEK_API_KEY` | DeepSeek API Key（必填，AI 功能依赖） |
| `JWT_SECRET` | JWT 签名密钥，生产环境必须更换 |

### 启动后端

1. 执行 `rental-platform-backend/sql/schema.sql` 初始化数据库；
2. 启动 MySQL 与 MinIO（`rental-contracts` 桶）；
3. 设置上述环境变量后运行：

```bash
cd rental-platform-backend
mvn spring-boot:run
```

接口文档（Knife4j）：`http://localhost:8088/doc.html`

## 前端

```bash
cd rental-platform-frontend
npm install
npm run dev      # http://localhost:5173
npm run build    # 生产构建
```

Vite 开发服务器已将 `/api` 代理到 `http://localhost:8088`。

## 一键启动

项目根目录提供了两个启动脚本，无需手动配置 PATH：

```powershell
.\start-backend.ps1     # 启动后端（首次使用前请先设置系统环境变量 DB_USERNAME/DB_PASSWORD/DEEPSEEK_API_KEY）
.\start-frontend.ps1    # 启动前端
```

合同上传依赖 MinIO（本机安装于 `E:\software\minio`），请先启动：

```powershell
.\start-minio.ps1       # 启动 MinIO（API 9000，控制台 9001）
```

> 在 cmd 窗口里请使用对应的 `.bat` 脚本（或先输入 `powershell` 再运行 `.ps1`）：

```cmd
start-backend.bat
start-frontend.bat
```

## 部署到其他环境

见 [docs/部署文档.md](docs/部署文档.md)：支持局域网访问、内网穿透（临时公网）和云服务器部署（Nginx + HTTPS）三种方式，示例配置在 `deploy/` 目录。

## 检查命令

```bash
npm run build    # 前端构建
npm test         # 前端单元测试（Vitest，8 个用例）
mvn test         # 后端单元测试（24 个用例，覆盖 JWT/安全/用户/管理/AI 会话与文件解析）
```

> 注：项目已配置 Vitest 前端测试与 JUnit/Mockito 后端测试；未配置 ESLint，`npm run lint` 不可用。

## 已知限制

- 合同文本解析支持 TXT/PDF/docx/doc；扫描版 PDF（图片）需 OCR 支持，属后续规划；
- AI 对话与合同审查依赖 DeepSeek API，需要网络与有效 API Key；
- OCR 识别与 Redis 缓存属后续规划；JWT 密钥为开发默认值，生产环境须通过环境变量更换。
