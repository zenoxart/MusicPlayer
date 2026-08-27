Add-Type -AssemblyName System.Drawing

function New-RoundedRectPath {
    param([single]$x, [single]$y, [single]$w, [single]$h, [single]$radius)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $radius * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-NoteIconBitmap {
    param([int]$size)

    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $bmp.SetResolution(96, 96)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $bgColor = [System.Drawing.Color]::FromArgb(255, 18, 18, 18)
    $radius = $size * 0.22
    $bgPath = New-RoundedRectPath 0 0 $size $size $radius
    $g.FillPath((New-Object System.Drawing.SolidBrush $bgColor), $bgPath)

    $noteColor = [System.Drawing.Color]::FromArgb(255, 29, 185, 84)
    $noteBrush = New-Object System.Drawing.SolidBrush $noteColor
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    # Draw an eighth-note glyph manually so it stays crisp at every size.
    $cx = $size * 0.5
    $headRX = $size * 0.15
    $headRY = $size * 0.115
    $headY = $size * 0.72
    $stemW = $size * 0.055
    $stemTopY = $size * 0.14
    $headX = $cx - ($size * 0.08)

    # Note head (slightly rotated ellipse)
    $state = $g.Save()
    $g.TranslateTransform($headX, $headY)
    $g.RotateTransform(-18)
    $g.FillEllipse($noteBrush, -$headRX, -$headRY, $headRX * 2, $headRY * 2)
    $g.Restore($state)

    # Stem
    $stemX = $headX + $headRX - ($stemW * 0.3)
    $stemRect = New-Object System.Drawing.RectangleF ($stemX), ($stemTopY), $stemW, ($headY - $stemTopY)
    $g.FillRectangle($noteBrush, $stemRect)

    # Flag
    $flagPath = New-Object System.Drawing.Drawing2D.GraphicsPath
    $flagX = $stemX + $stemW
    $flagPath.AddBezier(
        [single]$flagX, [single]$stemTopY,
        [single]($flagX + $size * 0.30), [single]($stemTopY + $size * 0.02),
        [single]($flagX + $size * 0.26), [single]($stemTopY + $size * 0.22),
        [single]$flagX, [single]($stemTopY + $size * 0.30)
    )
    $flagPath.AddBezier(
        [single]$flagX, [single]($stemTopY + $size * 0.30),
        [single]($flagX + $size * 0.12), [single]($stemTopY + $size * 0.20),
        [single]($flagX + $size * 0.14), [single]($stemTopY + $size * 0.08),
        [single]$flagX, [single]$stemTopY
    )
    $flagPath.CloseFigure()
    $g.FillPath($noteBrush, $flagPath)

    $g.Dispose()
    return $bmp
}

function Save-Ico {
    param([string]$path, [int[]]$sizes)

    $images = @()
    foreach ($s in $sizes) {
        $bmp = New-NoteIconBitmap $s
        $ms = New-Object System.IO.MemoryStream
        $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
        $images += , @{ Size = $s; Bytes = $ms.ToArray() }
        $bmp.Dispose()
    }

    $fs = New-Object System.IO.FileStream $path, ([System.IO.FileMode]::Create)
    $bw = New-Object System.IO.BinaryWriter $fs

    $bw.Write([UInt16]0)          # reserved
    $bw.Write([UInt16]1)          # type: icon
    $bw.Write([UInt16]$images.Count)

    $headerSize = 6 + (16 * $images.Count)
    $offset = $headerSize

    foreach ($img in $images) {
        $s = $img.Size
        $byteSize = $img.Bytes.Length
        $bw.Write([byte]($(if ($s -ge 256) { 0 } else { $s })))
        $bw.Write([byte]($(if ($s -ge 256) { 0 } else { $s })))
        $bw.Write([byte]0)        # color count
        $bw.Write([byte]0)        # reserved
        $bw.Write([UInt16]1)      # planes
        $bw.Write([UInt16]32)     # bit count
        $bw.Write([UInt32]$byteSize)
        $bw.Write([UInt32]$offset)
        $offset += $byteSize
    }

    foreach ($img in $images) {
        $bw.Write($img.Bytes)
    }

    $bw.Flush()
    $fs.Close()
}

$pngPath = "C:\Users\ZenoxArt\Documents\!CODING\Java\music-player\src\main\resources\icon\app-icon.png"
$icoPath = "C:\Users\ZenoxArt\Documents\!CODING\Java\music-player\packaging\app-icon.ico"

$bigBmp = New-NoteIconBitmap 256
$bigBmp.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bigBmp.Dispose()

Save-Ico -path $icoPath -sizes @(16, 32, 48, 256)

Write-Host "PNG saved to $pngPath"
Write-Host "ICO saved to $icoPath"
