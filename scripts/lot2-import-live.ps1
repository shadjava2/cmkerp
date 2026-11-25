# Lot 2 — import sélectif Services / Imputations / Locations via API (CLINIQUE → MySQL).
# Prérequis : gateway UP sur le port configuré, .env chargé, WRITE_ENTITIES=TCONTRAT inchangé.
param(
  [string]$GatewayBase = "http://127.0.0.1:8999/cmkerp-gateway",
  [string]$Username = "schadrack",
  [long]$UserId = 1,
  [long]$RoleId = 5
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Get-Content (Join-Path $root ".env") | ForEach-Object {
  $line = $_.Trim()
  if (-not $line -or $line.StartsWith("#")) { return }
  $i = $line.IndexOf("=")
  if ($i -lt 1) { return }
  $k = $line.Substring(0, $i).Trim()
  $v = $line.Substring($i + 1).Trim().Trim('"').Trim("'")
  Set-Item -Path "Env:$k" -Value $v
}

$secret = $env:CMK_JWT_SECRET
if ([string]::IsNullOrWhiteSpace($secret)) {
  throw "CMK_JWT_SECRET manquant dans .env"
}

function ConvertTo-Base64Url([byte[]]$bytes) {
  return [Convert]::ToBase64String($bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

function New-AccessToken {
  $headerJson = '{"alg":"HS256","typ":"JWT"}'
  $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
  $payloadObj = [ordered]@{
    userId   = $UserId
    roleId   = $RoleId
    username = $Username
    locked   = $false
    sub      = $Username
    iat      = $now
    exp      = $now + 3600
  }
  $payloadJson = ($payloadObj | ConvertTo-Json -Compress)
  $headerB64 = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($headerJson))
  $payloadB64 = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payloadJson))
  $data = [Text.Encoding]::UTF8.GetBytes("$headerB64.$payloadB64")
  $hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($secret))
  $sig = ConvertTo-Base64Url ($hmac.ComputeHash($data))
  return "$headerB64.$payloadB64.$sig"
}

$token = New-AccessToken
$headers = @{ Authorization = "Bearer $token" }

function Import-Referential([string]$key) {
  Write-Host "=== $key : candidats ==="
  $cand = Invoke-RestMethod -Headers $headers -Uri "$GatewayBase/api/v1/facturation/referentials/$key/import/candidates?page=0&size=100"
  $available = [int]$cand.available
  $sourceTotal = [int]$cand.sourceTotal
  Write-Host ("sourceTotal={0} available={1} already={2} divergences={3}" -f $sourceTotal, $available, $cand.alreadyImported, $cand.divergences)
  if ($available -le 0) {
    Write-Host "Rien à importer."
    return
  }
  $ids = @()
  foreach ($item in @($cand.candidates)) {
    if ($item.id) { $ids += [string]$item.id }
    elseif ($item.key) { $ids += [string]$item.key }
    elseif ($item.code) { $ids += [string]$item.code }
  }
  # Si la page ne couvre pas tout, recharger par lots
  $page = 0
  while ($ids.Count -lt $available -and $page -lt 20) {
    $page++
    $more = Invoke-RestMethod -Headers $headers -Uri "$GatewayBase/api/v1/facturation/referentials/$key/import/candidates?page=$page&size=100"
    foreach ($item in @($more.candidates)) {
      $v = if ($item.id) { [string]$item.id } elseif ($item.key) { [string]$item.key } else { [string]$item.code }
      if ($v -and ($ids -notcontains $v)) { $ids += $v }
    }
  }
  Write-Host ("Sélection de {0} clé(s)…" -f $ids.Count)
  # IMPORT_SELECTION_LIMIT = 200 — découper si besoin
  $imported = 0
  $skipped = 0
  $failed = 0
  for ($i = 0; $i -lt $ids.Count; $i += 200) {
    $chunk = $ids[$i..([Math]::Min($i + 199, $ids.Count - 1))]
    $body = @{ codes = @($chunk) } | ConvertTo-Json
    $result = Invoke-RestMethod -Method Post -Headers $headers -ContentType "application/json" `
      -Uri "$GatewayBase/api/v1/facturation/referentials/$key/import/selected" -Body $body
    $imported += [int]$result.imported
    $skipped += [int]$result.skipped
    $failed += [int]$result.failed
  }
  Write-Host ("Résultat $key : imported={0} skipped={1} failed={2}" -f $imported, $skipped, $failed)
}

foreach ($ref in @("services", "imputations", "locations")) {
  Import-Referential $ref
}

Write-Host "=== listes ERP ==="
foreach ($ref in @("services", "imputations", "locations")) {
  $list = Invoke-RestMethod -Headers $headers -Uri "$GatewayBase/api/v1/facturation/referentials/$ref?page=0&size=5"
  Write-Host ("{0}: total={1}" -f $ref, $list.total)
}
