@echo off
for /r %%i in (*.txt) do (
    ren "%%i" "%%~ni.md"
)
echo 完成
pause