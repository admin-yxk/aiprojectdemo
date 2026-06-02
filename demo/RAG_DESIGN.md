# RAG 功能设计文档

## 1. 背景与目标

当前项目是一个基于 Java 17、Spring Boot、Maven Wrapper 的 AI 对话应用，已经具备以下能力：

- `/agent`：普通非流式 AI 对话接口。
- `/agent/stream`：基于 `text/event-stream` 的流式 AI 对话接口。
- `WebClientConfig`：统一配置 DeepSeek API 访问地址。
- `AiResServiceImpl`、`AiStreamServiceImpl`：负责组装聊天请求并调用 DeepSeek Chat Completions 接口。

RAG 的目标是在现有 AI 对话能力前增加“知识检索增强”流程，使模型回答时可以引用本地或业务知识库内容，减少纯模型幻觉，并支持后续扩展到文件上传、知识库管理、引用来源展示等能力。

本设计优先考虑在当前项目结构上渐进式扩展，不一次性引入过重框架。

## 2. 文件格式选择与分割建议

### 2.1 推荐优先级

| 优先级 | 文件格式 | 是否推荐 | 原因 |
| --- | --- | --- | --- |
| 1 | Markdown `.md` | 强烈推荐 | 标题层级清晰，天然适合按章节分割；保留语义结构；对代码、列表、表格支持好。 |
| 2 | 纯文本 `.txt` | 推荐 | 解析简单、稳定，适合 FAQ、制度、说明类文档；但缺少结构，需要依赖长度和段落分割。 |
| 3 | HTML `.html` | 推荐但需清洗 | 有标题、段落、列表等结构；需要去除导航、脚本、样式等噪声。 |
| 4 | Word `.docx` | 可用 | 常见办公格式，可提取段落、标题、表格；需要 Apache POI 或 Tika。 |
| 5 | PDF `.pdf` | 谨慎使用 | 普及度高，但文本顺序、页眉页脚、表格和扫描件处理复杂；扫描件还需要 OCR。 |
| 6 | Excel `.xlsx` / CSV | 场景化使用 | 适合结构化问答、指标解释、产品清单；更适合转成行级记录或 Markdown 表格后入库。 |

### 2.2 最适合 RAG 分割的格式

首选 Markdown。

原因：

- `#`、`##`、`###` 可以作为天然的分割边界。
- 一个 chunk 可以携带标题路径，例如“产品手册 > 安装部署 > Windows 部署”。
- 代码块、列表、表格不容易被错误拆散。
- 后续展示引用来源时，可以精确展示章节标题。

如果业务文档现在是 Word 或 PDF，建议在入库前转换为 Markdown 或结构化文本，再进行分割和向量化。RAG 系统不应直接依赖原始文件格式回答问题，而应依赖清洗后的标准化文本。

### 2.3 分割策略

推荐采用“结构优先 + 长度兜底”的混合分割策略：

1. Markdown 按标题层级分割，优先保留完整小节。
2. 单个小节过长时，按段落继续切分。
3. 段落仍然过长时，按句子或固定字符长度切分。
4. chunk 之间保留 10% 到 20% 的重叠，避免上下文断裂。
5. 每个 chunk 保存元数据，至少包括文件名、标题路径、段落序号、原始位置、知识库 ID。

建议初始参数：

| 参数 | 建议值 | 说明 |
| --- | --- | --- |
| chunkSize | 800 到 1200 中文字符 | 兼顾召回精度和上下文完整性。 |
| chunkOverlap | 100 到 200 中文字符 | 避免重要信息跨 chunk 被截断。 |
| topK | 3 到 5 | 每次问题召回的片段数量。 |
| minScore | 0.35 到 0.5 | 低于阈值时不强行引用知识库。 |

## 3. 总体架构

### 3.1 调用链路

```text
用户问题
  |
  v
Controller
  |
  v
RagService
  |
  +-- QueryRewriteService        可选：问题改写、提取检索关键词
  |
  +-- EmbeddingService           将用户问题转成向量
  |
  +-- VectorStoreService         在向量库中召回相关 chunk
  |
  +-- PromptBuilder              将问题、历史上下文、召回片段组装成提示词
  |
  v
AiResService / AiStreamService
  |
  v
DeepSeek Chat Completions
```

### 3.2 离线入库链路

```text
原始文件
  |
  v
DocumentParser
  |
  v
DocumentCleaner
  |
  v
ChunkSplitter
  |
  v
EmbeddingService
  |
  v
VectorStoreService
  |
  v
MySQL / 向量库
```

