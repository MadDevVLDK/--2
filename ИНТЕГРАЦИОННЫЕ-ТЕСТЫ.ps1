[CmdletBinding()]
param(
    [string]$BookServiceUrl = 'http://localhost:8081',
    [string]$UserServiceUrl = 'http://localhost:8082',
    [string]$BorrowServiceUrl = 'http://localhost:8083',
    [switch]$SkipBorrow
)

$ErrorActionPreference = 'Stop'

function Invoke-Api {
    param(
        [Parameter(Mandatory)] [ValidateSet('GET', 'POST', 'PUT', 'PATCH', 'DELETE')] [string]$Method,
        [Parameter(Mandatory)] [string]$Uri,
        [object]$Body,
        [int]$ExpectedStatus = 200
    )

    try {
        $request = @{
            Method      = $Method
            Uri         = $Uri
            UseBasicParsing = $true
            ErrorAction = 'Stop'
        }
        if ($null -ne $Body) {
            $request.Body = $Body | ConvertTo-Json -Compress
            $request.ContentType = 'application/json'
        }

        $response = Invoke-WebRequest @request
        $status = [int]$response.StatusCode
        $rawContent = if ($response.Content -is [byte[]]) {
            [System.Text.Encoding]::UTF8.GetString($response.Content)
        } else {
            [string]$response.Content
        }
        $content = if ($rawContent) { $rawContent | ConvertFrom-Json } else { $null }
    }
    catch {
        $webResponse = $_.Exception.Response
        if ($null -eq $webResponse) {
            throw "HTTP $Method $Uri failed: $($_.Exception.Message)"
        }

        $status = [int]$webResponse.StatusCode
        $stream = $webResponse.GetResponseStream()
        $reader = [System.IO.StreamReader]::new($stream)
        $rawContent = $reader.ReadToEnd()
        $reader.Dispose()
        $stream.Dispose()
        $content = if ($rawContent) { $rawContent | ConvertFrom-Json } else { $null }
    }

    if ($status -ne $ExpectedStatus) {
        throw "Expected HTTP $ExpectedStatus from $Method $Uri, received $status"
    }

    return $content
}

function Assert-Equal {
    param(
        [Parameter(Mandatory)] $Actual,
        [Parameter(Mandatory)] $Expected,
        [Parameter(Mandatory)] [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message. Expected '$Expected', received '$Actual'"
    }
}

function Assert-True {
    param(
        [Parameter(Mandatory)] [bool]$Condition,
        [Parameter(Mandatory)] [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

$createdBookId = $null
$createdUserId = $null
$suffix = [Guid]::NewGuid().ToString('N').Substring(0, 8)

try {
    Write-Host 'Checking service health...' -ForegroundColor Cyan
    foreach ($serviceUrl in @($BookServiceUrl, $UserServiceUrl, $BorrowServiceUrl)) {
        $health = Invoke-Api -Method GET -Uri "$serviceUrl/actuator/health"
        Assert-Equal $health.status 'UP' "Service $serviceUrl is not healthy"
    }

    Write-Host 'Checking user CRUD...' -ForegroundColor Cyan
    $user = Invoke-Api -Method POST -Uri "$UserServiceUrl/users" -Body @{
        name = "PowerShell User $suffix"
        email = "powershell-$suffix@example.test"
        active = $true
    } -ExpectedStatus 201
    $createdUserId = [long]$user.id
    Assert-True ($createdUserId -gt 0) 'User response does not contain a valid id'

    $savedUser = Invoke-Api -Method GET -Uri "$UserServiceUrl/users/$createdUserId"
    Assert-Equal $savedUser.email "powershell-$suffix@example.test" 'Created user was not persisted'

    $updatedUser = Invoke-Api -Method PUT -Uri "$UserServiceUrl/users/$createdUserId" -Body @{
        name = "Updated PowerShell User $suffix"
        email = "updated-$suffix@example.test"
        active = $true
    }
    Assert-Equal $updatedUser.name "Updated PowerShell User $suffix" 'User update failed'

    Write-Host 'Checking book CRUD and availability...' -ForegroundColor Cyan
    $book = Invoke-Api -Method POST -Uri "$BookServiceUrl/books" -Body @{
        title = "PowerShell Book $suffix"
        author = 'Test Author'
        available = $true
    } -ExpectedStatus 201
    $createdBookId = [long]$book.id
    Assert-True ($createdBookId -gt 0) 'Book response does not contain a valid id'

    $savedBook = Invoke-Api -Method GET -Uri "$BookServiceUrl/books/$createdBookId"
    Assert-Equal $savedBook.title "PowerShell Book $suffix" 'Created book was not persisted'

    $unavailableBook = Invoke-Api -Method PATCH -Uri "$BookServiceUrl/books/$createdBookId/availability" -Body @{ available = $false }
    Assert-Equal $unavailableBook.available $false 'Book availability was not updated'

    if (-not $SkipBorrow) {
        Write-Host 'Checking borrow workflow...' -ForegroundColor Cyan
        Invoke-Api -Method PATCH -Uri "$BookServiceUrl/books/$createdBookId/availability" -Body @{ available = $true }
        $borrow = Invoke-Api -Method POST -Uri "$BorrowServiceUrl/borrowings" -Body @{
            bookId = $createdBookId
            userId = $createdUserId
        } -ExpectedStatus 201
        Assert-Equal ([long]$borrow.bookId) $createdBookId 'Borrow record has an unexpected book id'
        Assert-Equal ([long]$borrow.userId) $createdUserId 'Borrow record has an unexpected user id'

        $borrowedBook = Invoke-Api -Method GET -Uri "$BookServiceUrl/books/$createdBookId"
        Assert-Equal $borrowedBook.available $false 'Borrowing did not mark the book unavailable'
    }

    Write-Host 'All PowerShell service tests passed.' -ForegroundColor Green
}
finally {
    if ($createdBookId) {
        try { Invoke-Api -Method DELETE -Uri "$BookServiceUrl/books/$createdBookId" -ExpectedStatus 204 | Out-Null } catch { Write-Warning "Could not delete test book ${createdBookId}: $($_.Exception.Message)" }
    }
    if ($createdUserId) {
        try { Invoke-Api -Method DELETE -Uri "$UserServiceUrl/users/$createdUserId" -ExpectedStatus 204 | Out-Null } catch { Write-Warning "Could not delete test user ${createdUserId}: $($_.Exception.Message)" }
    }
}