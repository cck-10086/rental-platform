# MinIO 一键启动脚本（合同文件存储）
$ErrorActionPreference = 'Stop'

$minio = 'E:\software\minio\minio.exe'
$dataDir = 'E:\software\minio\data'

if (-not (Test-Path $minio)) {
    Write-Error "未找到 MinIO：$minio，请先下载 minio.exe 放到该目录"
    exit 1
}

New-Item -ItemType Directory -Force -Path $dataDir | Out-Null

if (-not $env:MINIO_ROOT_USER) {
    $env:MINIO_ROOT_USER = $env:MINIO_ACCESS_KEY
}
if (-not $env:MINIO_ROOT_PASSWORD) {
    $env:MINIO_ROOT_PASSWORD = $env:MINIO_SECRET_KEY
}
if (-not $env:MINIO_ROOT_USER -or -not $env:MINIO_ROOT_PASSWORD) {
    Write-Error '未检测到 MinIO 凭据。请先设置 MINIO_ACCESS_KEY 和 MINIO_SECRET_KEY 环境变量。'
    exit 1
}

Write-Host '正在启动 MinIO（API: http://localhost:9000，控制台: http://localhost:9001）...' -ForegroundColor Cyan
& $minio server $dataDir --address :9000 --console-address :9001
