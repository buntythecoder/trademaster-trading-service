# TradeMaster MCP Server Guide

## Overview

The TradeMaster Model Context Protocol (MCP) Server exposes trading capabilities to external AI agents (like Claude) through a standardized protocol. This enables AI agents to interact with the trading platform programmatically using tools and resources.

## Architecture

### Components

1. **MCPServer** - Main MCP server implementation
   - Coordinates tool execution and resource access
   - Manages sessions and context
   - Implements MCP protocol specification

2. **MCPToolRegistry** - Tool management and execution
   - Registers AgentOS capabilities as MCP tools
   - Validates parameters with JSON Schema
   - Executes tools with proper error handling

3. **MCPResourceProvider** - Resource access management
   - Provides read-only access to trading data
   - Supports portfolio, market, risk, and analytics resources
   - URI-based resource addressing

4. **MCPSession** - Session state management
   - Tracks tool call history
   - Maintains conversation context
   - Manages session lifecycle

## Available Tools

### Market Data Tools

#### `get_realtime_quote`
Get real-time market quote for a symbol.

**Parameters:**
- `symbol` (string, required): Stock symbol (e.g., RELIANCE, TCS)

**Example:**
```json
{
  "toolName": "get_realtime_quote",
  "parameters": {
    "symbol": "RELIANCE"
  }
}
```

**Response:**
```json
{
  "symbol": "RELIANCE",
  "price": 2500.50,
  "change": 25.30,
  "changePercent": 1.02,
  "volume": 1250000,
  "timestamp": 1704106800000
}
```

#### `get_market_status`
Get current market status and trading hours.

**Parameters:** None

**Response:**
```json
{
  "status": "OPEN",
  "exchange": "NSE",
  "openTime": "09:15",
  "closeTime": "15:30",
  "isTrading": true
}
```

### Order Strategy Tools

#### `place_stop_loss_order`
Place a stop-loss order with automatic trigger.

**Parameters:**
- `symbol` (string, required): Stock symbol
- `quantity` (number, required): Order quantity
- `stopPrice` (number, required): Stop trigger price
- `orderType` (string, optional): Order type (MARKET or LIMIT)

**Example:**
```json
{
  "toolName": "place_stop_loss_order",
  "parameters": {
    "symbol": "RELIANCE",
    "quantity": 50,
    "stopPrice": 2450.00,
    "orderType": "MARKET"
  }
}
```

**Response:**
```json
{
  "orderId": "SL-A1B2C3D4",
  "status": "ACCEPTED",
  "symbol": "RELIANCE",
  "quantity": 50,
  "stopPrice": 2450.00,
  "timestamp": 1704106800000
}
```

#### `place_trailing_stop_order`
Place a trailing stop order with dynamic adjustment.

**Parameters:**
- `symbol` (string, required): Stock symbol
- `quantity` (number, required): Order quantity
- `trailPercent` (number, required): Trail percentage
- `trailAmount` (number, optional): Trail amount

### AI Analysis Tools

#### `analyze_technical_indicators`
Calculate technical indicators (RSI, MACD, Bollinger Bands) for a symbol.

**Parameters:**
- `symbol` (string, required): Stock symbol
- `periods` (number, optional): Number of historical periods (default: 50)

**Response:**
```json
{
  "symbol": "RELIANCE",
  "rsi": 65.5,
  "macd": 12.3,
  "macdSignal": 10.8,
  "macdHistogram": 1.5,
  "bollingerUpper": 2550.0,
  "bollingerMiddle": 2500.0,
  "bollingerLower": 2450.0,
  "trend": "BULLISH",
  "momentum": "BUY",
  "signalStrength": 75
}
```

#### `analyze_market_sentiment`
Analyze market sentiment using price action and volume patterns.

**Parameters:**
- `symbol` (string, required): Stock symbol
- `periods` (number, optional): Number of historical periods (default: 50)

**Response:**
```json
{
  "symbol": "RELIANCE",
  "overallSentiment": "BULLISH",
  "buyPressure": 72.5,
  "sellPressure": 27.5,
  "marketStrength": 68.0,
  "volatilityScore": 45.0,
  "volumeScore": 65.0,
  "confidenceScore": 78,
  "sentimentReason": "Positive sentiment driven by 1.5% price movement and 72.5% buy pressure"
}
```

#### `assess_trade_risk`
Calculate risk metrics (Sharpe ratio, VaR, max drawdown) for a trading opportunity.

**Parameters:**
- `symbol` (string, required): Stock symbol
- `portfolioValue` (number, required): Current portfolio value
- `periods` (number, optional): Number of historical periods (default: 50)

**Response:**
```json
{
  "symbol": "RELIANCE",
  "riskLevel": "MEDIUM",
  "riskScore": 45,
  "sharpeRatio": 1.25,
  "maxDrawdown": 12.5,
  "volatility": 22.0,
  "valueAtRisk": 5000.0,
  "recommendedPosition": 0.05,
  "stopLossPrice": 2450.0,
  "takeProfitPrice": 2650.0
}
```

#### `get_trade_recommendation`
Get AI-powered trade recommendation combining technical, sentiment, and risk analysis.

**Parameters:**
- `symbol` (string, required): Stock symbol
- `portfolioValue` (number, required): Current portfolio value
- `periods` (number, optional): Number of historical periods (default: 50)

