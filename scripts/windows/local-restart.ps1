$ErrorActionPreference = "Stop"

$ScriptDir = $PSScriptRoot
& (Join-Path $ScriptDir "local-down.ps1")
& (Join-Path $ScriptDir "local-up.ps1")
