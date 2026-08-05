# 05 - MCP Server 开发实战

> 🎯 用 Python/TypeScript/Java 三种语言构建生产级 MCP Server — 从 Hello World 到多工具+多资源+流式输出的完整 Server

---

## 目录

1. [架构设计](#1-架构设计)
2. [Python MCP Server（fastmcp）](#2-python-mcp-serverfastmcp)
3. [TypeScript MCP Server（官方 SDK）](#3-typescript-mcp-server官方-sdk)
4. [Java MCP Server（Spring AI）](#4-java-mcp-serverspring-ai)
5. [Server 测试与调试](#5-server-测试与调试)

---

## 1. 架构设计

### 1.1 MCP Server 分层架构

```text
┌──────────────────────────────────────────────────────────┐
│                    MCP Server 架构                         │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌──────────────────────────────────────────────────┐    │
│  │  传输层 (Transport Layer)                          │    │
│  │  ├── StdioTransport    — 本地 stdio               │    │
│  │  ├── SSETransport      — HTTP + SSE              │    │
│  │  └── StreamableTransport — Streamable HTTP        │    │
│  └──────────────────────┬───────────────────────────┘    │
│                         │                                 │
│  ┌──────────────────────┴───────────────────────────┐    │
│  │  协议层 (Protocol Layer)                          │    │
│  │  ├── JSON-RPC Parser  — 消息解析与序列化          │    │
│  │  ├── Lifecycle        — Initialize/Shutdown       │    │
│  │  └── Capabilities     — 能力声明与协商            │    │
│  └──────────────────────┬───────────────────────────┘    │
│                         │                                 │
│  ┌──────────────────────┴───────────────────────────┐    │
│  │  业务层 (Business Layer)                          │    │
│  │  ├── Tool Registry    — 工具注册/发现/调用        │    │
│  │  ├── Resource Manager — 资源注册/URI路由/内容提供  │    │
│  │  └── Prompt Store     — Prompt模板管理            │    │
│  └──────────────────────────────────────────────────┘    │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

---

## 2. Python MCP Server（fastmcp）

### 2.1 最简示例

```python
"""
最简 MCP Server — 5 分钟上手
使用 fastmcp 库（官方推荐的高级封装）
"""
from fastmcp import FastMCP

# 创建 MCP Server
mcp = FastMCP(
    name="Weather Server",
    description="提供天气查询和预报服务"
)


@mcp.tool()
def get_weather(city: str, unit: str = "celsius") -> str:
    """获取指定城市的实时天气信息。

    Args:
        city: 城市名称，如'北京'或'Beijing'
        unit: 温度单位，celsius=摄氏度，fahrenheit=华氏度
    """
    # 模拟天气 API 调用
    weather_data = {
        "北京": {"temp": 25, "humidity": 45, "condition": "晴"},
        "上海": {"temp": 28, "humidity": 70, "condition": "多云"},
    }

    data = weather_data.get(city)
    if not data:
        return f"❌ 城市 '{city}' 不在支持范围内。支持：{', '.join(weather_data.keys())}"

    temp = data["temp"]
    if unit == "fahrenheit":
        temp = temp * 9 / 5 + 32

    return f"{city} 当前天气：{data['condition']}，{temp}°{'F' if unit == 'fahrenheit' else 'C'}，湿度 {data['humidity']}%"


@mcp.tool()
def get_forecast(city: str, days: int = 3) -> str:
    """获取城市未来天气预报。

    Args:
        city: 城市名称
        days: 预报天数（1-7天）
    """
    return f"{city} 未来{days}天预报：\n- 明天: 晴 22-30°C\n- 后天: 多云 20-28°C\n- 大后天: 小雨 18-25°C"


@mcp.resource("weather://cities")
def list_cities() -> str:
    """返回支持的城市列表"""
    import json
    return json.dumps({
        "cities": [
            {"name": "北京", "english": "Beijing"},
            {"name": "上海", "english": "Shanghai"},
            {"name": "广州", "english": "Guangzhou"},
        ]
    }, ensure_ascii=False)


# 启动 Server（默认 stdio 传输）
if __name__ == "__main__":
    mcp.run(transport="stdio")
```

### 2.2 生产级 Server（多传输 + 错误处理）

```python
"""
生产级 MCP Server 示例
特性：多传输、错误处理、进度通知、日志、资源订阅
"""
import asyncio
import logging
from typing import Optional
from fastmcp import FastMCP
from fastmcp.server.dependencies import get_http_request
from fastmcp.exceptions import ToolError

logger = logging.getLogger(__name__)

mcp = FastMCP(
    name="Enterprise Weather Server",
    version="2.1.0",
    description="企业级天气服务：实时天气、预报、预警、历史数据",

    # 生命周期钩子
    lifespan=lambda app: lifespan_handler(app)
)


async def lifespan_handler(app):
    """Server 生命周期管理"""
    logger.info("Server starting...")
    # 初始化连接池、缓存等
    yield  # Server 运行中
    logger.info("Server shutting down...")
    # 清理资源


# ===== 工具定义 =====

@mcp.tool(
    name="get_weather",
    annotations={
        "readOnlyHint": True,
        "destructiveHint": False,
        "idempotentHint": True,
        "openWorldHint": True,
    }
)
async def get_weather(
    city: str,
    unit: str = "celsius",
    include_air_quality: bool = False,
) -> str:
    """
    获取指定城市的实时天气信息。

    适用场景：
    - 用户询问"XX天气怎么样"
    - 出行前查看目的地天气
    - 需要空气质量数据时设置 include_air_quality=True

    不适用场景：
    - 查询历史天气（请使用 get_historical_weather）
    - 查询多城市对比（请多次调用本工具）

    Args:
        city: 城市名称，支持中文名和英文名
        unit: 温度单位，celsius(摄氏度)/fahrenheit(华氏度)
        include_air_quality: 是否包含空气质量指数(AQI)
    """
    try:
        # 实际业务逻辑
        result = await weather_service.query(city, unit)

        if include_air_quality:
            aqi = await air_quality_service.query(city)
            result += f"\n空气质量指数(AQI): {aqi.value} ({aqi.level})"

        return result

    except CityNotFoundError as e:
        # 业务错误：使用 ToolError 而非抛出异常
        raise ToolError(
            f"城市 '{city}' 未找到。建议：检查拼写或使用 list_cities 工具查看支持的城市"
        ) from e
    except ServiceUnavailableError:
        raise ToolError(
            "天气服务暂时不可用，请稍后重试。如紧急，可尝试使用备用服务"
        )


@mcp.tool(
    name="list_cities",
    annotations={"readOnlyHint": True, "idempotentHint": True}
)
async def list_cities(region: Optional[str] = None) -> str:
    """
    列出支持的城市列表。

    Args:
        region: 可选的区域过滤，如 asia/europe/north_america
    """
    cities = await city_service.list(region)
    return "\n".join(f"- {c.name} ({c.english_name})" for c in cities)


@mcp.tool(
    name="deploy_weather_alert",
    annotations={
        "readOnlyHint": False,
        "destructiveHint": True,
    }
)
async def deploy_weather_alert(
    city: str,
    alert_type: str,
    message: str
) -> str:
    """
    发布天气预警。⚠️ 会向用户发送通知！

    Args:
        city: 目标城市
        alert_type: 预警类型：storm(暴风雨)/heat(高温)/cold(寒潮)/flood(洪水)
        message: 预警消息内容
    """
    # 高风险操作需要额外权限检查
    await require_permission("alert:create")

    alert = await alert_service.create(city, alert_type, message)
    return f"✅ 预警已发布: ID={alert.id}, 覆盖城市={city}, 预计送达{alert.estimated_recipients}人"


# ===== 资源定义 =====

@mcp.resource("weather://config/cities")
async def cities_config() -> str:
    """城市配置文件"""
    import json
    return json.dumps(await config_service.get_cities(), ensure_ascii=False, indent=2)


@mcp.resource("weather://stats/{city}")
async def city_stats(city: str) -> str:
    """城市历史天气统计"""
    stats = await stats_service.get_city_stats(city)
    return f"城市: {city}\n年均温度: {stats.avg_temp}°C\n年均降雨: {stats.avg_rainfall}mm"


# ===== Prompt 模板 =====

@mcp.prompt()
def weather_analysis(city: str) -> str:
    """天气分析报告模板"""
    return f"""请基于以下格式生成 {city} 的天气分析报告：

## {city} 天气分析报告

### 1. 当前天气状况
（使用 get_weather 获取）

### 2. 未来趋势
（使用 get_forecast 获取）

### 3. 建议
- 出行建议
- 穿衣建议
- 健康提示
"""


# ===== 启动 =====

if __name__ == "__main__":
    import sys

    transport = sys.argv[1] if len(sys.argv) > 1 else "stdio"

    if transport == "sse":
        mcp.run(transport="sse", host="0.0.0.0", port=8080)
    elif transport == "streamable-http":
        mcp.run(transport="streamable-http", host="0.0.0.0", port=8080)
    else:
        mcp.run(transport="stdio")
```

---

## 3. TypeScript MCP Server（官方 SDK）

### 3.1 生产级 TypeScript Server

```typescript
/**
 * 企业级 MCP Server — TypeScript 实现
 * 使用 @modelcontextprotocol/sdk
 */
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  ListResourcesRequestSchema,
  ReadResourceRequestSchema,
  ListPromptsRequestSchema,
  GetPromptRequestSchema,
  Tool,
  ErrorCode,
  McpError,
} from "@modelcontextprotocol/sdk/types.js";

// ===== Server 初始化 =====

const server = new Server(
  {
    name: "enterprise-weather-server",
    version: "2.1.0",
  },
  {
    capabilities: {
      tools: { listChanged: true },
      resources: { subscribe: true, listChanged: true },
      prompts: { listChanged: false },
      logging: {},
    },
  }
);

// ===== 数据库连接池 =====
import postgres from "postgres";
const sql = postgres(process.env.DATABASE_URL!);

// ===== 工具定义 =====

const TOOLS: Tool[] = [
  {
    name: "get_weather",
    description:
      "获取指定城市的实时天气信息。适用：用户询问天气、出行规划",
    inputSchema: {
      type: "object",
      properties: {
        city: {
          type: "string",
          description: "城市名称，如'北京'或'Beijing'",
        },
        unit: {
          type: "string",
          enum: ["celsius", "fahrenheit"],
          description: "温度单位",
          default: "celsius",
        },
        include_air_quality: {
          type: "boolean",
          description: "是否包含空气质量指数",
          default: false,
        },
      },
      required: ["city"],
    },
    annotations: {
      readOnlyHint: true,
      destructiveHint: false,
      idempotentHint: true,
    },
  },
  {
    name: "deploy_weather_alert",
    description:
      "发布天气预警。⚠️ 会向用户发送通知！使用前需确认",
    inputSchema: {
      type: "object",
      properties: {
        city: { type: "string", description: "目标城市" },
        alert_type: {
          type: "string",
          enum: ["storm", "heat", "cold", "flood"],
          description: "预警类型",
        },
        message: { type: "string", description: "预警消息内容" },
      },
      required: ["city", "alert_type", "message"],
    },
    annotations: {
      readOnlyHint: false,
      destructiveHint: true,
      idempotentHint: false,
    },
  },
  {
    name: "list_cities",
    description: "列出支持的城市列表",
    inputSchema: {
      type: "object",
      properties: {
        region: {
          type: "string",
          description: "区域过滤：asia/europe/north_america",
        },
      },
    },
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
    },
  },
];

// ===== 请求处理器 =====

// tools/list
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return { tools: TOOLS };
});

// tools/call
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case "get_weather": {
        const { city, unit, include_air_quality } = args as any;

        // 参数验证
        if (!city || typeof city !== "string") {
          throw new McpError(
            ErrorCode.InvalidParams,
            "city 参数是必填的字符串"
          );
        }

        // 调用天气 API
        const weather = await fetchWeather(city, unit);

        let result = `${city} 当前天气：${weather.condition}，${weather.temp}°${unit === "fahrenheit" ? "F" : "C"}，湿度 ${weather.humidity}%`;

        if (include_air_quality) {
          const aqi = await fetchAirQuality(city);
          result += `\n空气质量指数(AQI): ${aqi.value} (${aqi.level})`;
        }

        return {
          content: [{ type: "text", text: result }],
          isError: false,
        };
      }

      case "deploy_weather_alert": {
        const { city, alert_type, message } = args as any;

        // 高风险操作：额外的安全检查
        await validateAlertPermission(city, alert_type);

        const alert = await createAlert(city, alert_type, message);

        return {
          content: [
            {
              type: "text",
              text: `✅ 预警已发布: ID=${alert.id}, 覆盖城市=${city}`,
            },
          ],
          isError: false,
        };
      }

      case "list_cities": {
        const { region } = args as any;
        const cities = await queryCities(region);
        return {
          content: [
            {
              type: "text",
              text: cities.map((c) => `- ${c.name} (${c.english})`).join("\n"),
            },
          ],
          isError: false,
        };
      }

      default:
        throw new McpError(ErrorCode.MethodNotFound, `未知工具: ${name}`);
    }
  } catch (error) {
    // 业务错误 → 工具级错误
    if (error instanceof McpError) throw error;

    return {
      content: [
        {
          type: "text",
          text: `❌ 执行失败: ${(error as Error).message}`,
        },
      ],
      isError: true,
    };
  }
});

