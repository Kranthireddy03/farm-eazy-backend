# Clear all FarmEazy database tables
# Run this script from the backend directory

Write-Host "Connecting to H2 database and clearing all tables..." -ForegroundColor Yellow

$sqlCommands = @"
DELETE FROM otp_verifications;
DELETE FROM products;
DELETE FROM irrigation_schedules;
DELETE FROM crops;
DELETE FROM farms;
DELETE FROM users;
ALTER TABLE otp_verifications ALTER COLUMN id RESTART WITH 1;
ALTER TABLE products ALTER COLUMN id RESTART WITH 1;
ALTER TABLE irrigation_schedules ALTER COLUMN id RESTART WITH 1;
ALTER TABLE crops ALTER COLUMN id RESTART WITH 1;
ALTER TABLE farms ALTER COLUMN id RESTART WITH 1;
ALTER TABLE users ALTER COLUMN id RESTART WITH 1;
"@

# Save to temp file
$sqlCommands | Out-File -FilePath "temp_clear.sql" -Encoding UTF8

Write-Host "`nSQL commands saved to temp_clear.sql" -ForegroundColor Green
Write-Host "`nTo execute:" -ForegroundColor Cyan
Write-Host "1. Open H2 Console at http://localhost:8080/h2-console" -ForegroundColor White
Write-Host "2. Login with:" -ForegroundColor White
Write-Host "   - JDBC URL: jdbc:h2:file:./data/farmeazy_db" -ForegroundColor White
Write-Host "   - Username: sa" -ForegroundColor White
Write-Host "   - Password: (empty)" -ForegroundColor White
Write-Host "3. Copy and run the SQL from temp_clear.sql" -ForegroundColor White
Write-Host "`nAll tables will be emptied and IDs reset to 1" -ForegroundColor Yellow
