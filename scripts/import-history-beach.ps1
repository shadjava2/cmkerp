# Import historique Beach -> MySQL (lecture SQL Server, écriture MySQL, aucun outbox).
param(
  [ValidateSet("prestations", "medicaments")]
  [string]$Entity = "prestations",
  [int]$Offset = 2500,
  [int]$Batches = 20,
  [int]$BatchSize = 500
)

$ErrorActionPreference = "Continue"
Add-Type -AssemblyName System.Data

$beachCs = "Server=192.168.100.254;Database=CLINIQUE;User Id=clinic;Password=CMK@2020;Encrypt=false;TrustServerCertificate=true;Connection Timeout=30"
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$dbArgs = @("-h", "127.0.0.1", "-P", "32768", "-u", "shad", "-pSDconceptsrdc@243", "cmkerp-v24prod", "--connect-timeout=30")

function Esc([object]$v) {
  if ($null -eq $v -or $v -is [DBNull]) { return "NULL" }
  if ($v -is [datetime]) { return "'" + ($v.ToString("yyyy-MM-dd HH:mm:ss")) + "'" }
  if ($v -is [decimal] -or $v -is [double] -or $v -is [single] -or $v -is [int] -or $v -is [long] -or $v -is [byte] -or $v -is [int16] -or $v -is [int64]) {
    return ([string]$v).Replace(",", ".")
  }
  $t = [string]$v
  return "'" + $t.Replace("\", "\\").Replace("'", "''") + "'"
}

function Mysql([string]$sql) {
  $out = cmd /c "`"$mysql`" -h127.0.0.1 -P32768 -ushad -pSDconceptsrdc@243 cmkerp-v24prod -N -e `"$sql`" 2>nul"
  return $out
}

$conn = New-Object System.Data.SqlClient.SqlConnection $beachCs
$conn.Open()
$totalIns = 0
$off = $Offset

for ($b = 0; $b -lt $Batches; $b++) {
  $cmd = $conn.CreateCommand()
  $cmd.CommandTimeout = 120
  if ($Entity -eq "prestations") {
    $cmd.CommandText = @"
SELECT IDMVT, DATEPRES, MATRICULE, CODEPRES, LIBPRES, MONTANT, CODEMED, CODESERV, CODESOC, CONTRASOC, IMPUTATION, LOCATION, UTILISATEUR, AFACT, SERIE, K, CODECMK, SOURCE, NDATE, NOM
FROM dbo.TPRESTATION
ORDER BY IDMVT
OFFSET $off ROWS FETCH NEXT $BatchSize ROWS ONLY
"@
  } else {
    $cmd.CommandText = @"
SELECT IDM, DATEMVT, MATRICULE, CODEPROD, DESIGNATION, QTE, UNITGEST, PAU, MONTANT, CODEMED, CODESERV, CODESOC, CONTRASOC, CODEPHARM, LOCATION, UTILISATEUR, AFACT, SOURCE, NDATE, NOM
FROM dbo.TMEDICAMENT
ORDER BY IDM
OFFSET $off ROWS FETCH NEXT $BatchSize ROWS ONLY
"@
  }

  $da = New-Object System.Data.SqlClient.SqlDataAdapter $cmd
  $dt = New-Object System.Data.DataTable
  [void]$da.Fill($dt)
  if ($dt.Rows.Count -eq 0) {
    Write-Host "Fin de source a offset=$off"
    break
  }

  $values = New-Object System.Collections.Generic.List[string]
  foreach ($r in $dt.Rows) {
    if ($Entity -eq "prestations") {
      $legacy = [string]$r["IDMVT"]
      $mat = $r["MATRICULE"]; $codepres = $r["CODEPRES"]; $montant = $r["MONTANT"]; $datep = $r["DATEPRES"]
      $codemed = $r["CODEMED"]; $codeserv = $r["CODESERV"]; $codesoc = $r["CODESOC"]; $contrat = $r["CONTRASOC"]
      $imput = $r["IMPUTATION"]; $loc = $r["LOCATION"]
      $snap = (@{ IDMVT = $legacy; MATRICULE = "$mat"; CODEPRES = "$codepres"; MONTANT = "$montant"; DATEPRES = "$datep" } | ConvertTo-Json -Compress)
      $values.Add("(" + (Esc $legacy) + "," + (Esc ("HIST-PREST-" + $legacy)) + "," + (Esc $legacy) +
        ",(SELECT id FROM erp_ayants_droit WHERE matricule=" + (Esc $mat) + " ORDER BY id LIMIT 1)," + (Esc $mat) +
        ",(SELECT id FROM erp_societes WHERE code=" + (Esc $codesoc) + " LIMIT 1)," +
        "(SELECT id FROM erp_contrats WHERE code=" + (Esc $contrat) + " LIMIT 1)," +
        "(SELECT id FROM erp_tarifs WHERE code=" + (Esc $codepres) + " LIMIT 1)," + (Esc $codepres) +
        ",(SELECT id FROM erp_services WHERE code=" + (Esc $codeserv) + " LIMIT 1)," +
        "(SELECT id FROM erp_imputations WHERE code=" + (Esc $imput) + " LIMIT 1)," +
        "(SELECT id FROM erp_medecins WHERE code=" + (Esc $codemed) + " LIMIT 1)," + (Esc $codemed) +
        ",(SELECT id FROM erp_locations WHERE code=" + (Esc $loc) + " LIMIT 1)," +
        (Esc $montant) + "," + (Esc $datep) + "," + (Esc $snap) +
        ",1,'CLINIQUE_BEACH','SYNCED'," + (Esc ("fp-" + $legacy)) + ",NOW(),1)")
    } else {
      $legacy = [string]$r["IDM"]
      $mat = $r["MATRICULE"]; $prod = $r["CODEPROD"]; $pharm = $r["CODEPHARM"]; $montant = $r["MONTANT"]
      $qte = $r["QTE"]; $datem = $r["DATEMVT"]; $codemed = $r["CODEMED"]; $codeserv = $r["CODESERV"]
      $codesoc = $r["CODESOC"]; $contrat = $r["CONTRASOC"]; $loc = $r["LOCATION"]
      $snap = (@{ IDM = $legacy; MATRICULE = "$mat"; CODEPROD = "$prod"; MONTANT = "$montant"; DATEMVT = "$datem" } | ConvertTo-Json -Compress)
      $values.Add("(" + (Esc $legacy) + "," + (Esc ("HIST-MED-" + $legacy)) +
        ",(SELECT id FROM erp_ayants_droit WHERE matricule=" + (Esc $mat) + " ORDER BY id LIMIT 1)," + (Esc $mat) +
        ",(SELECT id FROM erp_societes WHERE code=" + (Esc $codesoc) + " LIMIT 1)," +
        "(SELECT id FROM erp_contrats WHERE code=" + (Esc $contrat) + " LIMIT 1)," +
        "(SELECT erp_product_id FROM erp_product_external_mappings WHERE legacy_product_code=" + (Esc $prod) + " AND erp_product_id IS NOT NULL ORDER BY id LIMIT 1)," + (Esc $prod) +
        ",(SELECT id FROM erp_medecins WHERE code=" + (Esc $codemed) + " LIMIT 1)," + (Esc $codemed) +
        ",(SELECT erp_pharmacy_id FROM erp_pharmacy_external_mappings WHERE legacy_pharmacy_code=" + (Esc $pharm) + " AND erp_pharmacy_id IS NOT NULL ORDER BY id LIMIT 1)," + (Esc $pharm) +
        ",(SELECT id FROM erp_services WHERE code=" + (Esc $codeserv) + " LIMIT 1)," +
        "(SELECT id FROM erp_locations WHERE code=" + (Esc $loc) + " LIMIT 1)," +
        (Esc $qte) + "," + (Esc $montant) + "," + (Esc $datem) + "," + (Esc $snap) +
        ",1,'CLINIQUE_BEACH','SYNCED'," + (Esc ("fp-" + $legacy)) + ",NOW(),1)")
    }
  }

  if ($Entity -eq "prestations") {
    $sql = @"
INSERT INTO erp_facturation_prestations
  (legacy_id, operation_id, legacy_idmvt, fk_ayant_droit, legacy_matricule, fk_societe, fk_contrat,
   fk_tarif, legacy_tarif_code, fk_service, fk_imputation, fk_medecin, legacy_medecin_code,
   fk_location, montant, date_prestation, payload_snapshot_json, actif, source_system,
   sync_status, sync_fingerprint, last_synced_at, version)
VALUES
$($values -join ",`n")
ON DUPLICATE KEY UPDATE
  montant = VALUES(montant),
  date_prestation = VALUES(date_prestation),
  payload_snapshot_json = VALUES(payload_snapshot_json),
  sync_fingerprint = VALUES(sync_fingerprint),
  last_synced_at = NOW(),
  version = version + 1;
"@
  } else {
    $sql = @"
INSERT INTO erp_facturation_medicaments
  (legacy_id, operation_id, fk_ayant_droit, legacy_matricule, fk_societe, fk_contrat,
   fk_product, legacy_product_code, fk_medecin, legacy_medecin_code, fk_pharmacy,
   legacy_pharmacy_code, fk_service, fk_location, quantite, montant, date_mouvement,
   payload_snapshot_json, actif, source_system, sync_status, sync_fingerprint,
   last_synced_at, version)
VALUES
$($values -join ",`n")
ON DUPLICATE KEY UPDATE
  quantite = VALUES(quantite),
  montant = VALUES(montant),
  date_mouvement = VALUES(date_mouvement),
  payload_snapshot_json = VALUES(payload_snapshot_json),
  sync_fingerprint = VALUES(sync_fingerprint),
  last_synced_at = NOW(),
  version = version + 1;
"@
  }

  $tmp = Join-Path $env:TEMP ("hist-import-" + $Entity + "-" + $off + ".sql")
  # ASCII without BOM — mysql source is picky with UTF-8 BOM
  [System.IO.File]::WriteAllText($tmp, $sql, [System.Text.UTF8Encoding]::new($false))
  $null = cmd /c "`"$mysql`" -h127.0.0.1 -P32768 -ushad -pSDconceptsrdc@243 cmkerp-v24prod --default-character-set=utf8mb4 < `"$tmp`" 2>nul"
  if ($LASTEXITCODE -ne 0) {
    throw "MySQL import failed at offset=$off exit=$LASTEXITCODE file=$tmp"
  }

  $totalIns += $dt.Rows.Count
  Write-Host ("{0} offset={1} rows={2} cumulative={3}" -f $Entity, $off, $dt.Rows.Count, $totalIns)
  $off += $BatchSize
}

$conn.Close()
if ($Entity -eq "prestations") {
  $count = Mysql "SELECT COUNT(*), ROUND(COALESCE(SUM(montant),0),2) FROM erp_facturation_prestations"
} else {
  $count = Mysql "SELECT COUNT(*), ROUND(COALESCE(SUM(montant),0),2) FROM erp_facturation_medicaments"
}
Write-Host "DONE entity=$Entity nextOffset=$off mysql=$count"