## 4. 建议项目结构

在现有 `com.example.demo` 主包下新增以下包：

```text
src/main/java/com/example/demo
+-- controller
|   +-- RagController.java
|   +-- KnowledgeController.java
+-- dto
|   +-- RagChatRequest.java
|   +-- RagChatResponse.java
|   +-- KnowledgeImportRequest.java
|   +-- RetrievedChunk.java
+-- entity
|   +-- KnowledgeDocument.java
|   +-- KnowledgeChunk.java
+-- service
|   +-- RagService.java
|   +-- KnowledgeImportService.java
|   +-- DocumentParserService.java
|   +-- ChunkSplitterService.java
|   +-- EmbeddingService.java
|   +-- VectorStoreService.java
+-- service/impl
|   +-- RagServiceImpl.java
|   +-- KnowledgeImportServiceImpl.java
|   +-- MarkdownDocumentParserServiceImpl.java
|   +-- DefaultChunkSplitterServiceImpl.java
|   +-- DeepSeekEmbeddingServiceImpl.java
|   +-- MysqlVectorStoreServiceImpl.java
+-- config
|   +-- RagProperties.java
```

说明：

- `controller`：提供 RAG 对话和知识库导入接口。
- `dto`：定义接口入参、出参和召回片段对象。
- `entity`：定义文档和 chunk 的持久化结构。
- `service`：拆分 RAG 各阶段能力，避免把解析、向量化、检索和对话全部塞进一个类。
- `config`：集中维护 chunk 大小、topK、score 阈值、模型名称等配置。

## 5. 核心模块设计

### 5.1 KnowledgeImportService

职责：

- 接收知识库导入请求。
- 调用 `DocumentParserService` 解析文件内容。
- 调用 `ChunkSplitterService` 生成 chunk。
- 调用 `EmbeddingService` 生成向量。
- 调用 `VectorStoreService` 保存 chunk 和向量。

建议接口：

```java
public interface KnowledgeImportService {
    String importDocument(KnowledgeImportRequest request);
}
```

### 5.2 DocumentParserService

职责：

- 将不同格式文件解析成统一的标准化文档对象。
- 第一阶段建议只支持 Markdown 和 TXT。
- 后续再扩展 DOCX、PDF、HTML。

建议第一阶段支持：

- `.md`
- `.txt`

不建议第一阶段直接做 PDF，除非当前业务知识主要来自 PDF。

### 5.3 ChunkSplitterService

职责：

- 根据文档结构切分 chunk。
- 为 chunk 添加标题路径、顺序号、来源信息。
- 保证 chunk 不超过配置的最大长度。

Markdown 分割规则：

1. 按标题识别章节。
2. 标题路径写入 metadata。
3. 章节正文过长时按段落切分。
4. 保留 overlap。

TXT 分割规则：

1. 优先按空行切分段落。
2. 段落过长时按句号、问号、感叹号切分。
3. 仍然过长时按固定长度切分。

### 5.4 EmbeddingService

职责：

- 调用 embedding 模型，把文本转换为向量。
- 对外隐藏具体模型供应商。

建议接口：

```java
public interface EmbeddingService {
    List<Double> embed(String text);

    List<List<Double>> embedBatch(List<String> texts);
}
```

注意：

- 当前项目已经使用 DeepSeek Chat API，但 DeepSeek 是否提供适合当前场景的 embedding API 需要以实际接口为准。
- 如果 DeepSeek 当前账号或模型不支持 embedding，可以改用 OpenAI、阿里云 DashScope、智谱、BGE 本地模型等。
- 设计上不要把 embedding 实现绑定死在 RAG 主流程里。

### 5.5 VectorStoreService

职责：

- 保存 chunk 文本、metadata 和向量。
- 根据问题向量做相似度检索。

建议接口：

```java
public interface VectorStoreService {
    void saveChunks(List<KnowledgeChunk> chunks);

    List<RetrievedChunk> search(List<Double> queryVector, int topK, double minScore);
}
```

第一阶段可选方案：

- 小规模验证：MySQL 保存 chunk，内存中计算余弦相似度。
- 中等规模：MySQL + 向量字段或独立向量库。
- 生产推荐：Milvus、Qdrant、Elasticsearch 向量检索、PostgreSQL + pgvector。

当前项目已经有 MySQL 驱动，但还没有 JPA/MyBatis 依赖。若要落库，需要新增数据访问方案。第一阶段如果只是验证 RAG 流程，可以先用内存向量库，后续再替换。