// ===== 资源处理器 =====

server.setRequestHandler(ListResourcesRequestSchema, async () => {
  return {
    resources: [
      {
        uri: "weather://config/cities",
        name: "城市配置",
        description: "当前支持的城市列表和配置",
        mimeType: "application/json",
      },
      {
        uri: "weather://stats/{city}",
        name: "城市统计",
        description: "指定城市的历史天气统计数据",
        mimeType: "text/plain",
      },
    ],
  };
});

server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
  const { uri } = request.params;

  if (uri === "weather://config/cities") {
    const cities = await queryCities();
    return {
      contents: [
        {
          uri,
          mimeType: "application/json",
          text: JSON.stringify(cities, null, 2),
        },
      ],
    };
  }

  if (uri.startsWith("weather://stats/")) {
    const city = uri.replace("weather://stats/", "");
    const stats = await getCityStats(city);
    return {
      contents: [
        {
          uri,
          mimeType: "text/plain",
          text: `城市: ${city}\n年均温度: ${stats.avgTemp}°C`,
        },
      ],
    };
  }

  throw new McpError(ErrorCode.InvalidParams, `未知资源: ${uri}`);
});

// ===== 启动 =====

async function main() {
  const transport = process.argv[2] || "stdio";

  if (transport === "streamable-http") {
    const httpTransport = new StreamableHTTPServerTransport({
      port: 8080,
      // OAuth 配置
      // auth: { type: "oauth2", ... }
    });
    await server.connect(httpTransport);
    console.log("MCP Server running on http://localhost:8080/mcp");
  } else {
    const stdioTransport = new StdioServerTransport();
    await server.connect(stdioTransport);
    console.error("MCP Server running on stdio");  // stderr，不影响 stdout 协议通信
  }
}

