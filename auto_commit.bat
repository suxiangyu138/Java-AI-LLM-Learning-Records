@echo off
cd /d %~dp0
echo %date% %time% > .daily_commit.tmp
git add .daily_commit.tmp
git commit -m "auto daily update"
git push