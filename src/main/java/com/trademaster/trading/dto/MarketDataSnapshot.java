package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Market Data Snapshot DTO
 * 
 * Real-time market data snapshot with:
 * - Multi-venue order book data
 * - Price and volume information
 * - Market microstructure indicators
 * - Liquidity and volatility metrics
 * - Execution context information
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataSnapshot {
    
    /**
     * Basic Information
     */
    private String symbol;
    private String exchange;
    private String assetClass; // EQUITY, DERIVATIVE, COMMODITY, CURRENCY
    private String sector;
    private String industry;
    private Instant timestamp;
    private String currency;
    
    /**
     * Price Information
     */
    private PriceData priceData;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceData {
        private BigDecimal lastPrice;
        private BigDecimal bidPrice;
        private BigDecimal askPrice;
        private BigDecimal midPrice;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private BigDecimal closePrice; // Previous close
        private BigDecimal vwap; // Volume-weighted average price
        private BigDecimal change; // Price change from previous close
        private BigDecimal changePercent; // Percentage change
        private Integer tickDirection; // 1 = uptick, -1 = downtick, 0 = same
    }
    
    /**
     * Volume Information
     */
    private VolumeData volumeData;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolumeData {
        private Long volume; // Current day volume
        private Long volumeWeightedPrice; // Volume at current price
        private BigDecimal averageVolume; // Average daily volume
        private Long bidVolume; // Volume at bid
        private Long askVolume; // Volume at ask
        private BigDecimal turnover; // Turnover value
        private Integer tradesCount; // Number of trades
        private BigDecimal averageTradeSize; // Average trade size
        private Long volumeImbalance; // Buy volume - Sell volume
    }
    
    /**
     * Order Book Information
     */
    private OrderBookData orderBook;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderBookData {
        private List<OrderBookLevel> bids; // Buy orders
        private List<OrderBookLevel> asks; // Sell orders
        private BigDecimal spread; // Bid-ask spread
        private BigDecimal spreadPercent; // Spread as percentage
        private Integer depth; // Order book depth
        private BigDecimal imbalance; // Order book imbalance
        private Long totalBidVolume; // Total bid volume
        private Long totalAskVolume; // Total ask volume
        private Integer bidCount; // Number of bid levels
        private Integer askCount; // Number of ask levels
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderBookLevel {
        private BigDecimal price;
        private Long volume;
        private Integer orderCount;
        private String venue; // For multi-venue aggregation
        private Instant timestamp;
    }
    
    /**
     * Multi-Venue Data
     */
    private List<VenueData> venueData;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenueData {
        private String venueName;
        private String venueType; // LIT, DARK, ATS, CROSSING
        private BigDecimal bidPrice;
        private BigDecimal askPrice;
        private Long bidVolume;
        private Long askVolume;
        private BigDecimal lastPrice;
        private Long lastVolume;
        private Instant lastUpdate;
        private String venueStatus; // OPEN, CLOSED, HALTED
        private BigDecimal marketShare; // Venue market share percentage
        private Long latencyMicros; // Venue latency in microseconds
    }
    
    /**
     * Market Microstructure Indicators
     */
    private MicrostructureData microstructure;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MicrostructureData {
        private BigDecimal effectiveSpread; // Effective bid-ask spread
        private BigDecimal priceImpact; // Estimated price impact
        private BigDecimal realizednSpread; // Realized spread
        private Integer averageTradeSize; // Average trade size
        private BigDecimal tradingIntensity; // Trades per minute
        private BigDecimal volatility; // Intraday volatility
        private BigDecimal momentum; // Price momentum
        private String flowToxicity; // HIGH, MEDIUM, LOW
        private BigDecimal adverseSelection; // Adverse selection measure
    }
    
    /**
     * Volatility Metrics
     */
    private VolatilityData volatility;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolatilityData {
        private BigDecimal realizedVolatility; // Historical realized volatility
        private BigDecimal impliedVolatility; // Option implied volatility
        private BigDecimal intradayVolatility; // Current day volatility
        private BigDecimal volatilityRank; // Percentile rank
        private String volatilityRegime; // LOW, NORMAL, HIGH, EXTREME
        private BigDecimal garchVolatility; // GARCH model estimate
        private BigDecimal ewmaVolatility; // EWMA estimate
        private List<BigDecimal> volatilityHistory; // Recent volatility history
    }
    
    /**
     * Liquidity Metrics
     */
    private LiquidityData liquidity;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiquidityData {
        private BigDecimal liquidityScore; // Overall liquidity score 0-100
        private BigDecimal amihudIlliquidity; // Amihud illiquidity measure
        private Integer quotedSpread; // Quoted bid-ask spread
        private BigDecimal effectiveSpread; // Effective spread
        private BigDecimal marketDepth; // Market depth at best prices
        private BigDecimal resiliency; // Market resiliency measure
        private String liquidityProvision; // HIGH, MEDIUM, LOW
        private Integer timeToExecution; // Expected execution time
        private BigDecimal liquidationCost; // Estimated liquidation cost
    }
    
    /**
     * Trading Information
     */
    private TradingInfo tradingInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradingInfo {
        private String marketStatus; // PRE_OPEN, OPEN, CLOSED, HALTED
        private String sessionType; // PRE_MARKET, REGULAR, POST_MARKET
        private Instant marketOpen;
        private Instant marketClose;
        private Boolean tradingHalted;
        private String haltReason; // NEWS, VOLATILITY, REGULATORY
        private List<String> tradingRestrictions; // Short sale restrictions etc.
        private String circuitBreakerStatus; // NORMAL, LEVEL1, LEVEL2, LEVEL3
        private BigDecimal priceLimit; // Price limit if applicable
    }
    
    /**
     * Corporate Actions and Events
     */
    private EventData events;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventData {
        private List<String> corporateActions; // Pending corporate actions
        private List<String> newsEvents; // Recent news events
        private String earningsDate; // Next earnings date
        private String dividendExDate; // Dividend ex-date
        private List<String> economicEvents; // Relevant economic events
        private String analystRecommendation; // BUY, HOLD, SELL
        private Integer analystCount; // Number of analysts covering
        private BigDecimal consensusTarget; // Consensus price target
    }
    
    /**
     * Technical Indicators
     */
    private TechnicalIndicators technical;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicalIndicators {
        private BigDecimal rsi; // Relative Strength Index
        private BigDecimal macd; // MACD indicator
        private BigDecimal bollingerUpper; // Bollinger upper band
        private BigDecimal bollingerLower; // Bollinger lower band
        private BigDecimal sma20; // 20-period simple moving average
        private BigDecimal ema50; // 50-period exponential moving average
        private BigDecimal supportLevel; // Key support level
        private BigDecimal resistanceLevel; // Key resistance level
        private String trend; // BULLISH, BEARISH, SIDEWAYS
        private BigDecimal atr; // Average True Range
    }
    
    /**
     * Data Quality Information
     */
    private DataQuality dataQuality;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataQuality {
        private String overallQuality; // EXCELLENT, GOOD, FAIR, POOR
        private Long latencyMicros; // Data latency in microseconds
        private BigDecimal completeness; // Data completeness percentage
        private Integer stalePriceCount; // Number of stale prices
        private List<String> dataIssues; // Known data quality issues
        private String primaryDataSource; // Primary data source
        private List<String> backupSources; // Backup data sources
        private Instant lastRefresh; // Last data refresh time
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if market is open
     */
    public boolean isMarketOpen() {
        return tradingInfo != null && "OPEN".equals(tradingInfo.getMarketStatus());
    }
    
    /**
     * Check if data is stale - eliminates if-statement with Optional
     */
    public boolean isDataStale(long maxAgeMillis) {
        return Optional.ofNullable(timestamp)
            .map(ts -> System.currentTimeMillis() - ts.toEpochMilli() > maxAgeMillis)
            .orElse(true);
    }
    
    /**
     * Get current spread in basis points - eliminates if-statement with Optional chains
     */
    public BigDecimal getSpreadBps() {
        return Optional.ofNullable(orderBook)
            .flatMap(ob -> Optional.ofNullable(ob.getSpread())
                .flatMap(spread -> Optional.ofNullable(priceData)
                    .flatMap(pd -> Optional.ofNullable(pd.getMidPrice())
                        .filter(mid -> mid.compareTo(BigDecimal.ZERO) != 0)
                        .map(mid -> spread.divide(mid, 6, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(10000)))))) // Convert to basis points
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Check if order book is imbalanced - eliminates if-statement with Optional
     */
    public boolean isOrderBookImbalanced() {
        return Optional.ofNullable(orderBook)
            .flatMap(ob -> Optional.ofNullable(ob.getImbalance())
                .map(imb -> imb.abs().compareTo(new BigDecimal("0.6")) > 0)) // >60% imbalance
            .orElse(false);
    }
    
    /**
     * Get best venue for buying - eliminates if-statement with Optional
     */
    public String getBestBuyVenue() {
        return Optional.ofNullable(venueData)
            .filter(venues -> !venues.isEmpty())
            .flatMap(venues -> venues.stream()
                .filter(venue -> venue.getAskPrice() != null)
                .min((v1, v2) -> v1.getAskPrice().compareTo(v2.getAskPrice()))
                .map(VenueData::getVenueName))
            .orElse(null);
    }
    
    /**
     * Get best venue for selling - eliminates if-statement with Optional
     */
    public String getBestSellVenue() {
        return Optional.ofNullable(venueData)
            .filter(venues -> !venues.isEmpty())
            .flatMap(venues -> venues.stream()
                .filter(venue -> venue.getBidPrice() != null)
                .max((v1, v2) -> v1.getBidPrice().compareTo(v2.getBidPrice()))
                .map(VenueData::getVenueName))
            .orElse(null);
    }
    
    /**
     * Calculate market cap (if applicable) - eliminates if-statement with Optional
     */
    public BigDecimal getMarketCap(Long sharesOutstanding) {
        return Optional.ofNullable(priceData)
            .flatMap(pd -> Optional.ofNullable(pd.getLastPrice())
                .flatMap(lastPrice -> Optional.ofNullable(sharesOutstanding)
                    .map(shares -> lastPrice.multiply(BigDecimal.valueOf(shares)))))
            .orElse(null);
    }
    
    /**
     * Check if trading is halted
     */
    public boolean isTradingHalted() {
        return tradingInfo != null && 
               (Boolean.TRUE.equals(tradingInfo.getTradingHalted()) || 
                "HALTED".equals(tradingInfo.getMarketStatus()));
    }
    
    /**
     * Get liquidity assessment - eliminates if-else chain with Stream pattern
     */
    public String getLiquidityAssessment() {
        return Optional.ofNullable(liquidity)
            .flatMap(liq -> Optional.ofNullable(liq.getLiquidityScore())
                .map(score -> {
                    record LiquidityThreshold(BigDecimal minScore, String assessment) {}

                    return Stream.of(
                        new LiquidityThreshold(new BigDecimal("80"), "HIGH"),
                        new LiquidityThreshold(new BigDecimal("50"), "MEDIUM")
                    )
                    .filter(threshold -> score.compareTo(threshold.minScore()) >= 0)
                    .findFirst()
                    .map(LiquidityThreshold::assessment)
                    .orElse("LOW");
                }))
            .orElse("UNKNOWN");
    }
    
    /**
     * Get market data summary - eliminates all 7 ternary operators with Optional patterns
     */
    public Map<String, Object> getMarketSummary() {
        return Map.of(
            "symbol", Optional.ofNullable(symbol).orElse("N/A"),
            "lastPrice", Optional.ofNullable(priceData)
                .flatMap(pd -> Optional.ofNullable(pd.getLastPrice()))
                .orElse(BigDecimal.ZERO),
            "change", Optional.ofNullable(priceData)
                .flatMap(pd -> Optional.ofNullable(pd.getChange()))
                .orElse(BigDecimal.ZERO),
            "volume", Optional.ofNullable(volumeData)
                .flatMap(vd -> Optional.ofNullable(vd.getVolume()))
                .orElse(0L),
            "spread", Optional.ofNullable(orderBook)
                .flatMap(ob -> Optional.ofNullable(ob.getSpread()))
                .orElse(BigDecimal.ZERO),
            "marketStatus", Optional.ofNullable(tradingInfo)
                .flatMap(ti -> Optional.ofNullable(ti.getMarketStatus()))
                .orElse("UNKNOWN"),
            "liquidityAssessment", getLiquidityAssessment(),
            "isDataStale", isDataStale(5000), // 5 second staleness threshold
            "isTradingHalted", isTradingHalted(),
            "timestamp", Optional.ofNullable(timestamp).orElse(Instant.EPOCH)
        );
    }
    
    /**
     * Static factory method for empty snapshot
     */
    public static MarketDataSnapshot empty(String symbol) {
        return MarketDataSnapshot.builder()
            .symbol(symbol)
            .timestamp(Instant.now())
            .priceData(PriceData.builder()
                .lastPrice(BigDecimal.ZERO)
                .change(BigDecimal.ZERO)
                .build())
            .volumeData(VolumeData.builder()
                .volume(0L)
                .tradesCount(0)
                .build())
            .tradingInfo(TradingInfo.builder()
                .marketStatus("UNKNOWN")
                .tradingHalted(false)
                .build())
            .dataQuality(DataQuality.builder()
                .overallQuality("POOR")
                .completeness(BigDecimal.ZERO)
                .build())
            .build();
    }
}