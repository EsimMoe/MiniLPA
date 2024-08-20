#!/usr/bin/env pwsh
param(
    [String]$Path,
    [String]$ProductCode
)

$WindowsInstaller = New-Object -com WindowsInstaller.Installer
$Database = $WindowsInstaller.OpenDatabase($Path, 2)
$Query = "UPDATE Property SET Property.Value='$ProductCode' WHERE Property.Property='ProductCode'"
$View = $Database.OpenView($Query)
$View.Execute()
$Database.Commit()
$View.Close()

$View = $null
$Database = $null
[System.GC]::Collect()
[System.GC]::WaitForPendingFinalizers()