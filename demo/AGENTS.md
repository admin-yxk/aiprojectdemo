# AGENTS.md

## 项目概览

这是一个基础的 Spring Boot 应用。

- 开发语言：Java 17
- 构建工具：Maven，使用项目内置的 Maven Wrapper
- 主包名：`com.example.demo`
- 应用入口：`src/main/java/com/example/demo/DemoApplication.java`
- 运行配置：`src/main/resources/application.properties`
- 测试源码目录：`src/test/java`

## 项目结构

```text
.
+-- pom.xml
+-- mvnw
+-- mvnw.cmd
+-- src
|   +-- main
|   |   +-- java/com/example/demo
|   |   +-- resources/application.properties
|   +-- test
|       +-- java/com/example/demo
+-- HELP.md
```

## 开发命令

优先使用 Maven Wrapper，保证不同机器上的构建行为一致。

- 在 Windows 上构建并运行测试：
  ```powershell
  .\mvnw.cmd test
  ```

- 在 Windows 上打包应用：
  ```powershell
  .\mvnw.cmd package
  ```

- 在 Windows 上本地运行应用：
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```

- 在 macOS/Linux 上构建并运行测试：
  ```sh
  ./mvnw test
  ```

## 编码约定

- 应用代码放在 `src/main/java/com/example/demo` 下。
- 测试代码放在 `src/test/java/com/example/demo` 下，并与被测试代码保持对应包路径。
- Spring 组件优先使用构造器注入。
- 配置优先放在 `application.properties` 中，只有在确实需要新 profile 或结构化配置时再扩展。
- 不要提交 `target/` 等构建生成目录。
- 只有在能提升可读性并且与附近代码风格一致时才使用 Lombok。

## 测试要求

- 新增或修改行为时，同步新增或更新测试。
- 在 Windows 环境完成改动前，运行 `.\mvnw.cmd test`。
- 如果因为依赖不可用或环境阻止下载导致测试无法运行，需要明确说明。

## 依赖说明

当前主要依赖：

- `spring-boot-starter`
- `spring-boot-devtools`
- `mysql-connector-j`
- `lombok`
- `spring-boot-starter-test`

添加依赖时，只为当前实现确实需要的库修改 `pom.xml`。
