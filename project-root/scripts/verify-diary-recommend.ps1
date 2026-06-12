param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$BackendJar = ""
)

$ErrorActionPreference = "Stop"

if (-not $env:DB_USERNAME -or -not $env:DB_PASSWORD) {
    throw "DB_USERNAME and DB_PASSWORD must be set."
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$baseUri = [Uri]$BaseUrl
$serverPort = $baseUri.Port
if (Get-NetTCPConnection -LocalPort $serverPort -State Listen -ErrorAction SilentlyContinue) {
    throw "Port $serverPort is already in use. Choose an isolated BaseUrl."
}
if (-not $BackendJar) {
    $BackendJar = Join-Path $projectRoot "backend\target\trip-backend-0.0.1-SNAPSHOT.jar"
}
if (-not (Test-Path -LiteralPath $BackendJar)) {
    throw "Backend jar not found: $BackendJar"
}

$marker = "REC{0}{1}" -f (Get-Date -Format "yyyyMMddHHmmss"), (Get-Random -Minimum 100 -Maximum 999)
$userA = ("rec_a_" + $marker).ToLowerInvariant()
$userB = ("rec_b_" + $marker).ToLowerInvariant()
$password = "RecTest_{0}!" -f ([Guid]::NewGuid().ToString("N").Substring(0, 12))
$destinationName = $marker + "_recommend_destination"
$userAId = $null
$userBId = $null
$destinationId = $null
$backendProcess = $null

function Invoke-JsonApi {
    param(
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [string]$Token = $null
    )

    $headers = @{}
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }
    $parameters = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
        TimeoutSec = 20
    }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json; charset=utf-8"
        $parameters.Body = $Body | ConvertTo-Json -Depth 10 -Compress
    }
    try {
        Invoke-RestMethod @parameters
    } catch {
        $responseBody = ""
        if ($_.Exception.Response) {
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $responseBody = $reader.ReadToEnd()
                $reader.Dispose()
            } catch {
                $responseBody = ""
            }
        }
        throw "API $Method $Path failed. Response: $responseBody"
    }
}

function Invoke-DatabaseScalar {
    param([string]$Sql)

    $env:MYSQL_PWD = $env:DB_PASSWORD
    try {
        $result = & mysql --default-character-set=utf8mb4 -h localhost -u $env:DB_USERNAME `
            -N -B tour_system -e $Sql
        if ($LASTEXITCODE -ne 0) {
            throw "mysql command failed with exit code $LASTEXITCODE"
        }
        return @($result)[0]
    } finally {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Invoke-Database {
    param([string]$Sql)

    $env:MYSQL_PWD = $env:DB_PASSWORD
    try {
        & mysql --default-character-set=utf8mb4 -h localhost -u $env:DB_USERNAME `
            tour_system -e $Sql | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "mysql command failed with exit code $LASTEXITCODE"
        }
    } finally {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Assert-Condition {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw "ASSERTION FAILED: $Message"
    }
}

function Get-IdSequence {
    param($Response)
    return ($Response.data.list | ForEach-Object { [string]$_.id }) -join ","
}

