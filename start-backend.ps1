# 后端一键启动脚本（不依赖系统 PATH，双击或右键"使用 PowerShell 运行"）
# 使用前请先在系统环境变量中配置 DB_USERNAME、DB_PASSWORD 与 DEEPSEEK_API_KEY（setx 后重启终端）
$ErrorActionPreference = 'Stop'

$env:JAVA_HOME = 'E:\software\java\java-jdk.25'
$env:MAVEN_HOME = 'E:\software\maven\apache-maven-3.9.15'
$env:DB_USERNAME = 'root'
 
if (-not $env:DB_PASSWORD) {
    Write-Warning '未检测到环境变量 DB_PASSWORD，后端将无法连接数据库。请先执行：setx DB_PASSWORD "你的数据库密码" 并重启终端'
    exit 1
}

if (-not $env:DEEPSEEK_API_KEY) {
    Write-Warning '未检测到环境变量 DEEPSEEK_API_KEY，AI 功能将不可用。请先执行：setx DEEPSEEK_API_KEY "你的Key" 并重启终端'
}

Set-Location 'E:\study\毕业设计\rental-platform-backend'
Write-Host '正在启动后端（http://localhost:8088，文档 /doc.html）...' -ForegroundColor Cyan
& "$env:MAVEN_HOME\bin\mvn.cmd" spring-boot:run
