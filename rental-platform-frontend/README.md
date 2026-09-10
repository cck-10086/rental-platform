# 租房平台前端

基于 Vue 3 + Vite + Element Plus + Pinia 的单页应用。

## 启动

```bash
npm install
npm run dev      # 开发模式，http://localhost:5173
npm run build    # 生产构建，产物输出到 dist/
npm run preview  # 预览生产构建
```

## 目录说明

```text
src/api/       axios 请求封装与各模块接口
src/router/    路由与登录守卫
src/store/     Pinia 状态（用户信息）
src/views/     页面视图
```

开发模式下 `/api` 请求由 Vite 代理到 `http://localhost:8088`，生产部署需在网关/Nginx 配置相同代理。
