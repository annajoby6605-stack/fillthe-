$toolsDir = Join-Path $env:USERPROFILE ".tools"
$dbDir = Join-Path $toolsDir "mariadb"
$mysqld = Join-Path $dbDir "bin\mysqld.exe"
$mysql = Join-Path $dbDir "bin\mysql.exe"

$running = Get-Process -Name "mysqld" -ErrorAction SilentlyContinue
if (!$running) {
    Write-Host "Starting MySQL server..."
    Start-Process -FilePath $mysqld -ArgumentList "--console" -WindowStyle Hidden
    Start-Sleep -Seconds 3
} else {
    Write-Host "MySQL server is already running."
}

Write-Host "Testing connection and creating database..."
& $mysql -u root -e "CREATE DATABASE IF NOT EXISTS fill_the_pockets; SHOW DATABASES;"
