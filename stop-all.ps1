# 一键停止全部服务（后端 8088 / 前端 5173 / MinIO 9000、9001）
# 按端口精确停止，不会误杀其他程序

$ports = 8088, 5173, 9000, 9001
$stopped = @()

foreach ($port in $ports) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
        $proc = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
        if ($proc) {
            Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
            $stopped += "端口 ${port} 的 $($proc.ProcessName) (PID=$($_.OwningProcess))"
        }
    }
}

if ($stopped.Count -eq 0) {
    Write-Host '没有检测到运行中的服务（8088/5173/9000/9001 均未监听）' -ForegroundColor Green
} else {
    $stopped | ForEach-Object { Write-Host "已停止: $_" -ForegroundColor Yellow }
    Write-Host '服务已全部停止' -ForegroundColor Green
}