try {
    $stdout = Join-Path $projectRoot "backend\target\diary-recommend-http.stdout.log"
    $stderr = Join-Path $projectRoot "backend\target\diary-recommend-http.stderr.log"
    $backendProcess = Start-Process -FilePath "java" `
        -ArgumentList "-jar", $BackendJar, "--server.port=$serverPort" `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -PassThru

    $healthy = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($backendProcess.HasExited) {
            throw "Backend exited early with code $($backendProcess.ExitCode)."
        }
        try {
            $health = Invoke-RestMethod -Uri "$BaseUrl/api/v1/health" -TimeoutSec 2
            if ($health.success -and $health.data.status -eq "ok") {
                $healthy = $true
                break
            }
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    Assert-Condition $healthy "backend did not become healthy"

    Write-Output "STAGE=register"
    $registerA = Invoke-JsonApi POST "/api/v1/auth/register" @{
        username = $userA
        password = $password
        nickname = "Recommend User A"
    }
    $registerB = Invoke-JsonApi POST "/api/v1/auth/register" @{
        username = $userB
        password = $password
        nickname = "Recommend User B"
    }
    Assert-Condition ($registerA.success -and $registerB.success) "temporary user registration failed"

    Write-Output "STAGE=login"
    $loginA = Invoke-JsonApi POST "/api/v1/auth/login" @{ username = $userA; password = $password }
    $loginB = Invoke-JsonApi POST "/api/v1/auth/login" @{ username = $userB; password = $password }
    Assert-Condition ($loginA.success -and $loginB.success) "temporary user login failed"
    $tokenA = $loginA.data.token
    $tokenB = $loginB.data.token
    $userAId = [long]$loginA.data.user.id
    $userBId = [long]$loginB.data.user.id

    Write-Output "STAGE=preference"
    $preferenceA = Invoke-JsonApi PUT "/api/v1/user-preferences/me" @{
        preferHotLevel = 1
        preferThemeList = @("quietstudy")
    } $tokenA
    $preferenceB = Invoke-JsonApi PUT "/api/v1/user-preferences/me" @{
        preferHotLevel = 1
        preferThemeList = @("nightphoto")
    } $tokenB
    Assert-Condition ((@($preferenceA.data.preferThemeList) -join ",") -eq "quietstudy") `
        "user A preference mismatch"
    Assert-Condition ((@($preferenceB.data.preferThemeList) -join ",") -eq "nightphoto") `
        "user B preference mismatch"

    Write-Output "STAGE=fixture"
    Invoke-Database @"
INSERT INTO destination(
  name, type, category, city, description, heat_score, rating_score, tag_json, status
) VALUES (
  '$destinationName', 'campus', 'recommend-test', 'Beijing',
  'recommend database regression only', 0, 0, '[]', 1
);
"@
    $destinationId = [long](Invoke-DatabaseScalar `
        "SELECT id FROM destination WHERE name='$destinationName' ORDER BY id DESC LIMIT 1;")
    Assert-Condition ($destinationId -gt 0) "temporary destination insert failed"

    Invoke-Database @"
INSERT INTO diary(
  user_id, destination_id, title, content_text, heat_score, rating_score,
  rating_count, visibility, status, created_at, updated_at
) VALUES
  ($userAId, $destinationId, '$marker quietstudy diary',
   'quietstudy library reading culture', 100, 3.00, 1, 'public', 1, NOW(), NOW()),
  ($userAId, $destinationId, '$marker nightphoto diary',
   'nightphoto sports checkin night view', 90, 3.00, 1, 'public', 1, NOW(), NOW()),
  ($userAId, $destinationId, '$marker heat baseline',
   'neutral baseline content', 900001, 2.00, 1, 'public', 1, NOW(), NOW()),
  ($userAId, $destinationId, '$marker rating baseline',
   'another neutral baseline', 10, 5.00, 1, 'public', 1, NOW(), NOW());
"@
    $createdCount = [int](Invoke-DatabaseScalar `
        "SELECT COUNT(*) FROM diary WHERE destination_id=$destinationId;")
    Assert-Condition ($createdCount -eq 4) "temporary diary insert failed"
    $heatBefore = Invoke-DatabaseScalar `
        "SELECT GROUP_CONCAT(CONCAT(id, ':', heat_score) ORDER BY id) FROM diary WHERE destination_id=$destinationId;"

    Write-Output "STAGE=requests"
    $results = @{}
    foreach ($entry in @(@("A", $tokenA), @("B", $tokenB))) {
        foreach ($sortBy in @("interest", "heat", "rating")) {
            $key = $entry[0] + "_" + $sortBy
            $results[$key] = Invoke-JsonApi GET `
                "/api/v1/diaries/recommend?sortBy=$sortBy&pageSize=4" $null $entry[1]
        }
    }

    foreach ($key in $results.Keys) {
        $response = $results[$key]
        Assert-Condition ($response.success -and $response.code -eq "SUCCESS") "$key was unsuccessful"
        Assert-Condition ($response.data.pageNum -eq 1 -and $response.data.pageSize -eq 4) `
            "$key pagination mismatch"
        Assert-Condition ($response.data.list.Count -eq 4) "$key did not return Top-4"
    }

    $userAInterestFirst = [string]$results["A_interest"].data.list[0].title
    $userBInterestFirst = [string]$results["B_interest"].data.list[0].title
    Assert-Condition ($userAInterestFirst -like "*$marker*quietstudy*") `
        "user A interest recommendation mismatch"
    Assert-Condition ($userBInterestFirst -like "*$marker*nightphoto*") `
        "user B interest recommendation mismatch"
    Assert-Condition ($results["A_heat"].data.list[0].title -like "*$marker*heat baseline*") `
        "user A heat recommendation mismatch"
    Assert-Condition ($results["B_heat"].data.list[0].title -like "*$marker*heat baseline*") `
        "user B heat recommendation mismatch"
    Assert-Condition ($results["A_rating"].data.list[0].title -like "*$marker*rating baseline*") `
        "user A rating recommendation mismatch"
    Assert-Condition ($results["B_rating"].data.list[0].title -like "*$marker*rating baseline*") `
        "user B rating recommendation mismatch"
    Assert-Condition ((Get-IdSequence $results["A_heat"]) -eq (Get-IdSequence $results["B_heat"])) `
        "heat order changed with user preference"
    Assert-Condition ((Get-IdSequence $results["A_rating"]) -eq (Get-IdSequence $results["B_rating"])) `
        "rating order changed with user preference"

    $totals = @($results.Values | ForEach-Object { [long]$_.data.total } | Sort-Object -Unique)
    Assert-Condition ($totals.Count -eq 1) "candidate total differed across requests"

    $expectedRoot = @("code", "data", "message", "success", "timestamp")
    $expectedData = @("list", "pageNum", "pageSize", "pages", "total")
    $expectedDiary = @(
        "contentText", "createdAt", "destinationId", "destinationName", "heatScore",
        "id", "mediaList", "ratingCount", "ratingScore", "routeHistoryId", "title",
        "updatedAt", "userId", "username", "visibility"
    )
    foreach ($key in $results.Keys) {
        $rootKeys = @($results[$key].PSObject.Properties.Name | Sort-Object)
        $dataKeys = @($results[$key].data.PSObject.Properties.Name | Sort-Object)
        $diaryKeys = @($results[$key].data.list[0].PSObject.Properties.Name | Sort-Object)
        Assert-Condition (($rootKeys -join ",") -eq (($expectedRoot | Sort-Object) -join ",")) `
            "$key root contract mismatch"
        Assert-Condition (($dataKeys -join ",") -eq (($expectedData | Sort-Object) -join ",")) `
            "$key page contract mismatch"
        Assert-Condition (($diaryKeys -join ",") -eq (($expectedDiary | Sort-Object) -join ",")) `
            "$key diary contract mismatch"
    }

    $heatAfter = Invoke-DatabaseScalar `
        "SELECT GROUP_CONCAT(CONCAT(id, ':', heat_score) ORDER BY id) FROM diary WHERE destination_id=$destinationId;"
    Assert-Condition ($heatBefore -eq $heatAfter) "recommendation requests changed diary heat"

    [pscustomobject]@{
        Marker = $marker
        CandidateTotal = $totals[0]
        UserAInterestFirst = $userAInterestFirst
        UserBInterestFirst = $userBInterestFirst
        HeatFirst = [string]$results["A_heat"].data.list[0].title
        RatingFirst = [string]$results["A_rating"].data.list[0].title
        HeatOrderSameAcrossUsers = $true
        RatingOrderSameAcrossUsers = $true
        ContractConsistent = $true
        HeatUnchanged = $true
    } | Format-List
} finally {
    if ($destinationId) {
        try {
            Invoke-Database "DELETE FROM diary WHERE destination_id=$destinationId; DELETE FROM destination WHERE id=$destinationId;"
        } catch {
            Write-Warning "Temporary diary or destination cleanup failed."
        }
    }
    $userIds = @($userAId, $userBId) | Where-Object { $_ } | ForEach-Object { [string]$_ }
    if ($userIds.Count -gt 0) {
        try {
            Invoke-Database "DELETE FROM user WHERE id IN ($($userIds -join ','));"
        } catch {
            Write-Warning "Temporary user cleanup failed."
        }
    }
    try {
        $remaining = Invoke-DatabaseScalar @"
SELECT
  (SELECT COUNT(*) FROM user WHERE username IN ('$userA', '$userB')) +
  (SELECT COUNT(*) FROM destination WHERE name='$destinationName') +
  (SELECT COUNT(*) FROM diary WHERE title LIKE '$marker%');
"@
        Write-Output "CLEANUP_REMAINING=$remaining"
    } catch {
        Write-Warning "Cleanup verification failed."
    }
    if ($backendProcess -and -not $backendProcess.HasExited) {
        Start-Sleep -Seconds 1
        Stop-Process -Id $backendProcess.Id -Force
    }
}
