package com.trademaster.trading.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

/**
 * Order Placement Load Test Simulation
 *
 * Validates system performance for order placement workflow under various load conditions.
 * Tests the complete order lifecycle: authentication, validation, routing, and execution.
 *
 * Test Scenarios:
 * - Normal Load: 5,000 concurrent users (50% capacity)
 * - Peak Load: 10,000 concurrent users (100% capacity)
 * - Stress Load: 15,000 concurrent users (150% capacity)
 *
 * Performance Targets:
 * - API Response Time p95: <200ms
 * - API Response Time p99: <500ms
 * - Order Processing Time: <50ms
 * - Error Rate: <0.1%
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
class OrderPlacementLoadTest extends Simulation {

  // Base URL for trading service
  private val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")

  // Load testing parameters
  private val loadProfile = System.getProperty("loadProfile", "normal") // normal, peak, stress
  private val duration = System.getProperty("duration", "30").toInt // minutes

  // HTTP protocol configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test / TradeMaster")
    .shareConnections // Connection pooling for better performance

  // Test data generators
  private val symbols = Seq("RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK", "SBIN", "ITC", "LT", "AXISBANK", "BHARTIARTL")
  private val exchanges = Seq("NSE", "BSE")
  private val orderTypes = Seq("MARKET", "LIMIT", "STOP_LOSS")
  private val sides = Seq("BUY", "SELL")
  private val timeInForces = Seq("DAY", "IOC", "FOK")

  // JWT token for authentication (in production, would be dynamically generated)
  private val authToken = System.getProperty("authToken", "test-jwt-token")

  // Order placement feeder - generates realistic order requests
  val orderFeeder = Iterator.continually(Map(
    "userId" -> Random.nextInt(10000),
    "symbol" -> symbols(Random.nextInt(symbols.length)),
    "exchange" -> exchanges(Random.nextInt(exchanges.length)),
    "orderType" -> orderTypes(Random.nextInt(orderTypes.length)),
    "side" -> sides(Random.nextInt(sides.length)),
    "quantity" -> (Random.nextInt(1000) + 1),
    "limitPrice" -> (Random.nextDouble() * 1000 + 100).formatted("%.2f"),
    "timeInForce" -> timeInForces(Random.nextInt(timeInForces.length))
  ))

  // Scenario 1: Place Market Order
  val placeMarketOrder = scenario("Place Market Order")
    .feed(orderFeeder)
    .exec(http("Place Market Order")
      .post("/api/v1/orders")
      .header("Authorization", s"Bearer $authToken")
      .body(StringBody("""{
        "userId": ${userId},
        "symbol": "${symbol}",
        "exchange": "${exchange}",
        "orderType": "MARKET",
        "side": "${side}",
        "quantity": ${quantity},
        "timeInForce": "${timeInForce}"
      }""")).asJson
      .check(status.in(200, 201))
      .check(jsonPath("$.orderId").exists)
      .check(jsonPath("$.status").in("PENDING", "EXECUTED"))
      .check(responseTimeInMillis.lte(200)) // p95 target
    )
    .pause(1.seconds, 3.seconds) // Think time between requests

  // Scenario 2: Place Limit Order
  val placeLimitOrder = scenario("Place Limit Order")
    .feed(orderFeeder)
    .exec(http("Place Limit Order")
      .post("/api/v1/orders")
      .header("Authorization", s"Bearer $authToken")
      .body(StringBody("""{
        "userId": ${userId},
        "symbol": "${symbol}",
        "exchange": "${exchange}",
        "orderType": "LIMIT",
        "side": "${side}",
        "quantity": ${quantity},
        "limitPrice": ${limitPrice},
        "timeInForce": "${timeInForce}"
      }""")).asJson
      .check(status.in(200, 201))
      .check(jsonPath("$.orderId").exists)
      .check(responseTimeInMillis.lte(200))
    )
    .pause(1.seconds, 3.seconds)

  // Scenario 3: Place Stop-Loss Order
  val placeStopLossOrder = scenario("Place Stop-Loss Order")
    .feed(orderFeeder)
    .exec(http("Place Stop-Loss Order")
      .post("/api/v1/orders")
      .header("Authorization", s"Bearer $authToken")
      .body(StringBody("""{
        "userId": ${userId},
        "symbol": "${symbol}",
        "exchange": "${exchange}",
        "orderType": "STOP_LOSS",
        "side": "${side}",
        "quantity": ${quantity},
        "limitPrice": ${limitPrice},
        "stopPrice": ${limitPrice},
        "timeInForce": "${timeInForce}"
      }""")).asJson
      .check(status.in(200, 201))
      .check(jsonPath("$.orderId").exists)
      .check(responseTimeInMillis.lte(200))
    )
    .pause(1.seconds, 3.seconds)

  // Scenario 4: Get Order Status
  val getOrderStatus = scenario("Get Order Status")
    .exec(http("Get Order Status")
      .get("/api/v1/orders/${orderId}")
      .header("Authorization", s"Bearer $authToken")
      .check(status.is(200))
      .check(jsonPath("$.orderId").exists)
      .check(responseTimeInMillis.lte(100)) // Read operations should be faster
    )
    .pause(1.seconds, 2.seconds)

  // Load profiles - different user concurrency levels
  private val normalLoad = loadProfile match {
    case "normal" => rampUsers(5000).during(5.minutes) // Normal load: 5,000 users
    case "peak" => rampUsers(10000).during(10.minutes) // Peak load: 10,000 users
    case "stress" => rampUsers(15000).during(10.minutes) // Stress load: 15,000 users
    case _ => rampUsers(5000).during(5.minutes)
  }

  // Setup scenarios with realistic distribution
  setUp(
    placeMarketOrder.inject(normalLoad).protocols(httpProtocol), // 50% market orders
    placeLimitOrder.inject(normalLoad.scale(0.35)).protocols(httpProtocol), // 35% limit orders
    placeStopLossOrder.inject(normalLoad.scale(0.15)).protocols(httpProtocol) // 15% stop-loss orders
  )
    .assertions(
      // Response time assertions (p95 and p99)
      global.responseTime.percentile3.lt(200), // p95 < 200ms
      global.responseTime.percentile4.lt(500), // p99 < 500ms
      global.successfulRequests.percent.gt(99.9) // Error rate < 0.1%
    )
    .maxDuration(duration.minutes) // Test duration from system property
}