**Response:**
```json
{
  "recommendationId": "REC-A1B2C3D4",
  "symbol": "RELIANCE",
  "action": "BUY",
  "entryPrice": 2495.0,
  "targetPrice": 2650.0,
  "stopLoss": 2450.0,
  "quantity": 20,
  "overallScore": 75,
  "confidenceLevel": 78,
  "strategy": "MOMENTUM",
  "timeframe": "SHORT_TERM",
  "primaryReason": "Buy opportunity with BULLISH trend, BULLISH sentiment, risk level MEDIUM",
  "supportingFactors": [
    "Strong technical signal strength: 75/100",
    "High sentiment confidence: 78/100",
    "Favorable Sharpe ratio: 1.25"
  ],
  "risks": ["Standard market risks"],
  "expiresAt": 1704121200000
}
```

## Available Resources

Resources are read-only data sources accessed via URI: `trademaster://[category]/[resource-id]`

### Portfolio Resources

- `trademaster://portfolio/summary` - Complete portfolio summary
- `trademaster://portfolio/positions` - All open positions with P&L

### Market Resources

- `trademaster://market/status` - Market status and trading hours
- `trademaster://market/top-gainers` - Top performing stocks

### Risk Resources

- `trademaster://risk/portfolio-metrics` - Portfolio risk metrics
- `trademaster://risk/compliance-status` - Compliance status

### Analytics Resources

- `trademaster://analytics/performance` - Performance metrics

### Order Resources

- `trademaster://orders/open` - Currently open orders
- `trademaster://orders/recent` - Recently executed orders

## API Endpoints

### Initialize Server
```http
POST /mcp/initialize
```

Get server information and capabilities.

**Response:**
```json
{
  "name": "TradeMaster Trading Service MCP Server",
  "version": "2.0.0",
  "capabilities": {
    "tools": true,
    "resources": true,
    "prompts": false,
    "logging": true
  },
  "metadata": {
    "vendor": "TradeMaster",
    "environment": "production",
    "agentCount": 6,
    "toolCount": 10,
    "resourceCount": 12
  }
}
```

### List Tools
```http
GET /mcp/tools/list
```

List all available tools with their schemas.

### Call Tool
```http
POST /mcp/tools/call
Content-Type: application/json

{
  "toolName": "get_trade_recommendation",
  "parameters": {
    "symbol": "RELIANCE",
    "portfolioValue": 100000
  },
  "sessionId": "optional-session-id"
}
```

### List Resources
```http
GET /mcp/resources/list
```

List all available resources.

### Read Resource
```http
POST /mcp/resources/read
Content-Type: application/json

{
  "uri": "trademaster://portfolio/summary",
  "sessionId": "optional-session-id"
}
```

### Health Status
```http
GET /mcp/health
```

Get server and agent health metrics.

### Create Session
```http
POST /mcp/sessions/create
Content-Type: application/json

{
  "metadata": {
    "userId": "user123",
    "clientType": "claude"
  }
}
```

## Error Codes

Following JSON-RPC 2.0 error codes:

- `-32700`: Parse error
- `-32600`: Invalid Request
- `-32601`: Method/Tool not found
- `-32602`: Invalid params
- `-32603`: Internal error
- `-32000`: Server error

## Usage Examples

### Example 1: Get Trade Recommendation

```bash
# Create session
curl -X POST http://localhost:8083/mcp/sessions/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "metadata": {
      "userId": "trader123"
    }
  }'

# Get recommendation
curl -X POST http://localhost:8083/mcp/tools/call \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "toolName": "get_trade_recommendation",
    "parameters": {
      "symbol": "RELIANCE",
      "portfolioValue": 100000
    },
    "sessionId": "session-id-from-above"
  }'
```

### Example 2: Place Stop-Loss Order

```bash
curl -X POST http://localhost:8083/mcp/tools/call \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "toolName": "place_stop_loss_order",
    "parameters": {
      "symbol": "RELIANCE",
      "quantity": 50,
      "stopPrice": 2450.00
    }
  }'
```

### Example 3: Read Portfolio Summary

```bash
curl -X POST http://localhost:8083/mcp/resources/read \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "uri": "trademaster://portfolio/summary"
  }'
```

## Integration with Claude

The MCP server enables Claude to interact with TradeMaster programmatically:

1. **Tool Discovery**: Claude can list available tools and their schemas
2. **Tool Execution**: Claude can execute trading operations
3. **Resource Access**: Claude can read trading data and portfolio information
4. **Session Management**: Claude can maintain context across multiple interactions

### Example Claude Interaction

**User**: "Analyze RELIANCE and tell me if I should buy it"

**Claude**: [Uses `analyze_technical_indicators` + `analyze_market_sentiment` + `get_trade_recommendation` tools]

**Response**: "Based on the analysis:
- Technical indicators show RSI at 65.5 (bullish momentum)
- Market sentiment is BULLISH with 72.5% buy pressure
- Risk assessment shows MEDIUM risk with Sharpe ratio of 1.25
- **Recommendation**: BUY at ₹2,495 with target ₹2,650 and stop-loss at ₹2,450
- Confidence: 78/100"

## Security

- All MCP endpoints require authentication (`@PreAuthorize`)
- Session tracking and correlation IDs for audit trails
- Parameter validation with JSON Schema
- Rate limiting and circuit breakers apply

## Monitoring

- Health endpoint: `/mcp/health`
- Actuator metrics: `/actuator/prometheus`
- Logs: Structured logging with correlation IDs
- Agent health scores tracked in real-time

## Configuration

MCP server is configured in `application.yml` under `agentos.agent.capabilities`.

Tool and resource registration happens automatically on startup via `MCPConfiguration`.
