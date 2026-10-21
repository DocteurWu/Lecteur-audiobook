param(
    [Parameter(Mandatory = $true)][int]$Size,
    [Parameter(Mandatory = $true)][string]$Out
)

Add-Type -AssemblyName System.Drawing

$bmp = New-Object System.Drawing.Bitmap($Size, $Size)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.Clear([System.Drawing.Color]::Transparent)

# Fond arrondi avec dégradé
$radius = [int]($Size * 0.22)
$path = New-Object System.Drawing.Drawing2D.GraphicsPath
$path.AddArc(0, 0, $radius, $radius, 180, 90)
$path.AddArc($Size - $radius, 0, $radius, $radius, 270, 90)
$path.AddArc($Size - $radius, $Size - $radius, $radius, $radius, 0, 90)
$path.AddArc(0, $Size - $radius, $radius, $radius, 90, 90)
$path.CloseFigure()

$p1 = New-Object System.Drawing.Point(0, 0)
$p2 = New-Object System.Drawing.Point($Size, $Size)
$brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($p1, $p2, [System.Drawing.Color]::FromArgb(255, 142, 124, 255), [System.Drawing.Color]::FromArgb(255, 64, 40, 180))
$g.FillPath($brush, $path)

# Casque
$white = [System.Drawing.Color]::White
$pen = New-Object System.Drawing.Pen($white, [float]($Size * 0.085))
$pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
$pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
$g.DrawArc($pen, [float]($Size * 0.24), [float]($Size * 0.28), [float]($Size * 0.52), [float]($Size * 0.52), 180, 180)

$whiteBrush = New-Object System.Drawing.SolidBrush($white)
$g.FillEllipse($whiteBrush, [float]($Size * 0.17), [float]($Size * 0.46), [float]($Size * 0.13), [float]($Size * 0.24))
$g.FillEllipse($whiteBrush, [float]($Size * 0.70), [float]($Size * 0.46), [float]($Size * 0.13), [float]($Size * 0.24))

$g.Dispose()
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "OK $Out"
