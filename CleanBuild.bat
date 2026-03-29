setlocal

echo ========================================
echo AdventureRPG Full Rebuild
echo ========================================
echo.

echo [1/4] Cleaning editor + main modules...
call gradlew.bat --no-daemon clean :core:clean :lwjgl3:clean
if errorlevel 1 goto :fail

echo.
echo [2/4] Rebuilding shared core (main game logic)...
call gradlew.bat --no-daemon :core:build
if errorlevel 1 goto :fail

echo.
echo [3/4] Rebuilding desktop launcher (main + editor entry points)...
call gradlew.bat --no-daemon :lwjgl3:build
if errorlevel 1 goto :fail

echo.
echo [4/4] Refreshing all project artifacts...
call gradlew.bat --no-daemon build
if errorlevel 1 goto :fail

echo.
echo Done. Clean rebuild completed successfully.
exit /b 0

:fail
echo.
echo Build failed. See errors above.
exit /b 1