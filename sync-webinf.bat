@echo off
setlocal

set ROOT=%~dp0
set SRC=%ROOT%backend\WEB-INF
set DST=%ROOT%WEB-INF

echo [1/3] Ensure root WEB-INF folders...
if not exist "%DST%" mkdir "%DST%"
if not exist "%DST%\classes" mkdir "%DST%\classes"
if not exist "%DST%\lib" mkdir "%DST%\lib"

echo [2/3] Copy web.xml and classes...
copy /Y "%SRC%\web.xml" "%DST%\web.xml" >nul
xcopy "%SRC%\classes\*" "%DST%\classes\" /E /I /Y >nul

echo [3/3] Copy runtime libs...
if exist "%SRC%\lib\json-20240303.jar" (
  copy /Y "%SRC%\lib\json-20240303.jar" "%DST%\lib\json-20240303.jar" >nul
)

echo Done. Root WEB-INF is ready for Tomcat deployment.
echo Note: Keep servlet-api.jar only in backend\WEB-INF\lib for compile, do not deploy it to PAGE\WEB-INF\lib.

endlocal
