package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
     * Check if data is stale
     */
    public boolean isDataStale(long maxAgeMillis) {
        if (timestamp == null) return true;
        return System.currentTimeMillis() - timestamp.toEpochMilli() > maxAgeMillis;
    }
    
    /**
     * Get current spread in basis points
     */
    public BigDecimal getSpreadBps() {
        if (orderBook == null || orderBook.getSpread() == null || 
            priceData == null || priceData.getMidPrice() == null ||
            priceData.getMidPrice().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return orderBook.getSpread()
               .divide(priceData.getMidPrice(), 6, java.math.RoundingMode.HALF_UP)
               .multiply(BigDecimal.valueOf(10000)); // Convert to basis points
    }
    
    /**
     * Check if order book is imbalanced
     */
    public boolean isOrderBookImbalanced() {
        if (orderBook == null || orderBook.getImbalance() == null) {
            return false;
        }
        return orderBook.getImbalance().abs().compareTo(new BigDecimal("0.6")) > 0; // >60% imbalance
    }
    
    /**
     * Get best venue for buying
     */
    public String getBestBuyVenue() {
        if (venueData == null || venueData.isEmpty()) {
            return null;
        }
        
        return venueData.stream()
               .filter(venue -> venue.getAskPrice() != null)
               .min((v1, v2) -> v1.getAskPrice().compareTo(v2.getAskPrice()))
               .map(VenueData::getVenueName)
               .orElse(null);
    }
    
    /**
     * Get best venue for selling
     */
    public String getBestSellVenue() {
        if (venueData == null || venueData.isEmpty()) {
            return null;
        }
        
        return venueData.stream()
               .filter(venue -> venue.getBidPrice() != null)
               .max((v1, v2) -> v1.getBidPrice().compareTo(v2.getBidPrice()))
               .map(VenueData::getVenueName)
               .orElse(null);
    }
    
    /**
     * Calculate market cap (if applicable)
     */
    public BigDecimal getMarketCap(Long sharesOutstanding) {
        if (priceData == null || priceData.getLastPrice() == null || sharesOutstanding == null) {
            return null;
        }
        return priceData.getLastPrice().multiply(BigDecimal.valueOf(sharesOutstanding));
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
     * Get liquidity assessment
     */
    public String getLiquidityAssessment() {
        if (liquidity == null || liquidity.getLiquidityScore() == null) {
            return "UNKNOWN";
        }
        
        BigDecimal score = liquidity.getLiquidityScore();
        if (score.compareTo(new BigDecimal("80")) >= 0) {
            return "HIGH";
        } else if (score.compareTo(new BigDecimal("50")) >= 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Get market data summary
     */
    public Map<String, Object> getMarketSummary() {
        return Map.of(
            "symbol", symbol != null ? symbol : "N/A",
            "lastPrice", priceData != null && priceData.getLastPrice() != null ? 
                        priceData.getLastPrice() : BigDecimal.ZERO,
            "change", priceData != null && priceData.getChange() != null ? 
                     priceData.getChange() : BigDecimal.ZERO,
            "volume", volumeData != null && volumeData.getVolume() != null ? 
                     volumeData.getVolume() : 0L,
            "spread", orderBook != null && orderBook.getSpread() != null ? 
                     orderBook.getSpread() : BigDecimal.ZERO,
            "marketStatus", tradingInfo != null && tradingInfo.getMarketStatus() != null ? 
                          tradingInfo.getMarketStatus() : "UNKNOWN",
            "liquidityAssessment", getLiquidityAssessment(),
            "isDataStale", isDataStale(5000), // 5 second staleness threshold
            "isTradingHalted", isTradingHalted(),
            "timestamp", timestamp != null ? timestamp : Instant.EPOCH
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