$toolsDir = "C:\Users\Anna Joby\.tools"
$dbDir = Join-Path $toolsDir "mariadb"
$zipFile = Join-Path $toolsDir "mariadb.zip"
$url = "https://archive.mariadb.org/mariadb-10.11.8/winx64-packages/mariadb-10.11.8-winx64.zip"

if (!(Test-Path (Join-Path $dbDir "bin\mysqld.exe"))) {
    Write-Host "Downloading MariaDB (MySQL-compatible portable)..."
    curl.exe -L $url -o $zipFile
    Write-Host "Extracting MariaDB..."
    $tmpExtract = Join-Path $toolsDir "mariadb-tmp"
    Expand-Archive -Path $zipFile -DestinationPath $tmpExtract -Force
    $firstFolder = Get-ChildItem -Path $tmpExtract -Directory | Select-Object -First 1
    if (Test-Path $dbDir) { Remove-Item -Path $dbDir -Recurse -Force }
    Move-Item -Path $firstFolder.FullName -Destination $dbDir -Force
    Remove-Item -Path $tmpExtract -Recurse -Force
    Remove-Item -Path $zipFile -Force
}

$dataDir = Join-Path $dbDir "data"
if (!(Test-Path (Join-Path $dataDir "mysql"))) {
    Write-Host "Initializing data directory..."
    & "$dbDir\bin\mysql_install_db.exe" "--datadir=$dataDir"
}

Write-Host "MariaDB setup complete!"

