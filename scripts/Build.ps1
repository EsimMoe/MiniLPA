#!/usr/bin/env pwsh
param(
    [Switch]$NativeExecutable,

    [ValidateSet('app-image', 'exe', 'msi', 'rpm', 'deb', 'pkg', 'dmg')]
    [String]$NativeExecutableType,

    [Switch]$NativeWayland,

    [Switch]$SkipSetupResources,

    [String]$GithubToken
)

$ErrorActionPreference = 'Stop'

$ProjectRootPath = Resolve-Path "$PSScriptRoot/../"

$GradlewPath = Resolve-Path "$ProjectRootPath/gradlew"

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
    & "$PSScriptRoot/Update-Euicc-Info.ps1" -CheckExists
}

if ($GithubToken)
{
    $BuildArgument += "-Pgithub-token=$GithubToken"
}

git diff --no-ext-diff --quiet --exit-code
$IsDirty = $LASTEXITCODE
$ShortCommitId = git rev-parse --short HEAD
if ($IsDirty) { $ShortCommitId = "$ShortCommitId-dirty" }
$BuildArgument += "-Pshort-commit-id=$ShortCommitId"

Push-Location $ProjectRootPath
& $GradlewPath $BuildArgument --info --stacktrace
Pop-Location

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($NativeExecutable)
{
    $DistFolderPath = "$ProjectRootPath/build/dist/"

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

    if ($NativeExecutableType -eq 'app-image')
    {
        $AppImageFolderPath = "$DistFolderPath/MiniLPA*"
        Compress-Archive -Path "$AppImageFolderPath/*" -DestinationPath "$DistFolderPath/$Name.zip" -Force
        Get-ChildItem -Path $DistFolderPath -Directory | Where-Object { $_.Name -like 'MiniLPA*' } | Remove-Item -Recurse -Force
    }
    else
    {
        Get-ChildItem -Path $DistFolderPath -File -Filter "MiniLPA*$NativeExecutableType" | ForEach-Object {
            $DistPath = "$DistFolderPath$Name$( $_.Extension )"
            Move-Item -Path $_.FullName -Destination $DistPath -Force
            if ($NativeExecutableType -eq "msi")
            {
                if ($IsDirty) { $Guid = [System.Guid]::NewGuid() }
                else
                {
                    $CommitId = git rev-parse HEAD
                    $CommitIdBytes = [System.Text.Encoding]::UTF8.GetBytes($CommitId)
                    $GuidBytes = $CommitIdBytes[0..15] -as [Byte[]]
                    $Guid = [System.Guid]::new($guidBytes)
                }
                & "$PSScriptRoot/Update-MSI-ProductCode.ps1" -Path $DistPath -ProductCode "{$($Guid.Guid)}"
            }
        }
    }
}