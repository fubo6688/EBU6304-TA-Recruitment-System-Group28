$ErrorActionPreference = 'Stop'
$s = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login = Invoke-WebRequest -Uri 'http://localhost:8080/ta-system/api/login' -Method Post -Body @{username='ta_bob';password='TaBobSafeA1'} -WebSession $s -UseBasicParsing
Write-Output '---LOGIN RESPONSE---'
Write-Output $login.Content
$res = Invoke-WebRequest -Uri 'http://localhost:8080/ta-system/api/user/resume-parse' -WebSession $s -Method Get -UseBasicParsing
Write-Output '---RESUME-PARSE RESPONSE---'
Write-Output $res.Content
Write-Output '---EXTERNAL PARSER (direct)---'
$headers = @{ 'apy-token' = 'APY0Ao6XkTkotbl0JrZ8Q8fevMVJboPV8rfgvaQVtqR9D4oCZnBl39ffipiliKLCAPgpyVwC' }
Invoke-RestMethod -Uri 'https://api.apyhub.com/sharpapi/api/v1/hr/parse_resume' -Method Post -Headers $headers -Form @{ file = Get-Item 'data/resumes/ta_bob.pdf' } | ConvertTo-Json -Depth 5