main().catch(console.error);
```

---

## 4. Java MCP Server（Spring AI）

### 4.1 Spring AI MCP Server

```java
/**
 * Spring AI MCP Server — 生产级实现
 * 依赖：spring-ai-starter-mcp-server
 */
package com.example.mcp;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class WeatherMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherMcpServerApplication.class, args);
    }

    // ===== 工具注册 =====

    @Bean
    public McpServerFeatures.SyncToolSpecification getWeatherTool(WeatherService weatherService) {
        // 工具 Schema 定义
        var schema = McpSchema.Tool.builder()
            .name("get_weather")
            .description("""
                获取指定城市的实时天气信息。
                适用：用户询问天气、出行前查询。
                不适用：历史天气数据查询
                """)
            .inputSchema(JsonSchema.builder()
                .type("object")
                .properties(Map.of(
                    "city", JsonSchema.builder()
                        .type("string")
                        .description("城市名称，如'北京'或'Beijing'")
                        .build(),
                    "unit", JsonSchema.builder()
                        .type("string")
                        .enumValues(List.of("celsius", "fahrenheit"))
                        .description("温度单位")
                        .defaultValue("celsius")
                        .build(),
                    "include_air_quality", JsonSchema.builder()
                        .type("boolean")
                        .description("是否包含空气质量指数")
                        .defaultValue("false")
                        .build()
                ))
                .required(List.of("city"))
                .build()
            )
            .annotations(ToolAnnotations.builder()
                .readOnlyHint(true)
                .destructiveHint(false)
                .idempotentHint(true)
                .build()
            )
            .build();

        // 工具执行逻辑
        return new McpServerFeatures.SyncToolSpecification(
            schema,
            (exchange, request) -> {
                Map<String, Object> args = request.params().arguments();

                String city = (String) args.get("city");
                String unit = (String) args.getOrDefault("unit", "celsius");
                boolean includeAQI = (boolean) args.getOrDefault("include_air_quality", false);

                try {
                    // 业务逻辑
                    WeatherData data = weatherService.query(city, unit);

                    StringBuilder result = new StringBuilder();
                    result.append(String.format(
                        "%s 当前天气：%s，%.1f°%s，湿度 %d%%",
                        city, data.condition(), data.temp(),
                        "fahrenheit".equals(unit) ? "F" : "C",
                        data.humidity()
                    ));

                    if (includeAQI) {
                        AirQuality aqi = weatherService.queryAirQuality(city);
                        result.append(String.format(
                            "\n空气质量指数(AQI): %d (%s)",
                            aqi.value(), aqi.level()
                        ));
                    }

                    return new CallToolResult(
                        List.of(new TextContent(result.toString())),
                        false  // isError = false
                    );

                } catch (CityNotFoundException e) {
                    // 业务错误：返回 isError=true
                    return new CallToolResult(
                        List.of(new TextContent(
                            "❌ " + e.getMessage() + "\n请使用 list_cities 查看支持的城市"
                        )),
                        true   // isError = true
                    );
                }
            }
        );
    }

    // ===== 资源注册 =====

    @Bean
    public McpServerFeatures.SyncResourceSpecification citiesResource() {
        var resource = new Resource(
            "weather://config/cities",
            "城市配置",
            "当前支持的城市列表和配置",
            "application/json",
            null
        );

        return new McpServerFeatures.SyncResourceSpecification(
            resource,
            (exchange, request) -> new ReadResourceResult(
                List.of(new TextResourceContents(
                    request.params().uri(),
                    "application/json",
                    """
                    {
                      "cities": [
                        {"name": "北京", "english": "Beijing"},
                        {"name": "上海", "english": "Shanghai"}
                      ]
                    }
                    """
                ))
            )
        );
    }

    // ===== MCP Server 配置 =====

    @Bean
    public McpServer mcpServer(
            List<McpServerFeatures.SyncToolSpecification> tools,
            List<McpServerFeatures.SyncResourceSpecification> resources) {

        return McpServer.builder()
            .serverInfo(new Implementation("weather-server", "2.1.0"))
            .capabilities(ServerCapabilities.builder()
                .tools(ToolsCapability.builder()
                    .listChanged(true)
                    .build())
                .resources(ResourcesCapability.builder()
                    .subscribe(true)
                    .listChanged(true)
                    .build())
                .logging(new LoggingCapability())
                .build()
            )
            .tools(tools)
            .resources(resources)
            .build();
    }
}

