# Libere le port 8999 (cmkerp-gateway) en arretant les processus qui occupent ce port.
param(
  [int]$Port = 8999,
  [switch]$Force,
  [switch]$Quiet
)

$ErrorActionPreference = "Stop"

function Write-Info([string]$msg) {
  if (-not $Quiet) { Write-Host $msg }
}

function Get-PortListeners([int]$listenPort) {
  $connections = Get-NetTCPConnection -LocalPort $listenPort -State Listen -ErrorAction SilentlyContinue
  if (-not $connections) { return @() }

  $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
  foreach ($procId in $pids) {
    $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
    [PSCustomObject]@{
      Port      = $listenPort
      PID       = $procId
      Name      = if ($proc) { $proc.ProcessName } else { "?" }
      Path      = if ($proc) { $proc.Path } else { $null }
      StartTime = if ($proc) { $proc.StartTime } else { $null }
    }
  }
}

$listeners = @(Get-PortListeners -listenPort $Port)

if ($listeners.Count -eq 0) {
  Write-Info "Port $Port libre - aucun processus a arreter."
  exit 0
}

Write-Info "Processus sur le port $Port :"
$listeners | Format-Table PID, Name, StartTime, Path -AutoSize | Out-String | ForEach-Object { Write-Info $_.TrimEnd() }

if (-not $Force) {
  $answer = Read-Host "Arreter ces processus ? (o/N)"
  if ($answer -notmatch "^(o|O|y|Y|yes|oui)$") {
    Write-Info "Annule."
    exit 1
  }
}

foreach ($item in $listeners) {
  try {
    Stop-Process -Id $item.PID -Force -ErrorAction Stop
    Write-Info "Arrete PID $($item.PID) ($($item.Name))."
  } catch {
    Write-Warning "Impossible de stopper PID $($item.PID): $($_.Exception.Message)"
  }
}

Start-Sleep -Milliseconds 500
$remaining = @(Get-PortListeners -listenPort $Port)
if ($remaining.Count -gt 0) {
  $pids = $remaining.PID -join ", "
  Write-Error "Le port $Port est encore occupe (PID: $pids)."
}

Write-Info "Port $Port libere."
