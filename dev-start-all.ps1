# ============================================================
# Dev Services — One-Click Start All
# JDK: D:/JDK25 (default)
# Save to D:\dev-start-all.ps1
# Usage:
#   .\dev-start-all.ps1           → start all
#   .\dev-start-all.ps1 -status   → check status
#   .\dev-start-all.ps1 -stop     → stop all
# ============================================================

param(
    [switch]$stop,
    [switch]$status
)

$env:JAVA_HOME = "D:\JDK25"

$services = @(
    @{Name="Zookeeper"; Port=2181; Script="D:\zookeeper\bin\zkServer.cmd"; Dir="D:\zookeeper"},
    @{Name="Kafka"; Port=9092; Script="D:\Kafka\bin\windows\kafka-server-start.bat"; Args="D:\Kafka\config\server.properties"; Dir="D:\Kafka"; DependsOn="Zookeeper"},
    @{Name="RocketMQ-NameServer"; Port=9876; Script="D:\RocketMQ\rocketmq-all-5.5.0-bin-release\bin\mqnamesrv.cmd"; Dir="D:\RocketMQ\rocketmq-all-5.5.0-bin-release"},
    @{Name="RocketMQ-Broker"; Port=10911; Script="D:\RocketMQ\rocketmq-all-5.5.0-bin-release\bin\mqbroker.cmd"; Args="-c D:\RocketMQ\rocketmq-all-5.5.0-bin-release\conf\broker.conf"; Dir="D:\RocketMQ\rocketmq-all-5.5.0-bin-release"; DependsOn="RocketMQ-NameServer"},
    @{Name="Nacos"; Port=8848; Script="D:\nacos\nacos\bin\startup.cmd"; Args="-m standalone"; Dir="D:\nacos\nacos"},
    @{Name="Seata"; Port=8091; Script="D:\seata\apache-seata-2.6.0-incubating-bin\seata-server\bin\seata-server.bat"; Dir="D:\seata\apache-seata-2.6.0-incubating-bin\seata-server"},
    @{Name="Sentinel"; Port=8089; Cmd="java -Dserver.port=8089 -Dcsp.sentinel.dashboard.server=localhost:8089 -jar D:\sentinel-dashboard\sentinel.jar"},
    @{Name="Nginx"; Port=80; Script="D:\Nginx\nginx.exe"; Dir="D:\Nginx"},
    @{Name="Tomcat"; Port=8080; Script="D:\Tomcat9\apache-tomcat-9.0.118\bin\startup.bat"; Dir="D:\Tomcat9\apache-tomcat-9.0.118"},
    @{Name="Elasticsearch"; Port=9200; Script="D:\ElasticSearch\elasticsearch-9.4.2\bin\elasticsearch.bat"; Dir="D:\ElasticSearch\elasticsearch-9.4.2"}
)

$autoRunning = @(3306, 6379, 27017, 5672)

function Test-Port {
    param($Port)
    return Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -WarningAction SilentlyContinue -InformationLevel Quiet
}

if ($status) {
    Write-Output "========================================"
    Write-Output "  DEV SERVICES STATUS"
    Write-Output "========================================"
    $autoNames = @{3306="MySQL"; 6379="Redis"; 27017="MongoDB"; 5672="RabbitMQ"}
    foreach ($port in $autoRunning) {
        $running = Test-Port $port
        $icon = if ($running) { "[OK]" } else { "[--]" }
        Write-Output "$icon $($autoNames[$port]) (port $port) [auto-start]"
    }
    foreach ($svc in $services) {
        $running = Test-Port $svc.Port
        $icon = if ($running) { "[OK]" } else { "[--]" }
        Write-Output "$icon $($svc.Name) (port $($svc.Port))"
    }
    Write-Output "========================================"
    exit
}

if ($stop) {
    Write-Output "Stopping all dev services..."
    Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force
    Get-Process -Name "nginx" -ErrorAction SilentlyContinue | Stop-Process -Force
    Write-Output "Done"
    exit
}

Write-Output "========================================"
Write-Output "  Starting Dev Services..."
Write-Output "========================================"

$started = @{}
foreach ($svc in $services) {
    if (Test-Port $svc.Port) {
        Write-Output "[SKIP] $($svc.Name) already running on port $($svc.Port)"
        $started[$svc.Name] = $true
        continue
    }
    Write-Output "[START] $($svc.Name)..."
    if ($svc.Cmd) {
        Start-Process -FilePath "cmd.exe" -ArgumentList "/c $($svc.Cmd)" -WindowStyle Minimized
    } elseif ($svc.Args) {
        Start-Process -FilePath $svc.Script -ArgumentList $svc.Args -WorkingDirectory $svc.Dir -WindowStyle Minimized
    } else {
        Start-Process -FilePath $svc.Script -WorkingDirectory $svc.Dir -WindowStyle Minimized
    }
    $started[$svc.Name] = $true
    Start-Sleep -Seconds 3
}

Write-Output ""
Write-Output "Waiting for services to be ready..."
Start-Sleep -Seconds 8

Write-Output ""
Write-Output "========================================"
Write-Output "  FINAL STATUS"
Write-Output "========================================"
$allOk = $true
$autoNames = @{3306="MySQL"; 6379="Redis"; 27017="MongoDB"; 5672="RabbitMQ"}
foreach ($port in $autoRunning) {
    $running = Test-Port $port
    $icon = if ($running) { "[OK]" } else { "[--]" }
    Write-Output "$icon $($autoNames[$port]) (port $port)"
}
foreach ($svc in $services) {
    $running = Test-Port $svc.Port
    $icon = if ($running) { "[OK]" } else { "[--]" }
    Write-Output "$icon $($svc.Name) (port $($svc.Port))"
    if (-not $running) { $allOk = $false }
}
Write-Output "========================================"
if ($allOk) { Write-Output "ALL SERVICES RUNNING!" }
else { Write-Output "Some services still starting — recheck with -status" }
