---
apply: 按文件模式
模式: *.java,pom.xml,*.properties,*.yml,*.yaml
---



- 不分析整个项目
- 仅处理当前打开文件
- 仅在明确要求时读取其他文件
- 优先最小上下文

## Code Generation Rules

- 优先最小修改
- 不修改无关代码
- 不自动新增依赖
- 不自动升级框架版本
- 不生成过度复杂实现

## Spring Boot Rules

- 使用构造器注入
- Controller 保持轻量
- 业务逻辑放在 Service
- 使用 DTO 处理请求与响应
- 配置优先放在 application.properties

## Testing Rules

- 不自动运行测试
- 单元测试优先使用 JUnit5
- 不连接真实数据库

## Token Optimization Rules

- 优先生成简洁代码
- 避免生成重复代码
- 避免输出超长解释
- 避免分析整个仓库