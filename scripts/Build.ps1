#!/usr/bin/env pwsh
param(
    [Switch]$NativeExecutable,

    [ValidateSet('app-image', 'exe', 'msi', 'rpm', 'deb', 'pkg', 'dmg')]
    [String]$NativeExecutableType=$null,

    [Switch]$NativeWayland,

    [Switch]$SkipSetupResources,

    [String]$GithubToken
)

$ErrorActionPreference = 'Stop'

$ProjectPath = Resolve-Path "$PSScriptRoot/../"

$GradlewPath = Resolve-Path "$ProjectPath/gradlew"

if ($IsWindows)
{
    $GradlewPath = "$GradlewPath.bat"
}

if ($PSVersionTable.Platform -eq "Unix")
{
    chmod +x $GradlewPath
}

$BuildArgument = @(if($NativeExecutable) { 'jpackage' } else { 'shadowJar' })

if ($NativeExecutable -and $NativeExecutableType)
{
    $BuildArgument += "-Ptype=$NativeExecutableType"
}

if ($NativeWayland)
{
    $BuildArgument += '-Pnative-wayland'
}

if ($SkipSetupResources)
{
    $BuildArgument += '-Pskip-setup-resources'
}
else
{
    & "$PSScriptRoot/Update-Euicc-Manifests.ps1" -CheckExists
}

if ($GithubToken)
{
    $BuildArgument += "-Pgithub-token=$GithubToken"
}

& {
    Set-Location $ProjectPath
    & $GradlewPath $BuildArgument --info --stacktrace
}

$DistFolderPath = "$ProjectPath/build/dist/"

$OS = if ($IsWindows) { 'Windows' }
elseif ($IsLinux) { 'Linux' }
elseif ($IsMacOS) { 'macOS' }
else { throw 'unknown os' }

$OSArchitecture = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture
$Arch = if ($OSArchitecture -eq 'X64') { 'x86_64' }
elseif ($OSArchitecture -eq 'Arm64') { 'aarch64' }
else { throw 'unkown arch' }

$Name = "MiniLPA-$OS-$Arch"

if ($NativeWayland)
{
    $Name += '-Wayland'
}

Get-ChildItem -Path $DistFolderPath -File -Filter 'MiniLPA*' | ForEach-Object { Move-Item -Path $_.FullName -Destination "$DistFolderPath$Name$($_.Extension)" -Force }