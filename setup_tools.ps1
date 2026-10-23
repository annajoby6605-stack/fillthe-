$toolsDir = Join-Path $env:USERPROFILE ".tools"

if (!(Test-Path $toolsDir)) {
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
}

$jdkZip = Join-Path $toolsDir "openjdk-17.zip"
$jdkTarget = Join-Path $toolsDir "jdk-17"

if (!(Test-Path (Join-Path $jdkTarget "bin\java.exe"))) {
    Write-Host "Downloading OpenJDK 17..."

    curl.exe -fL "https://download.visualstudio.microsoft.com/download/pr/a9672056-0a4d-425d-8488-f481ee56319c/ac8d1216f7d03a27c018bc960f947721/microsoft-jdk-17.0.20.1-windows-x64.zip" -o $jdkZip

    if ($LASTEXITCODE -ne 0) {
        throw "Java download failed. Please share the error."
    }

    Write-Host "Extracting OpenJDK 17..."
    $extractTmp = Join-Path $toolsDir "jdk-tmp"

    if (Test-Path $extractTmp) {
        Remove-Item $extractTmp -Recurse -Force
    }

    Expand-Archive -Path $jdkZip -DestinationPath $extractTmp -Force

    $firstFolder = Get-ChildItem -Path $extractTmp -Directory |
        Select-Object -First 1

    if (!$firstFolder) {
        throw "Java archive extraction failed."
    }

    if (Test-Path $jdkTarget) {
        Remove-Item $jdkTarget -Recurse -Force
    }

    Move-Item -Path $firstFolder.FullName -Destination $jdkTarget -Force
    Remove-Item $extractTmp -Recurse -Force
    Remove-Item $jdkZip -Force
}

$mvnZip = Join-Path $toolsDir "apache-maven.zip"
$mvnTarget = Join-Path $toolsDir "maven"

if (!(Test-Path (Join-Path $mvnTarget "bin\mvn.cmd"))) {
    Write-Host "Downloading Maven..."

    curl.exe -fL "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -o $mvnZip

    if ($LASTEXITCODE -ne 0) {
        throw "Maven download failed. Please share the error."
    }

    Write-Host "Extracting Maven..."
    $extractMvnTmp = Join-Path $toolsDir "mvn-tmp"

    if (Test-Path $extractMvnTmp) {
        Remove-Item $extractMvnTmp -Recurse -Force
    }

    Expand-Archive -Path $mvnZip -DestinationPath $extractMvnTmp -Force

    $firstMvnFolder = Get-ChildItem -Path $extractMvnTmp -Directory |
        Select-Object -First 1

    if (!$firstMvnFolder) {
        throw "Maven archive extraction failed."
    }

    if (Test-Path $mvnTarget) {
        Remove-Item $mvnTarget -Recurse -Force
    }

    Move-Item -Path $firstMvnFolder.FullName -Destination $mvnTarget -Force
    Remove-Item $extractMvnTmp -Recurse -Force
    Remove-Item $mvnZip -Force
}

Write-Host "`nTesting Java..."
& "$jdkTarget\bin\java.exe" -version

if ($LASTEXITCODE -ne 0) {
    throw "Java verification failed."
}

Write-Host "`nTesting Maven..."
$env:JAVA_HOME = $jdkTarget
& "$mvnTarget\bin\mvn.cmd" -version

if ($LASTEXITCODE -ne 0) {
    throw "Maven verification failed."
}

Write-Host "`nJava and Maven setup completed."
Write-Host "Java location: $jdkTarget"
Write-Host "Maven location: $mvnTarget"
