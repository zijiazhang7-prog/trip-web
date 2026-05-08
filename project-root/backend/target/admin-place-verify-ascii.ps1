$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8080/api/v1'
$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$adminUser = "admin_place_admin_$stamp"
$normalUser = "admin_place_user_$stamp"
$pwd = "Tmp" + $stamp + "Aa1"
$headersJson = @{ 'Content-Type' = 'application/json' }
$mysqlUser = $env:DB_USERNAME

function Invoke-Api($Method, $Url, $Headers = @{}, $Body = $null) {
    try {
        if ($null -eq $Body) {
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers -TimeoutSec 10 -UseBasicParsing
        } else {
            $resp = Invoke-WebRequest -Uri $Url -Method $Method -Headers $Headers -Body $Body -ContentType 'application/json' -TimeoutSec 10 -UseBasicParsing
        }
        return @{ Status = [int]$resp.StatusCode; Body = ($resp.Content | ConvertFrom-Json) }
    } catch {
        $status = 0
        $body = $null
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $text = $reader.ReadToEnd()
                if (-not [string]::IsNullOrWhiteSpace($text)) { $body = $text | ConvertFrom-Json }
            } catch {}
        }
        return @{ Status = $status; Body = $body }
    }
}

$job = Start-Job -ScriptBlock { Set-Location 'D:\GitHub\trip-web\project-root\backend'; java -jar target\trip-backend-0.0.1-SNAPSHOT.jar }
$createdPlaceIds = New-Object System.Collections.Generic.List[long]
$createdFacilityIds = New-Object System.Collections.Generic.List[long]
$createdNodeIds = New-Object System.Collections.Generic.List[long]
try {
    $healthy = $false
    for ($i = 0; $i -lt 35; $i++) {
        try {
            $h = Invoke-RestMethod -Uri "$base/health" -Method Get -TimeoutSec 2
            if ($h.success -eq $true -and $h.data.status -eq 'ok') { $healthy = $true; break }
        } catch { Start-Sleep -Seconds 2 }
    }
    if (-not $healthy) { throw 'backend health check failed' }

    $adminReg = Invoke-Api Post "$base/auth/register" $headersJson (@{ username = $adminUser; password = $pwd; nickname = 'Admin场所验证管理员' } | ConvertTo-Json -Compress)
    $normalReg = Invoke-Api Post "$base/auth/register" $headersJson (@{ username = $normalUser; password = $pwd; nickname = 'Admin场所验证普通用户' } | ConvertTo-Json -Compress)
    if ($adminReg.Status -ne 200 -or $adminReg.Body.success -ne $true) { throw 'admin register failed' }
    if ($normalReg.Status -ne 200 -or $normalReg.Body.success -ne $true) { throw 'normal register failed' }

    $env:MYSQL_PWD = $env:DB_PASSWORD
    mysql --user=$mysqlUser -D tour_system -N -e "UPDATE user SET role='admin' WHERE username='$adminUser';" | Out-Null
    $destinationId = [long](mysql --user=$mysqlUser -D tour_system -N -e "SELECT id FROM destination ORDER BY id LIMIT 1;")
    if ($destinationId -le 0) { throw 'destination missing' }

    $adminLogin = Invoke-Api Post "$base/auth/login" $headersJson (@{ username = $adminUser; password = $pwd } | ConvertTo-Json -Compress)
    $normalLogin = Invoke-Api Post "$base/auth/login" $headersJson (@{ username = $normalUser; password = $pwd } | ConvertTo-Json -Compress)
    if ($adminLogin.Status -ne 200 -or $adminLogin.Body.success -ne $true) { throw 'admin login failed' }
    if ($normalLogin.Status -ne 200 -or $normalLogin.Body.success -ne $true) { throw 'normal login failed' }
    $adminHeaders = @{ Authorization = "Bearer $($adminLogin.Body.data.token)" }
    $normalHeaders = @{ Authorization = "Bearer $($normalLogin.Body.data.token)" }

    $noToken = Invoke-Api Get "$base/admin/places?pageNum=1&pageSize=1"
    $normalDenied = Invoke-Api Get "$base/admin/places?pageNum=1&pageSize=1" $normalHeaders
    $adminList = Invoke-Api Get "$base/admin/places?pageNum=1&pageSize=5" $adminHeaders

    $name1 = "admin_place_no_ref_$stamp"
    $created = Invoke-Api Post "$base/admin/places" $adminHeaders (@{ destinationId = $destinationId; name = $name1; placeType = 'building'; description = 'Admin place real-db verification'; lng = 116.123456; lat = 39.123456; floorInfo = '1F-5F'; heatScore = 86.00; ratingScore = 4.70; openTimeRule = '08:00-22:00'; suggestedDurationMin = 60; costLevel = 1 } | ConvertTo-Json -Compress)
    if ($created.Status -ne 200 -or $created.Body.success -ne $true) { throw 'create place failed' }
    $placeId = [long]$created.Body.data.id
    $createdPlaceIds.Add($placeId)

    $listed = Invoke-Api Get "$base/admin/places?destinationId=$destinationId&keyword=$([uri]::EscapeDataString($name1))&pageNum=1&pageSize=10" $adminHeaders
    $updatedName = "admin_place_updated_$stamp"
    $updated = Invoke-Api Put "$base/admin/places/$placeId" $adminHeaders (@{ destinationId = $destinationId; name = $updatedName; placeType = 'scenic_spot'; description = 'updated by real-db verification'; lng = 116.223456; lat = 39.223456; floorInfo = '2F'; heatScore = 88.00; ratingScore = 4.80; openTimeRule = '09:00-21:00'; suggestedDurationMin = 45; costLevel = 2 } | ConvertTo-Json -Compress)
    $deleted = Invoke-Api Delete "$base/admin/places/$placeId" $adminHeaders
    $createdPlaceIds.Remove($placeId) | Out-Null
    $deletedCount = [int](mysql --user=$mysqlUser -D tour_system -N -e "SELECT COUNT(*) FROM place WHERE id=$placeId;")

    $protected = Invoke-Api Post "$base/admin/places" $adminHeaders (@{ destinationId = $destinationId; name = "admin_place_fac_ref_$stamp"; placeType = 'building'; description = 'facility reference protected' } | ConvertTo-Json -Compress)
    if ($protected.Status -ne 200 -or $protected.Body.success -ne $true) { throw 'create facility protected place failed' }
    $protectedPlaceId = [long]$protected.Body.data.id
    $createdPlaceIds.Add($protectedPlaceId)
    $facility = Invoke-Api Post "$base/admin/facilities" $adminHeaders (@{ destinationId = $destinationId; placeId = $protectedPlaceId; name = "admin_place_ref_facility_$stamp"; facilityType = 'toilet'; description = 'reference test' } | ConvertTo-Json -Compress)
    if ($facility.Status -ne 200 -or $facility.Body.success -ne $true) { throw 'create reference facility failed' }
    $facilityId = [long]$facility.Body.data.id
    $createdFacilityIds.Add($facilityId)
    $deleteReferencedByFacility = Invoke-Api Delete "$base/admin/places/$protectedPlaceId" $adminHeaders

    $nodeProtected = Invoke-Api Post "$base/admin/places" $adminHeaders (@{ destinationId = $destinationId; name = "admin_place_node_ref_$stamp"; placeType = 'building'; description = 'map node reference protected' } | ConvertTo-Json -Compress)
    if ($nodeProtected.Status -ne 200 -or $nodeProtected.Body.success -ne $true) { throw 'create node protected place failed' }
    $nodeProtectedPlaceId = [long]$nodeProtected.Body.data.id
    $createdPlaceIds.Add($nodeProtectedPlaceId)
    $node = Invoke-Api Post "$base/admin/map/nodes" $adminHeaders (@{ destinationId = $destinationId; nodeName = "admin_place_ref_node_$stamp"; nodeType = 'place'; refId = $nodeProtectedPlaceId; lng = 116.333333; lat = 39.333333; floorNo = 1 } | ConvertTo-Json -Compress)
    if ($node.Status -ne 200 -or $node.Body.success -ne $true) { throw 'create reference map node failed' }
    $nodeId = [long]$node.Body.data.id
    $createdNodeIds.Add($nodeId)
    $deleteReferencedByNode = Invoke-Api Delete "$base/admin/places/$nodeProtectedPlaceId" $adminHeaders

    [pscustomobject]@{
        Health = 'OK'
        DestinationId = $destinationId
        NoTokenStatus = $noToken.Status
        NormalUserStatus = $normalDenied.Status
        AdminListSuccess = $adminList.Body.success
        CreatedPlaceId = $placeId
        ListFoundTotal = $listed.Body.data.total
        UpdatedNameMatched = ($updated.Body.data.name -eq $updatedName)
        DeleteUnreferencedSuccess = $deleted.Body.data
        DeletedPlaceRemainingRows = $deletedCount
        FacilityReferenceDeleteStatus = $deleteReferencedByFacility.Status
        FacilityReferenceDeleteCode = $deleteReferencedByFacility.Body.code
        MapNodeReferenceDeleteStatus = $deleteReferencedByNode.Status
        MapNodeReferenceDeleteCode = $deleteReferencedByNode.Body.code
    } | ConvertTo-Json -Compress
}
finally {
    try {
        if ($createdNodeIds.Count -gt 0) { $ids = ($createdNodeIds -join ','); mysql --user=$mysqlUser -D tour_system -N -e "DELETE FROM map_node WHERE id IN ($ids);" | Out-Null }
        if ($createdFacilityIds.Count -gt 0) { $ids = ($createdFacilityIds -join ','); mysql --user=$mysqlUser -D tour_system -N -e "DELETE FROM facility WHERE id IN ($ids);" | Out-Null }
        if ($createdPlaceIds.Count -gt 0) { $ids = ($createdPlaceIds -join ','); mysql --user=$mysqlUser -D tour_system -N -e "DELETE FROM place WHERE id IN ($ids);" | Out-Null }
        mysql --user=$mysqlUser -D tour_system -N -e "DELETE FROM user WHERE username IN ('$adminUser','$normalUser');" | Out-Null
    } catch { Write-Output "CLEANUP_WARNING: $($_.Exception.Message)" }
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    Stop-Job $job -ErrorAction SilentlyContinue
    Remove-Job $job -Force -ErrorAction SilentlyContinue
}

