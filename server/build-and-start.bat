@echo off
chcp 65001 >nul
echo ========================================
echo 团队工作量管理系统 - 一键编译启动脚本
echo ========================================
echo.

REM 设置 JDK 1.8 环境
set "JAVA_HOME=C:\Program Files\Java\jdk-1.8"
set "PATH=%JAVA_HOME%\bin;%PATH%"


echo 正在检查 Java 版本...
java -version
if %errorlevel% neq 0 (
    echo.
    echo 错误：Java 1.8 未安装或路径不正确！
    echo 请确认：%JAVA_HOME% 存在。
    pause
    exit /b 1
)
echo.

echo ========================================
echo 步骤 1/2：编译并打包项目
echo ========================================
cd /d "%~dp0"
mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo 编译打包失败！请检查错误信息。
    echo ========================================
    pause
    exit /b 1
)

echo.
echo ========================================
echo 编译打包成功！
echo JAR 位置：%~dp0target\team-workload-1.0.0.jar
echo ========================================
echo.

echo ========================================
echo 步骤 2/2：启动后端服务
echo ========================================
echo 正在启动...
echo.
java -jar target\team-workload-1.0.0.jar

if %errorlevel% neq 0 (
    echo.
    echo 服务启动失败！请检查错误信息。
    pause
)
