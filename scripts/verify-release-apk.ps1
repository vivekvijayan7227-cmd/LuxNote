[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $ApkPath,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^(?:[A-Fa-f0-9]{64}|(?:[A-Fa-f0-9]{2}:){31}[A-Fa-f0-9]{2})$')]
    [string] $ExpectedCertificateSha256,

    [Parameter(Mandatory = $false)]
    [string] $ApkSignerPath
)

$ErrorActionPreference = 'Stop'

function Normalize-Sha256([string] $Value) {
    return ($Value -replace ':', '' -replace '\s', '').ToUpperInvariant()
}

if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
    throw "APK file not found: $ApkPath"
}
$ApkPath = (Resolve-Path -LiteralPath $ApkPath).Path

if ([string]::IsNullOrWhiteSpace($ApkSignerPath)) {
    if ($env:ANDROID_HOME) {
        $candidate = Get-ChildItem -Path (Join-Path $env:ANDROID_HOME 'build-tools') -Filter apksigner.bat -Recurse -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1
        if ($candidate) { $ApkSignerPath = $candidate.FullName }
    }
    if (-not $ApkSignerPath -and $env:LOCALAPPDATA) {
        $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk\build-tools'
        $candidate = Get-ChildItem -Path $sdk -Filter apksigner.bat -Recurse -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1
        if ($candidate) { $ApkSignerPath = $candidate.FullName }
    }
}

if ([string]::IsNullOrWhiteSpace($ApkSignerPath) -or -not (Test-Path -LiteralPath $ApkSignerPath -PathType Leaf)) {
    throw 'apksigner not found. Production verification must not be skipped.'
}

Write-Host '1/3 Verifying APK signature integrity...'
$verifyOutput = & $ApkSignerPath verify --verbose --print-certs $ApkPath 2>&1
if ($LASTEXITCODE -ne 0) {
    $verifyOutput | ForEach-Object { Write-Host $_ }
    throw "apksigner verification failed with exit code $LASTEXITCODE."
}
$verifyOutput | ForEach-Object { Write-Host $_ }

Write-Host '2/3 Checking production signing certificate fingerprint...'
$joined = $verifyOutput -join "`n"
# apksigner prints a line: Signer #1 certificate SHA-256 digest: <hex>
$matches = [regex]::Matches($joined, '(?im)certificate SHA-256 digest:\s*([A-Fa-f0-9:]+)')
if ($matches.Count -lt 1) {
    throw 'Could not extract signer certificate SHA-256 digest from apksigner output.'
}
$expected = Normalize-Sha256 $ExpectedCertificateSha256
$observed = Normalize-Sha256 $matches[0].Groups[1].Value
if ($observed -ne $expected) {
    throw "Unexpected signing certificate. Expected $expected; observed $observed."
}

Write-Host '3/3 Computing checksum of the exact verified APK...'
$hash = Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256
$checksumPath = "$ApkPath.sha256"
"$($hash.Hash.ToUpperInvariant())  $(Split-Path -Leaf $ApkPath)" | Set-Content -LiteralPath $checksumPath -Encoding ascii
Write-Host "APK:        $ApkPath"
Write-Host "Signer:     $observed"
Write-Host "SHA-256:    $($hash.Hash.ToUpperInvariant())"
Write-Host "Checksum:   $checksumPath"
Write-Host 'PASS: signature integrity and production certificate identity both verified.'
