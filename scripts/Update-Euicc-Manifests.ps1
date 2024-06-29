#!/usr/bin/env pwsh
param([Switch]$CheckExists)

$ProjectRootPath = "$PSScriptRoot/../"
$ResourcesPath = "$ProjectRootPath/src/main/resources/"
$EUMPath = "$ResourcesPath/eum-manifest.json"
$CIPath = "$ResourcesPath/ci-manifest.json"
$EuiccInfoUpdateTimePath = "$ProjectRootPath/build/euicc_info_update_time"

if (!$CheckExists -or (!(Test-Path $EUMPath) -or !(Test-Path $CIPath)) -or !(Test-Path $EuiccInfoUpdateTimePath))
{
    Invoke-WebRequest -Uri 'https://euicc-manual.osmocom.org/docs/pki/eum/manifest.json' -OutFile $EUMPath
    Invoke-WebRequest -Uri 'https://euicc-manual.osmocom.org/docs/pki/ci/manifest.json' -OutFile $CIPath
    $Timestamp = ([DateTimeOffset](Get-Date)).ToUnixTimeMilliseconds()
    New-Item $EuiccInfoUpdateTimePath -Force | Out-Null
    $Timestamp | Set-Content -Path "$ProjectRootPath/build/euicc_info_update_time" -NoNewline
}