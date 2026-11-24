# 后端一键启动脚本（Spring Boot，端口 8088）
# 用法：双击 start-backend.bat，或在本目录执行
#   powershell -ExecutionPolicy Bypass -File start-backend.ps1
# 自检（不启动服务，只检查环境和配置）：
#   powershell -ExecutionPolicy Bypass -File start-backend.ps1 -Check
param([switch]$Check)

$ErrorActionPreference = 'Stop'

# ---------- 1. 定位项目（用脚本自身目录，不写死中文绝对路径） ----------
$root    = $PSScriptRoot
$backend = Join-Path $root 'rental-platform-backend'

if (-not (Test-Path $backend)) {
    Write-Host "找不到后端目录：$backend" -ForegroundColor Red
    exit 1
}

# ---------- 2. 补齐环境变量（进程里没有就读 用户级/系统级 注册表） ----------
$required = 'DB_USERNAME', 'DB_PASSWORD', 'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY', 'JWT_SECRET'
$optional = 'DEEPSEEK_API_KEY'

foreach ($name in ($required + $optional)) {
    if ([Environment]::GetEnvironmentVariable($name, 'Process')) { continue }
    $val = [Environment]::GetEnvironmentVariable($name, 'User')
    if (-not $val) { $val = [Environment]::GetEnvironmentVariable($name, 'Machine') }
    if ($val) { [Environment]::SetEnvironmentVariable($name, $val, 'Process') }
}

$missing = @()
foreach ($name in $required) {
    if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) { $missing += $name }
}

if ($missing.Count -gt 0) {
    Write-Host ''
    Write-Host ('缺少环境变量：' + ($missing -join ', ')) -ForegroundColor Red
    Write-Host '在 PowerShell 里逐个执行下面这行（把值换成你自己的），然后重开终端：' -ForegroundColor Yellow
    foreach ($name in $missing) {
        Write-Host ('  setx ' + $name + ' "你的值"')
    }
    exit 1
}

if (-not [Environment]::GetEnvironmentVariable('DEEPSEEK_API_KEY', 'Process')) {
    Write-Host '提示：没有 DEEPSEEK_API_KEY，AI 审查与 AI 顾问会不可用（其余功能正常）。' -ForegroundColor Yellow
}

# ---------- 3. 定位 JDK 与 Maven ----------
if (-not $env:JAVA_HOME)  { $env:JAVA_HOME  = 'E:\software\java\java-jdk.25' }
if (-not $env:MAVEN_HOME) { $env:MAVEN_HOME = 'E:\software\maven\apache-maven-3.9.15' }

$mvn = Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'
if (-not (Test-Path $mvn)) {
    $cmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($cmd) { $mvn = $cmd.Source }
    else {
        Write-Host "找不到 mvn：$mvn" -ForegroundColor Red
        Write-Host '请安装 Maven，或把 MAVEN_HOME 指向 Maven 安装目录。' -ForegroundColor Yellow
        exit 1
    }
}

if (-not (Test-Path $env:JAVA_HOME)) {
    Write-Host "找不到 JDK：$env:JAVA_HOME" -ForegroundColor Red
    Write-Host '请安装 JDK 21+，或把 JAVA_HOME 指向 JDK 安装目录。' -ForegroundColor Yellow
    exit 1
}

# ---------- 4. 自检 ----------
if ($Check) {
    Write-Host ''
    Write-Host '[自检结果]' -ForegroundColor Cyan
    Write-Host ('  项目根      : ' + $root)
    Write-Host ('  后端目录    : ' + $backend + '  (存在)')
    Write-Host ('  JDK         : ' + $env:JAVA_HOME)
    Write-Host ('  Maven       : ' + $mvn)
    Write-Host ('  DB_USERNAME : ' + $env:DB_USERNAME)
    Write-Host ('  DB_PASSWORD : ' + ('*' * $env:DB_PASSWORD.Length) + '  (长度 ' + $env:DB_PASSWORD.Length + ')')
    Write-Host ('  JWT_SECRET  : 长度 ' + $env:JWT_SECRET.Length)
    Write-Host ('  MinIO 凭据  : ' + $env:MINIO_ACCESS_KEY + ' / 长度 ' + $env:MINIO_SECRET_KEY.Length)
    $dk = [Environment]::GetEnvironmentVariable('DEEPSEEK_API_KEY', 'Process')
    if ($dk) { Write-Host ('  DeepSeek    : 已配置（长度 ' + $dk.Length + '）') }
    else     { Write-Host '  DeepSeek    : 未配置，AI 功能不可用' }
    Write-Host ''
    Write-Host '自检通过，去掉 -Check 即可启动。' -ForegroundColor Green
    exit 0
}

# ---------- 5. 启动 ----------
Set-Location $backend
Write-Host ''
Write-Host '正在启动后端：http://localhost:8088    接口文档：http://localhost:8088/doc.html' -ForegroundColor Cyan
Write-Host '首次启动要下载依赖，可能等 1-3 分钟；看到 Started 就是好了。' -ForegroundColor DarkGray
Write-Host '按 Ctrl+C 停止。' -ForegroundColor DarkGray
Write-Host ''

& $mvn spring-boot:run
