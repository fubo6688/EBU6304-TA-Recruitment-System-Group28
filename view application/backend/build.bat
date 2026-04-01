@echo off
REM 编译 Java 文件

echo 正在编译 Java 源代码...

set SRC_DIR=src
set BIN_DIR=WEB-INF\classes
set LIB_DIR=WEB-INF\lib
set CP=%LIB_DIR%\*

REM 创建输出目录
if not exist %BIN_DIR% mkdir %BIN_DIR%

REM 编译所有 Java 文件
javac -encoding UTF-8 -cp "%CP%" -d %BIN_DIR% %SRC_DIR%\com\ta\model\*.java %SRC_DIR%\com\ta\util\*.java %SRC_DIR%\com\ta\servlet\*.java

if %ERRORLEVEL% EQU 0 (
    echo 编译成功！
    echo.
    echo 下一步：
    echo 1. 确保 WEB-INF/lib 下有 json-xxx.jar 和 servlet-api.jar
    echo 2. 配置 Tomcat 并部署
) else (
    echo 编译失败！请检查错误信息。
    echo 常见原因：
    echo 1. WEB-INF/lib 缺少 servlet-api.jar
    echo 2. WEB-INF/lib 缺少 json-xxx.jar
)

pause