### 5.6 RagService

职责：

- 接收用户问题。
- 检索相关知识片段。
- 组装增强提示词。
- 复用现有 AI 对话服务完成回答。

建议提示词结构：

```text
你是一个严谨的知识库问答助手。
只能根据【知识库片段】回答问题。
如果知识库片段中没有答案，请明确回答“知识库中没有找到相关信息”，不要编造。

【知识库片段】
[1] 来源：{fileName} > {headingPath}
{chunkText}

[2] 来源：{fileName} > {headingPath}
{chunkText}

【用户问题】
{question}
```

## 6. 接口设计

### 6.1 知识库导入接口

第一阶段可以先支持服务端本地文件路径导入，方便开发验证。

```http
POST /knowledge/import
Content-Type: application/json
```

请求示例：

```json
{
  "knowledgeBaseId": "default",
  "filePath": "E:/project_idea/demo/docs/product.md",
  "fileName": "product.md"
}
```

响应示例：

```json
{
  "documentId": "doc_001",
  "chunkCount": 36,
  "status": "SUCCESS"
}
```

后续可扩展为 multipart 文件上传：

```http
POST /knowledge/upload
Content-Type: multipart/form-data
```

### 6.2 RAG 普通问答接口

```http
POST /rag/agent
Content-Type: application/json
```

请求示例：

```json
{
  "knowledgeBaseId": "default",
  "model": "deepseek-chat",
  "question": "系统如何配置 DeepSeek API 地址？",
  "topK": 5
}
```

响应示例：

```json
{
  "answer": "可以在 param.yml 中配置 deepseek.dep-url，并由 WebClientConfig 读取后创建 WebClient。",
  "sources": [
    {
      "documentId": "doc_001",
      "fileName": "RAG_DESIGN.md",
      "headingPath": "总体架构 > 调用链路",
      "score": 0.82
    }
  ]
}
```

### 6.3 RAG 流式问答接口

```http
POST /rag/agent/stream
Content-Type: application/json
Accept: text/event-stream
```

该接口复用现有 `/agent/stream` 的响应方式，但在调用模型前先完成检索和提示词增强。

## 7. 数据模型设计

### 7.1 knowledge_document

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | varchar | 文档 ID |
| knowledge_base_id | varchar | 知识库 ID |
| file_name | varchar | 原始文件名 |
| file_type | varchar | 文件类型 |
| file_path | varchar | 原始文件路径或对象存储路径 |
| content_hash | varchar | 内容 hash，用于避免重复导入 |
| status | varchar | IMPORTING / SUCCESS / FAILED |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 7.2 knowledge_chunk

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | varchar | chunk ID |
| document_id | varchar | 文档 ID |
| knowledge_base_id | varchar | 知识库 ID |
| chunk_index | int | 文档内 chunk 顺序 |
| heading_path | varchar | 标题路径 |
| content | text | chunk 文本 |
| token_count | int | 估算 token 数 |
| metadata_json | text | 扩展元数据 |
| embedding | vector/json/blob | 向量数据，取决于存储方案 |
| created_at | datetime | 创建时间 |

如果第一阶段使用内存向量库，可以先不建表，但实体结构仍建议按上述模型设计，便于后续落库。

## 8. 配置设计

建议在 `application.properties` 或 `param.yml` 中新增：

```yaml
rag:
  chunk-size: 1000
  chunk-overlap: 150
  top-k: 5
  min-score: 0.4
  embedding-model: your-embedding-model
  vector-store-type: memory
```

配置说明：

- `chunk-size`：单个 chunk 的目标长度。
- `chunk-overlap`：相邻 chunk 重叠长度。
- `top-k`：默认召回数量。
- `min-score`：最低相似度分数。
- `embedding-model`：embedding 模型名称。
- `vector-store-type`：向量存储类型，第一阶段可以是 `memory`。

注意：当前 `application.properties` 中使用了 `//` 作为注释，这不是标准 properties 注释语法。后续建议改为 `#`，避免配置解析问题。

## 9. 与现有代码的集成方式

### 9.1 普通回答链路

当前 `AiResServiceImpl` 会直接从请求中取最后一条 `user` 消息，然后调用模型。

RAG 接入后不建议直接改动 `/agent` 的原有行为，而是新增 `/rag/agent`：

1. 从 `RagChatRequest.question` 获取问题。
2. 调用 `EmbeddingService.embed(question)`。
3. 调用 `VectorStoreService.search(...)`。
4. 调用 `PromptBuilder` 生成增强后的用户消息。
5. 构造 `AiRequest`。
6. 调用现有 `AiResService.aiAgentReturn(aiRequest)`。

