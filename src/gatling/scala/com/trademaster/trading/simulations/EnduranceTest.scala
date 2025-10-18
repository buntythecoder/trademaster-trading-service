package com.trademaster.trading.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

/**
 * Endurance Test Simulation
 *
 * Long-duration stability testing to validate system behavior over extended periods.
 * Detects memory leaks, resource exhaustion, and gradual performance degradation.
 *
 * Test Duration: 4 hours sustained load
 * Load Level: 5,000 concurrent users (50% capacity)
 *
 * Objectives:
 * - Detect memory leaks and resource exhaustion
 * - Validate connection pool stability
 * - Test garbage collection behavior over time
 * - Verify log rotation and disk space management
 * - Validate Virtual Threads stability
 *
 * Monitoring Points:
 * - JVM heap usage trends
 * - Connection pool utilization
 * - Response time stability
 * - Error rate trends
 * - Disk space consumption
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
class EnduranceTest extends Simulation {

  private val baseUrl = System.getProperty("baseUrl", "http://localhost:8000")
  private val duration = System.getProperty("duration", "240").toInt // 4 hours default

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Endurance Test / TradeMaster")
    .shareConnections
    .connectionHeader("keep-alive") // Maintain connections for endurance
    .disableFollowRedirect // Disable redirects for performance

  private val symbols = Seq("RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK", "SBIN", "ITC", "LT", "AXISBANK", "BHARTIARTL")
  private val exchanges = Seq("NSE", "BSE")
  private val authToken = System.getProperty("authToken", "test-jwt-token")

  val orderFeeder = Iterator.continually(Map(
    "userId" -> Random.nextInt(10000),
    "symbol" -> symbols(Random.nextInt(symbols.length)),
    "exchange" -> exchanges(Random.nextInt(exchanges.length)),
    "quantity" -> (Random.nextInt(500) + 1),
    "limitPrice" -> (Random.nextDouble() * 1000 + 100).formatted("%.2f")
  ))

  // Scenario 1: Steady Order Placement
  val steadyOrderPlacement = scenario("Steady Order Placement")
    .feed(orderFeeder)
    .exec(http("Place Market Order")
      .post("/api/v1/orders")
      .header("Authorization", s"Bearer $authToken")
      .body(StringBody("""{
        "userId": ${userId},
        "symbol": "${symbol}",
        "exchange": "${exchange}",
        "orderType": "MARKET",
        "side": "BUY",
        "quantity": ${quantity},
        "timeInForce": "DAY"
      }""")).asJson
      .check(status.in(200, 201))
      .check(jsonPath("$.orderId").exists)
      .check(responseTimeInMillis.lte(200))
    )
    .pause(2.seconds, 5.seconds) // Realistic user behavior

  // Scenario 2: Portfolio Monitoring
  val portfolioMonitoring = scenario("Portfolio Monitoring")
    .feed(orderFeeder)
    .exec(http("Get Portfolio Summary")
      .get("/api/v2/portfolios/${userId}")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .check(status.is(200))
      .check(jsonPath("$.portfolioId").exists)
      .check(responseTimeInMillis.lte(200))
    )
    .pause(5.seconds, 10.seconds)
    .exec(http("Get Positions")
      .get("/api/v2/portfolios/${userId}/positions")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .check(status.is(200))
      .check(responseTimeInMillis.lte(150))
    )
    .pause(5.seconds, 10.seconds)

  // Scenario 3: PnL Calculation Load
  val pnlCalculationLoad = scenario("PnL Calculation Load")
    .feed(orderFeeder)
    .exec(http("Calculate PnL")
      .get("/api/v2/portfolios/${userId}/pnl")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .queryParam("startDate", "2025-01-01")
      .queryParam("endDate", "2025-10-15")
      .check(status.is(200))
      .check(jsonPath("$.totalPnL").exists)
      .check(responseTimeInMillis.lte(300))
    )
    .pause(10.seconds, 20.seconds) // Less frequent heavy operation

  // Scenario 4: Risk Metrics Monitoring
  val riskMetricsMonitoring = scenario("Risk Metrics Monitoring")
    .feed(orderFeeder)
    .exec(http("Get Risk Metrics")
      .get("/api/v2/portfolios/${userId}/risk")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .check(status.is(200))
      .check(jsonPath("$.totalExposure").exists)
      .check(responseTimeInMillis.lte(250))
    )
    .pause(10.seconds, 20.seconds)

  // Scenario 5: Order Status Checks
  val orderStatusChecks = scenario("Order Status Checks")
    .feed(orderFeeder)
    .exec(http("List User Orders")
      .get("/api/v1/orders/user/${userId}")
      .header("Authorization", s"Bearer $authToken")
      .queryParam("limit", "20")
      .check(status.is(200))
      .check(responseTimeInMillis.lte(150))
    )
    .pause(15.seconds, 30.seconds)

  // Scenario 6: Health Check Monitoring
  val healthCheckMonitoring = scenario("Health Check Monitoring")
    .exec(http("Health Check")
      .get("/actuator/health")
      .check(status.is(200))
      .check(jsonPath("$.status").is("UP"))
      .check(responseTimeInMillis.lte(50))
    )
    .pause(30.seconds, 60.seconds) // Periodic health checks

  // Endurance Load Profile - Sustained for 4 hours
  val enduranceProfile = constantUsersPerSec(20).during(duration.minutes)

  setUp(
    // 40% order placement
    steadyOrderPlacement.inject(enduranceProfile.scale(0.4)).protocols(httpProtocol),

    // 25% portfolio monitoring
    portfolioMonitoring.inject(enduranceProfile.scale(0.25)).protocols(httpProtocol),

    // 15% PnL calculations
    pnlCalculationLoad.inject(enduranceProfile.scale(0.15)).protocols(httpProtocol),

    // 10% risk metrics
    riskMetricsMonitoring.inject(enduranceProfile.scale(0.1)).protocols(httpProtocol),

    // 8% order status checks
    orderStatusChecks.inject(enduranceProfile.scale(0.08)).protocols(httpProtocol),

    // 2% health checks
    healthCheckMonitoring.inject(enduranceProfile.scale(0.02)).protocols(httpProtocol)
  )
    .assertions(
      // Strict assertions for stability
      global.responseTime.percentile3.lt(200), // p95 must stay under 200ms
      global.responseTime.percentile4.lt(500), // p99 must stay under 500ms
      global.successfulRequests.percent.gt(99.9), // Error rate < 0.1% throughout
      // Performance should not degrade over time
      global.responseTime.max.lt(5000) // No single request should exceed 5s
    )
    .throttle(
      reachRps(1000).in(5.minutes), // Gradual ramp to steady state
      holdFor((duration - 10).minutes), // Maintain steady state
      reachRps(100).in(5.minutes) // Graceful shutdown
    )
    .maxDuration(duration.minutes)
    .protocols(httpProtocol)

  // Report Generation - Enhanced for endurance testing
  before {
    println(s"""
      |==============================================
      | TradeMaster Endurance Test Starting
      |==============================================
      | Duration: $duration minutes
      | Base URL: $baseUrl
      | Target Load: 5,000 concurrent users
      | RPS Target: ~1,000 req/sec sustained
      |
      | Monitoring:
      | - JVM heap usage trends
      | - Connection pool health
      | - Response time stability
      | - Error rate trends
      | - Disk space consumption
      |==============================================
      |""".stripMargin)
  }

  after {
    println("""
      |==============================================
      | TradeMaster Endurance Test Complete
      |==============================================
      | Review Grafana dashboards for:
      | - Memory leak indicators
      | - Response time trends
      | - Error rate patterns
      | - Resource exhaustion signs
      |==============================================
      |""".stripMargin)
  }
}
