# MinIO 一键启动脚本（合同文件存储）
$ErrorActionPreference = 'Stop'

$minio = 'E:\software\minio\minio.exe'
$dataDir = 'E:\software\minio\data'

if (-not (Test-Path $minio)) {
    Write-Error "未找到 MinIO：$minio，请先下载 minio.exe 放到该目录"
    exit 1
}

New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
Write-Host '正在启动 MinIO（API: http://localhost:9000，控制台: http://localhost:9001，默认账号 minioadmin/minioadmin）...' -ForegroundColor Cyan
& $minio server $dataDir --address :9000 --console-address :9001
