@echo off
REM TA 招聘系统 - 后端快速部署脚本
REM PowerShell 脚本用于配置后端

echo ============================================
echo TA 招聘系统 - 后端快速部署
echo ============================================
echo.

echo 第一步：验证 Java 环境
java -version
if %ERRORLEVEL% NEQ 0 (
    echo 错误：未找到 Java，请先安装 JDK 8+
    pause
    exit /b 1
)

echo.
echo 第二步：编译 Java 源代码
echo 正在编译...
javac -d WEB-INF\classes src\com\ta\model\*.java src\com\ta\util\*.java src\com\ta\servlet\*.java

if %ERRORLEVEL% EQU 0 (
    echo 编译成功！
) else (
    echo 编译失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo 第三步：检查依赖库
if not exist "WEB-INF\lib\json*.jar" (
    echo 警告：未找到 JSON 库 (json-xxx.jar)
    echo 请手动下载并放到 WEB-INF\lib\ 目录
    echo 下载地址：https://mvnrepository.com/artifact/org.json/json
    echo.
)
if not exist "WEB-INF\lib\servlet-api*.jar" (
    echo 警告：未找到 Servlet API 库 (servlet-api-xxx.jar)
    echo 请从 Tomcat\lib 复制 servlet-api.jar 到 WEB-INF\lib\
    echo.
)
if exist "WEB-INF\lib\json*.jar" if exist "WEB-INF\lib\servlet-api*.jar" (
    echo 已检测到 JSON 库
)

echo.
echo 第四步：部署说明
echo.
echo 1. 将此 backend 目录内容复制到 Tomcat webapps 下：
echo    C:\Program Files\Tomcat\webapps\ta-system\
echo.
echo 2. 启动 Tomcat
echo.
echo 3. 访问系统：
echo    http://localhost:8080/ta-system/
echo.
echo 默认账号：
echo    账号：ta001   密码：123456   角色：TA
echo    账号：mo001   密码：123456   角色：MO
echo    账号：admin001 密码：admin123 角色：Admin
echo.

pause
