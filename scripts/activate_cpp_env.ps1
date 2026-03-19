$gccBin = "D:\yinyue\tools\winlibs-gcc\mingw64\bin"

if (-not (Test-Path $gccBin)) {
    Write-Error "GCC bin directory not found: $gccBin"
    exit 1
}

$env:Path = "$gccBin;$env:Path"

Write-Host "C++ toolchain activated."
Write-Host "g++ path: $(Get-Command g++ | Select-Object -ExpandProperty Source)"
