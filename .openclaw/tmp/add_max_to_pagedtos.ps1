# Add @Max(100) to all Page DTOs' pageSize field (encoding-safe)
$root = "D:\我的项目\Myblog\Myblog_dev\src\main\java\com\jiuliu\myblog_dev\dto"
$files = Get-ChildItem $root -Recurse -Filter "Page*.java" | Where-Object { $_.Name -notmatch "Response" }
$maxMsg = [string]::Join('', @(0x6BCF, 0x9875, 0x6570, 0x91CF, 0x4E0D, 0x80FD, 0x8D85, 0x8FC7, 0x31, 0x30, 0x30)) # 每页数量不能超过100
$utf8 = New-Object System.Text.UTF8Encoding($false)
$utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)
$gbk = [System.Text.Encoding]::GetEncoding(936)

foreach ($f in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($f.FullName)
    $isUtf8 = $true
    try { $null = $utf8Strict.GetString($bytes) } catch { $isUtf8 = $false }
    $text = if ($isUtf8) { $utf8.GetString($bytes) } else { $gbk.GetString($bytes) }

    $changed = $false

    # 1. add @Max on pageSize
    $pattern = '(@Min\(value = 1, message = "[^"]*"\)\r?\n)(    private Integer pageSize;)'
    $newText = $text
    if ($newText -match $pattern) {
        $newText = [regex]::Replace($newText, $pattern, "`$1    @Max(value = 100, message = `"$maxMsg`")`n`$2", 1)
        $changed = $true
    }

    # 2. ensure Max import
    if ($newText -match 'import jakarta.validation.constraints.Min;' -and $newText -notmatch 'import jakarta.validation.constraints.Max;') {
        $newText = $newText -replace 'import jakarta.validation.constraints.Min;', "import jakarta.validation.constraints.Min;`nimport jakarta.validation.constraints.Max;"
        $changed = $true
    }

    if ($changed) {
        $outBytes = if ($isUtf8) { $utf8.GetBytes($newText) } else { $gbk.GetBytes($newText) }
        [System.IO.File]::WriteAllBytes($f.FullName, $outBytes)
        Write-Output ("UPDATED: " + $f.Name + " (utf8=" + $isUtf8 + ")")
    } else {
        Write-Output ("SKIP (no pageSize or already has Max): " + $f.Name)
    }
}
