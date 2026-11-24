# MinIO 一键启动脚本（合同文件存储，API 9000 / 控制台 9001）
# 用法：双击 start-minio.bat，或在本目录执行
#   powershell -ExecutionPolicy Bypass -File start-minio.ps1
# 自检（不启动服务，只检查程序和凭据）：
#   powershell -ExecutionPolicy Bypass -File start-minio.ps1 -Check
param([switch]$Check)

$ErrorActionPreference = 'Stop'

# minio.exe 位置：优先环境变量 MINIO_HOME，否则用默认路径
if (-not $env:MINIO_HOME) { $env:MINIO_HOME = 'E:\software\minio' }
$minioHome = $env:MINIO_HOME
$minio     = Join-Path $minioHome 'minio.exe'
$dataDir   = Join-Path $minioHome 'data'

if (-not (Test-Path $minio)) {
    Write-Host "找不到 minio.exe：$minio" -ForegroundColor Red
    Write-Host '请下载 minio.exe 放到该目录，或设置环境变量 MINIO_HOME 指向存放它的目录。' -ForegroundColor Yellow
    exit 1
}

# 凭据兜底：进程里没有就读 用户级/系统级 注册表
foreach ($name in 'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY') {
    if ([Environment]::GetEnvironmentVariable($name, 'Process')) { continue }
    $val = [Environment]::GetEnvironmentVariable($name, 'User')
    if (-not $val) { $val = [Environment]::GetEnvironmentVariable($name, 'Machine') }
    if ($val) { [Environment]::SetEnvironmentVariable($name, $val, 'Process') }
}

if (-not $env:MINIO_ACCESS_KEY -or -not $env:MINIO_SECRET_KEY) {
    Write-Host '缺少 MinIO 凭据。请先执行：' -ForegroundColor Red
    Write-Host '  setx MINIO_ACCESS_KEY "你的AccessKey"'
    Write-Host '  setx MINIO_SECRET_KEY "你的SecretKey"'
    Write-Host '然后重开终端。' -ForegroundColor Yellow
    exit 1
}

# MinIO 服务端读的是 ROOT_USER / ROOT_PASSWORD，不是 ACCESS_KEY / SECRET_KEY
if (-not $env:MINIO_ROOT_USER)     { $env:MINIO_ROOT_USER     = $env:MINIO_ACCESS_KEY }
if (-not $env:MINIO_ROOT_PASSWORD) { $env:MINIO_ROOT_PASSWORD = $env:MINIO_SECRET_KEY }

if ($Check) {
    Write-Host ''
    Write-Host '[自检结果]' -ForegroundColor Cyan
    Write-Host ('  minio.exe   : ' + $minio + '  (存在)')
    if (Test-Path $dataDir) { Write-Host ('  数据目录    : ' + $dataDir + '  (存在)') }
    else                    { Write-Host ('  数据目录    : ' + $dataDir + '  (不存在，启动时自动创建)') }
    Write-Host ('  账号        : ' + $env:MINIO_ROOT_USER)
    Write-Host ('  密码        : ' + ('*' * $env:MINIO_ROOT_PASSWORD.Length) + '  (长度 ' + $env:MINIO_ROOT_PASSWORD.Length + ')')
    Write-Host ''
    Write-Host '自检通过，去掉 -Check 即可启动。' -ForegroundColor Green
    exit 0
}

New-Item -ItemType Directory -Force -Path $dataDir | Out-Null

Write-Host ''
Write-Host '正在启动 MinIO：API http://localhost:9000    控制台 http://localhost:9001' -ForegroundColor Cyan
Write-Host ('控制台登录：' + $env:MINIO_ROOT_USER + ' / ' + ('*' * $env:MINIO_ROOT_PASSWORD.Length))
Write-Host '按 Ctrl+C 停止。' -ForegroundColor DarkGray
Write-Host ''

& $minio server $dataDir --address :9000 --console-address :9001