// ===== 业务模型 =====
record WeatherData(String condition, double temp, int humidity) {}
record AirQuality(int value, String level) {}
```

---

## 5. Server 测试与调试

### 5.1 MCP Inspector

```bash
# MCP 官方调试工具
npx @modelcontextprotocol/inspector

# 连接到你的 Server
# Inspector 提供可视化界面：
# - 查看 tools/list 结果
# - 手动调用 tools/call
# - 查看 resources
# - 模拟 Client 交互
```

### 5.2 单元测试

```python
# Python MCP Server 测试
import pytest
from fastmcp.testing import create_test_client

@pytest.mark.asyncio
async def test_get_weather_success():
    client = await create_test_client(mcp)  # mcp 是你定义的 FastMCP 实例

    result = await client.call_tool("get_weather", {
        "city": "北京",
        "unit": "celsius"
    })

    assert result.isError == False
    assert "北京" in result.content[0].text
    assert "°C" in result.content[0].text

@pytest.mark.asyncio
async def test_get_weather_invalid_city():
    client = await create_test_client(mcp)

    result = await client.call_tool("get_weather", {
        "city": "火星"
    })

    assert result.isError == True
    assert "不在支持范围内" in result.content[0].text
```

> 🎯 **核心要点**：Python 适合快速原型（fastmcp 封装最友好）、TypeScript 适合全栈 Node.js 项目（官方 SDK）、Java 适合 Spring 生态企业项目。三种语言都支持 stdio 和 Streamable HTTP 双传输

---

**上一模块**：[04 - 传输层与通信机制](./04-传输层与通信机制.md)  
**下一模块**：[06 - MCP Client 开发实战](./06-MCP%20Client开发实战.md)  
**返回总览**：[00 - MCP 知识体系总览](./00-MCP知识体系总览.md)
