# 小 One 业务闭环（Java 技术栈版）

根据简历技术栈生成：Java 17、Spring Boot、React、DeepSeek 配置、LangChain4j 依赖、RAG/Function Calling/MCP 扩展位、SSE 任务事件流。

## 启动后端

```powershell
$env:DEEPSEEK_API_KEY="你的 key"
mvn spring-boot:run
```

后端地址：`http://localhost:8080`。接口：`POST /api/v1/tasks`、`GET /api/v1/tasks/{id}`、`GET /api/v1/tasks/{id}/events`。

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

当前工具返回演示数据，真实接入时将 `TaskService.tool()` 替换为 MySQL、StarRocks、SLS、Elasticsearch 和 MCP Adapter；DeepSeek 调用可在该处注入 LangChain4j `ChatLanguageModel`，并将规划器返回改为结构化 Function Calling。