这样可以保留原有普通聊天接口，降低回归风险。

### 9.2 流式回答链路

新增 `/rag/agent/stream`，流程与普通回答一致，只是最后调用 `AiStreamService.aiAgentStream(aiRequest)`。

注意：

- 知识检索应在流式输出开始前完成。
- 来源信息可以在流结束后单独返回，也可以在开始时先发送一个 sources 事件。
- 如果前端暂时只接收纯文本，可以第一阶段只返回模型文本，来源展示后续再扩展。

## 10. 实施计划

### 第一阶段：最小可用 RAG

目标：跑通 Markdown/TXT 入库、向量化、检索、增强回答。

任务：

1. 新增 RAG DTO、Service 接口和实现类。
2. 新增 Markdown/TXT 文档解析。
3. 新增默认 chunk 分割器。
4. 新增内存向量存储实现。
5. 新增 embedding 实现，接入实际 embedding API。
6. 新增 `/knowledge/import`。
7. 新增 `/rag/agent`。
8. 为分割器、检索服务、RAG 服务添加测试。

### 第二阶段：持久化知识库

目标：应用重启后知识库不丢失。

任务：

1. 引入 JPA 或 MyBatis。
2. 新增 `knowledge_document`、`knowledge_chunk` 表。
3. 保存文档、chunk、向量和 metadata。
4. 增加重复导入检测。
5. 增加删除文档、重建索引能力。

### 第三阶段：生产能力增强

目标：提升回答质量、可观测性和可维护性。

任务：

1. 支持 DOCX、HTML、PDF。
2. 接入专业向量库。
3. 增加 rerank 模型。
4. 增加来源引用展示。
5. 增加知识库权限隔离。
6. 增加检索日志和回答质量评估。
7. 增加批量导入和异步任务。

## 11. 测试设计

### 11.1 单元测试

需要覆盖：

- Markdown 按标题分割是否正确。
- TXT 按段落和长度分割是否正确。
- chunk overlap 是否生效。
- 空文档、超短文档、超长段落是否处理稳定。
- 向量相似度计算是否正确。
- topK 和 minScore 是否生效。

### 11.2 集成测试

需要覆盖：

- 导入文档后可以检索到相关 chunk。
- `/rag/agent` 可以正确组装知识库片段。
- 没有召回结果时，模型提示词要求不得编造。
- 流式接口在检索完成后正常输出。

### 11.3 手工验证问题

可以准备一个 `docs/sample.md`：

```markdown
# DeepSeek 配置

## API 地址

DeepSeek API 地址配置在 param.yml 的 deepseek.dep-url 中。

## API Key

DeepSeek API Key 配置在 param.yml 的 deepseek.api-key 中。
```

验证问题：

- “DeepSeek 的 API 地址在哪里配置？”
- “API Key 在哪个配置项？”
- “项目如何配置不存在的功能？”预期回答知识库中没有找到相关信息。

## 12. 风险与注意事项

1. 当前 `param.yml` 中包含明文 API Key，建议改为环境变量或本地私有配置，不要提交真实密钥。
2. 当前普通和流式服务各自维护内存 `messages`，且所有用户共享同一份上下文；RAG 场景下建议引入会话 ID 或直接让 RAG 接口无状态。
3. PDF 解析质量不稳定，第一阶段不建议作为主格式。
4. chunk 太大会降低召回精度，chunk 太小会丢失上下文，需要结合业务文档调参。
5. 检索结果不能直接全部塞进 prompt，需要受模型上下文长度限制。
6. 如果使用内存向量库，应用重启后数据会丢失，只适合验证。
7. 如果知识库有权限要求，检索时必须带上用户权限或租户条件，避免越权召回。

## 13. 推荐结论

当前项目做 RAG，建议按以下路线推进：

1. 文档格式优先使用 Markdown，其次 TXT。
2. 第一阶段只支持 Markdown/TXT，先跑通完整 RAG 链路。
3. 新增 `/rag/agent` 和 `/rag/agent/stream`，不破坏现有 `/agent` 和 `/agent/stream`。
4. 第一阶段可使用内存向量库验证流程，生产化前再接入持久化向量库。
5. 分割策略采用“标题结构优先，长度限制兜底，chunk overlap 防断裂”。
6. 所有 chunk 必须保存来源 metadata，便于回答时展示引用来源和排查问题。
