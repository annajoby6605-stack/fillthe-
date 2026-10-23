$toolsDir = Join-Path $env:USERPROFILE ".tools"
$jdkDir = Join-Path $toolsDir "jdk-17"
$mvnDir = Join-Path $toolsDir "maven"

[Environment]::SetEnvironmentVariable('JAVA_HOME', $jdkDir, 'User')
[Environment]::SetEnvironmentVariable('M2_HOME', $mvnDir, 'User')
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$toAdd = "$jdkDir\bin;$mvnDir\bin"
if ($userPath -notlike '*jdk-17*') {
    [Environment]::SetEnvironmentVariable('Path', "$toAdd;$userPath", 'User')
}
$env:JAVA_HOME = $jdkDir
$env:M2_HOME = $mvnDir
$env:Path = "$toAdd;$env:Path"
Write-Host "Checking Java:"
java -version
Write-Host "Checking Maven:"
mvn -version
