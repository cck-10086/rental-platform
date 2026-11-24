# 前端一键启动脚本（Vue 3 + Vite，端口 5173）
# 用法：双击 start-frontend.bat，或在本目录执行
#   powershell -ExecutionPolicy Bypass -File start-frontend.ps1
# 自检（不启动服务，只检查环境和依赖）：
#   powershell -ExecutionPolicy Bypass -File start-frontend.ps1 -Check
param([switch]$Check)

$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$fe   = Join-Path $root 'rental-platform-frontend'

if (-not (Test-Path $fe)) {
    Write-Host "找不到前端目录：$fe" -ForegroundColor Red
    exit 1
}

$pkg = Join-Path $fe 'package.json'
if (-not (Test-Path $pkg)) {
    Write-Host "前端目录里没有 package.json：$fe" -ForegroundColor Red
    exit 1
}

$nodeCmd = Get-Command node -ErrorAction SilentlyContinue
$npmCmd  = Get-Command npm.cmd -ErrorAction SilentlyContinue
if (-not $npmCmd) { $npmCmd = Get-Command npm -ErrorAction SilentlyContinue }

if (-not $nodeCmd -or -not $npmCmd) {
    Write-Host '找不到 node / npm，请先安装 Node.js 18+ 并确保它在 PATH 里。' -ForegroundColor Red
    exit 1
}

$modules     = Join-Path $fe 'node_modules'
$needInstall = -not (Test-Path $modules)

if ($Check) {
    Write-Host ''
    Write-Host '[自检结果]' -ForegroundColor Cyan
    Write-Host ('  项目根      : ' + $root)
    Write-Host ('  前端目录    : ' + $fe + '  (存在)')
    Write-Host ('  node        : ' + (& node --version))
    Write-Host ('  npm         : ' + (& npm --version))
    if ($needInstall) { Write-Host '  node_modules: 缺失，启动时会自动 npm install' }
    else              { Write-Host '  node_modules: 已存在' }
    Write-Host ''
    Write-Host '自检通过，去掉 -Check 即可启动。' -ForegroundColor Green
    exit 0
}

Set-Location $fe

if ($needInstall) {
    Write-Host '首次运行，正在安装依赖（npm install，可能要几分钟）...' -ForegroundColor Yellow
    & npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'npm install 失败。可换国内镜像后重试：' -ForegroundColor Red
        Write-Host '  npm config set registry https://registry.npmmirror.com'
        exit 1
    }
}

Write-Host ''
Write-Host '正在启动前端：http://localhost:5173' -ForegroundColor Cyan
Write-Host '看到 VITE ready 就是好了；按 Ctrl+C 停止。' -ForegroundColor DarkGray
Write-Host ''

& npm run dev
