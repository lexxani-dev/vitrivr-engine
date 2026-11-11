[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })]
    [string]$ImagePath,

    [string]$Schema = "sandbox",
    [string]$ApiBase = "http://localhost:7074",

    [ValidateRange(1, 1000)]
    [int]$Limit = 10
)

$resolvedImagePath = (Resolve-Path -LiteralPath $ImagePath).Path
$mimeType = switch ([System.IO.Path]::GetExtension($resolvedImagePath).ToLowerInvariant()) {
    ".jpg"  { "image/jpeg" }
    ".jpeg" { "image/jpeg" }
    ".png"  { "image/png" }
    ".webp" { "image/webp" }
    default { throw "Unsupported image format: $([System.IO.Path]::GetExtension($resolvedImagePath))" }
}

$base64 = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes($resolvedImagePath))
$dataUrl = "data:$mimeType;base64,$base64"
$requestBody = @{
    inputs = @{
        image = @{
            type = "IMAGE"
            data = $dataUrl
        }
    }
    operations = @{
        clip = @{
            field = "clip"
            inputs = @{ image = "image" }
            parameters = @{ limit = [string]$Limit }
        }
    }
    output = "clip"
} | ConvertTo-Json -Depth 10

$baseUrl = $ApiBase.TrimEnd("/")
$uri = "$baseUrl/api/$Schema/query"
Invoke-RestMethod -Method Post -Uri $uri -ContentType "application/json" -Body $requestBody
