package com.trademaster.trading.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

/**
 * Comprehensive Stress Test Simulation
 *
 * Tests system behavior under extreme load conditions to identify breaking points,
 * failure modes, and system degradation patterns.
 *
 * Test Strategy:
 * - Start with normal load (5,000 users)
 * - Ramp up to peak load (10,000 users)
 * - Push to stress level (15,000 users)
 * - Test failure recovery and circuit breaker behavior
 *
 * Objectives:
 * - Identify system capacity limits
 * - Validate circuit breaker activation
 * - Test fallback mechanisms
 * - Measure recovery time after stress
 * - Validate Virtual Threads performance under extreme load
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
class ComprehensiveStressTest extends Simulation {

  private val baseUrl = System.getProperty("baseUrl", "http://localhost:8000")
  private val duration = System.getProperty("duration", "45").toInt // Extended for stress testing

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Stress Test / TradeMaster")
    .shareConnections
    .disableWarmUp // Start immediately without warmup

  private val symbols = Seq("RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK")
  private val exchanges = Seq("NSE", "BSE")
  private val authToken = System.getProperty("authToken", "test-jwt-token")

  // High-frequency order feeder
  val orderFeeder = Iterator.continually(Map(
    "userId" -> Random.nextInt(20000), // Larger user pool for stress
    "symbol" -> symbols(Random.nextInt(symbols.length)),
    "exchange" -> exchanges(Random.nextInt(exchanges.length)),
    "quantity" -> (Random.nextInt(500) + 1)
  ))

  // Aggressive Order Placement - Minimal think time
  val aggressiveOrderPlacement = scenario("Aggressive Order Placement")
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
      .check(status.in(200, 201, 429, 503)) // Accept throttling and service unavailable
      .check(responseTimeInMillis.lte(1000)) // Relaxed during stress
    )
    .pause(100.milliseconds, 500.milliseconds) // Minimal pause

  // Portfolio Read Storm - High volume reads
  val portfolioReadStorm = scenario("Portfolio Read Storm")
    .feed(orderFeeder)
    .exec(http("Get Portfolio")
      .get("/api/v2/portfolios/${userId}")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .check(status.in(200, 404, 429, 503))
      .check(responseTimeInMillis.lte(1000))
    )
    .pause(100.milliseconds, 300.milliseconds)

  // Mixed Load Scenario - Order + Portfolio operations
  val mixedLoadScenario = scenario("Mixed Load Scenario")
    .feed(orderFeeder)
    .exec(
      http("Place Order")
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
        .check(status.in(200, 201, 429, 503))
    )
    .pause(200.milliseconds)
    .exec(
      http("Get Portfolio")
        .get("/api/v2/portfolios/${userId}")
        .header("Authorization", s"Bearer $authToken")
        .header("X-API-Key", "test-api-key")
        .check(status.in(200, 404, 429, 503))
    )
    .pause(200.milliseconds)

  // Database Stress - PnL calculations and aggregations
  val databaseStressScenario = scenario("Database Stress")
    .feed(orderFeeder)
    .exec(
      http("Calculate PnL")
        .get("/api/v2/portfolios/${userId}/pnl")
        .header("Authorization", s"Bearer $authToken")
        .header("X-API-Key", "test-api-key")
        .queryParam("startDate", "2025-01-01")
        .queryParam("endDate", "2025-10-15")
        .check(status.in(200, 429, 503))
        .check(responseTimeInMillis.lte(2000)) // Very relaxed for complex queries
    )
    .pause(500.milliseconds, 1.seconds)

  // Stress Test Load Profile - Progressive ramp up
  val stressProfile = Seq(
    // Phase 1: Normal load - 5 minutes warmup
    rampUsers(5000).during(5.minutes),
    // Phase 2: Peak load - 10 minutes at capacity
    rampUsers(10000).during(10.minutes),
    // Phase 3: Stress load - 15 minutes beyond capacity
    rampUsers(15000).during(15.minutes),
    // Phase 4: Cooldown - reduce to normal
    rampUsers(5000).during(5.minutes)
  )

  setUp(
    // 40% aggressive order placement
    aggressiveOrderPlacement.inject(
      rampUsers(6000).during(5.minutes),
      constantUsersPerSec(4000).during(20.minutes),
      rampUsers(2000).during(5.minutes)
    ).protocols(httpProtocol),

    // 30% portfolio reads
    portfolioReadStorm.inject(
      rampUsers(4500).during(5.minutes),
      constantUsersPerSec(3000).during(20.minutes),
      rampUsers(1500).during(5.minutes)
    ).protocols(httpProtocol),

    // 20% mixed operations
    mixedLoadScenario.inject(
      rampUsers(3000).during(5.minutes),
      constantUsersPerSec(2000).during(20.minutes),
      rampUsers(1000).during(5.minutes)
    ).protocols(httpProtocol),

    // 10% database stress
    databaseStressScenario.inject(
      rampUsers(1500).during(5.minutes),
      constantUsersPerSec(1000).during(20.minutes),
      rampUsers(500).during(5.minutes)
    ).protocols(httpProtocol)
  )
    .assertions(
      // Relaxed assertions for stress testing
      global.responseTime.percentile4.lt(2000), // p99 < 2s under stress
      global.successfulRequests.percent.gt(95) // Accept 5% failure under extreme stress
    )
    .throttle(
      reachRps(5000).in(5.minutes), // Gradual ramp to 5,000 req/sec
      holdFor(15.minutes),
      reachRps(1000).in(5.minutes) // Cooldown
    )
    .maxDuration(duration.minutes)
}
