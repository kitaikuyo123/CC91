# CC91 编译 & 部署脚本
param(
    [switch]$buildOnly,   # 只编译不部署
    [switch]$skipBuild    # 跳过编译，只部署
)

$services = @("eureka-server", "gateway", "user-service", "forum-service",
              "notification-service", "content-service", "file-service")

if (-not $skipBuild) {
    Write-Host "=== 编译所有微服务 ===" -ForegroundColor Cyan
    foreach ($s in $services) {
        Write-Host "  $s..." -NoNewline
        pushd "$PSScriptRoot/../microservices/$s"
        $out = mvn clean package -DskipTests -B -q 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host " 失败!" -ForegroundColor Red
            Write-Host $out
            popd
            exit 1
        }
        popd
        Write-Host " 完成" -ForegroundColor Green
    }
    Write-Host "编译完成`n" -ForegroundColor Cyan
}

if (-not $buildOnly) {
    Write-Host "=== 部署 Docker ===" -ForegroundColor Cyan
    docker compose up -d --build
    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n部署完成: http://localhost:3001" -ForegroundColor Green
    }
}
