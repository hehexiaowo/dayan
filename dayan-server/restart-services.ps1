# 大雁四服务后台拉起脚本（独立进程，脱离调用方生命周期）
# 用法: powershell -NoProfile -ExecutionPolicy Bypass -File restart-services.ps1
# 背景：通过 CLI 后台任务启动的 java 进程会随任务回收被连带终止，
#       本脚本用 Start-Process 拉起真正独立的 Windows 进程，日志落盘到各 starter 目录。
$ErrorActionPreference = 'Stop'
$base = Join-Path $PSScriptRoot 'dayan-starters'
$services = @('dayan-admin', 'dayan-channel', 'dayan-agent', 'dayan-client')

foreach ($name in $services) {
    $dir = Join-Path $base $name
    $jar = "target\$name.jar"
    $outLog = Join-Path $dir 'service-run.log'
    $errLog = Join-Path $dir 'service-run.err.log'
    Start-Process -FilePath 'java' -ArgumentList '-jar', $jar `
        -WorkingDirectory $dir -WindowStyle Hidden `
        -RedirectStandardOutput $outLog -RedirectStandardError $errLog
    Write-Output "started $name"
}
