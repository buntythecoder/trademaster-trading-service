package com.trademaster.trading.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Random

/**
 * Portfolio Retrieval Load Test Simulation
 *
 * Validates system performance for portfolio data retrieval under various load conditions.
 * Tests portfolio aggregation, PnL calculations, risk metrics, and position queries.
 *
 * Test Scenarios:
 * - Get Portfolio Summary
 * - Get Portfolio Positions
 * - Calculate Portfolio PnL
 * - Get Risk Metrics
 *
 * Performance Targets:
 * - API Response Time p95: <200ms
 * - API Response Time p99: <500ms
 * - Query Latency: <100ms
 * - Error Rate: <0.1%
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
class PortfolioRetrievalLoadTest extends Simulation {

  // Base URL for portfolio service (accessed through Kong gateway)
  private val baseUrl = System.getProperty("baseUrl", "http://localhost:8000")

  // Load testing parameters
  private val loadProfile = System.getProperty("loadProfile", "normal")
  private val duration = System.getProperty("duration", "30").toInt

  // HTTP protocol configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Load Test / TradeMaster")
    .shareConnections

  // Test data - realistic user IDs
  private val userIds = (1 to 10000).toSeq
  private val portfolioIds = (1 to 5000).toSeq

  // JWT token for authentication
  private val authToken = System.getProperty("authToken", "test-jwt-token")

  // User feeder
  val userFeeder = Iterator.continually(Map(
    "userId" -> userIds(Random.nextInt(userIds.length)),
    "portfolioId" -> portfolioIds(Random.nextInt(portfolioIds.length))
  ))

  // Scenario 1: Get Portfolio Summary
  val getPortfolioSummary = scenario("Get Portfolio Summary")
    .feed(userFeeder)
    .exec(http("Get Portfolio Summary")
      .get("/api/v2/portfolios/${portfolioId}")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .check(status.is(200))
      .check(jsonPath("$.portfolioId").exists)
      .check(jsonPath("$.totalValue").exists)
      .check(jsonPath("$.totalPnL").exists)
      .check(responseTimeInMillis.lte(200)) // p95 target
    )
    .pause(2.seconds, 5.seconds)

  // Scenario 2: Get Portfolio Positions
  val getPortfolioPositions = scenario("Get Portfolio Positions")
    .feed(userFeeder)
    .exec(http("Get Portfolio Positions")
      .get("/api/v2/portfolios/${portfolioId}/positions")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .check(status.is(200))
      .check(jsonPath("$[*].symbol").exists)
      .check(jsonPath("$[*].quantity").exists)
      .check(responseTimeInMillis.lte(150)) // Slightly faster target for read operations
    )
    .pause(2.seconds, 5.seconds)

  // Scenario 3: Calculate Portfolio PnL
  val calculatePortfolioPnL = scenario("Calculate Portfolio PnL")
    .feed(userFeeder)
    .exec(http("Calculate Portfolio PnL")
      .get("/api/v2/portfolios/${portfolioId}/pnl")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .queryParam("startDate", "2025-01-01")
      .queryParam("endDate", "2025-10-15")
      .check(status.is(200))
      .check(jsonPath("$.totalPnL").exists)
      .check(jsonPath("$.realizedPnL").exists)
      .check(jsonPath("$.unrealizedPnL").exists)
      .check(responseTimeInMillis.lte(300)) // PnL calculations may be slower
    )
    .pause(3.seconds, 7.seconds)

  // Scenario 4: Get Risk Metrics
  val getRiskMetrics = scenario("Get Risk Metrics")
    .feed(userFeeder)
    .exec(http("Get Risk Metrics")
      .get("/api/v2/portfolios/${portfolioId}/risk")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .check(status.is(200))
      .check(jsonPath("$.totalExposure").exists)
      .check(jsonPath("$.valueAtRisk").exists)
      .check(responseTimeInMillis.lte(250)) // Risk calculations
    )
    .pause(3.seconds, 7.seconds)

  // Scenario 5: Get Portfolio Performance
  val getPortfolioPerformance = scenario("Get Portfolio Performance")
    .feed(userFeeder)
    .exec(http("Get Portfolio Performance")
      .get("/api/v2/portfolios/${portfolioId}/performance")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .queryParam("period", "1M")
      .check(status.is(200))
      .check(jsonPath("$.returnPercentage").exists)
      .check(responseTimeInMillis.lte(200))
    )
    .pause(3.seconds, 7.seconds)

  // Scenario 6: Get Position Details
  val getPositionDetails = scenario("Get Position Details")
    .feed(userFeeder)
    .exec(http("Get Position Details")
      .get("/api/v2/portfolios/${portfolioId}/positions/RELIANCE")
      .header("Authorization", s"Bearer $authToken")
      .header("X-API-Key", "test-api-key")
      .check(status.in(200, 404)) // 404 if position doesn't exist
      .check(responseTimeInMillis.lte(100))
    )
    .pause(2.seconds, 5.seconds)

  // Load profiles
  private val normalLoad = loadProfile match {
    case "normal" => rampUsers(5000).during(5.minutes)
    case "peak" => rampUsers(10000).during(10.minutes)
    case "stress" => rampUsers(15000).during(10.minutes)
    case _ => rampUsers(5000).during(5.minutes)
  }

  // Setup scenarios with realistic distribution
  setUp(
    getPortfolioSummary.inject(normalLoad.scale(0.3)).protocols(httpProtocol), // 30% - most common operation
    getPortfolioPositions.inject(normalLoad.scale(0.25)).protocols(httpProtocol), // 25%
    calculatePortfolioPnL.inject(normalLoad.scale(0.2)).protocols(httpProtocol), // 20%
    getRiskMetrics.inject(normalLoad.scale(0.15)).protocols(httpProtocol), // 15%
    getPortfolioPerformance.inject(normalLoad.scale(0.07)).protocols(httpProtocol), // 7%
    getPositionDetails.inject(normalLoad.scale(0.03)).protocols(httpProtocol) // 3%
  )
    .assertions(
      global.responseTime.percentile3.lt(200), // p95 < 200ms
      global.responseTime.percentile4.lt(500), // p99 < 500ms
      global.successfulRequests.percent.gt(99.9) // Error rate < 0.1%
    )
    .maxDuration(duration.minutes)
}
