<# Builds the distributable and optionally checks fresh generation plus reload in a private server world. #>
param(
    [switch]$Smoke,
    [switch]$Offline,
    [switch]$NoDaemon,
    [switch]$Benchmark,
    [switch]$Extended,
    [switch]$Profile,
    [string]$JavaHome,
    [long]$Seed = 123456789
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previousJavaHome = $env:JAVA_HOME
$previousGradleHome = $env:GRADLE_USER_HOME
Push-Location $projectRoot
try {
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user-home' }
    $gradleArguments = @('--console=plain')
    if ($NoDaemon) { $gradleArguments += '--no-daemon' }
    if ($Offline) { $gradleArguments += '--offline' }
    $buildTasks = @('build')
    if ($Extended -or $Smoke) { $buildTasks += ':terrain-core:extendedCheck' }
    & .\gradlew.bat @buildTasks @gradleArguments
    if ($LASTEXITCODE -ne 0) { throw 'Build or regression tests failed.' }
    if ($Profile) {
        & .\gradlew.bat :terrain-core:profile @gradleArguments
        if ($LASTEXITCODE -ne 0) { throw 'Core profiling failed.' }
    }
    if ($Benchmark) {
        & .\gradlew.bat :terrain-core:benchmark :terrain-core:componentBenchmark @gradleArguments
        if ($LASTEXITCODE -ne 0) { throw 'Core benchmark failed.' }
    }
    if ($Smoke) {
        $fixtureDirectory = Join-Path $projectRoot ('build/smoke-' + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $fixtureDirectory | Out-Null
        # A fresh directory makes this repeatable without deleting any existing worlds.
        @"
level-seed=$Seed
level-name=world
server-ip=127.0.0.1
server-port=0
online-mode=false
view-distance=2
simulation-distance=2
spawn-protection=0
max-tick-time=120000
"@ | Set-Content -LiteralPath (Join-Path $fixtureDirectory 'server.properties') -Encoding ascii
        for ($run = 0; $run -lt 2; $run++) {
            [string[]]$benchmarkArguments = @()
            if ($Benchmark) { $benchmarkArguments += '-PbenchmarkGeneration' }
            if ($Profile -and $run -eq 0) { $benchmarkArguments += '-PprofileGeneration' }
            & .\gradlew.bat runSmokeServer "-PsmokeDirectory=$fixtureDirectory" @benchmarkArguments @gradleArguments
            if ($LASTEXITCODE -ne 0) { throw "Server smoke run $run failed. Inspect $fixtureDirectory/logs/latest.log" }
            $report = Get-Content -LiteralPath (Join-Path $fixtureDirectory 'newdawn-smoke-result.txt') -Raw
            $expectedReload = if ($run -eq 1) { 'reload=true' } else { 'reload=false' }
            if (-not $report.StartsWith('PASS ') -or -not $report.Contains($expectedReload)) {
                throw "Missing successful smoke evidence for $expectedReload in $fixtureDirectory"
            }
            Write-Output $report
        }
    }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:GRADLE_USER_HOME = $previousGradleHome
    Pop-Location
}
