package com.trademaster.trading.dto.marketdata;

import java.time.Instant;
import java.util.Map;

/**
 * Sentiment Analysis Response DTO
 *
 * Immutable data transfer object for sentiment analysis from market-data-service.
 * Contains aggregated sentiment data from news sources and social media.
 *
 * @param symbol Trading symbol
 * @param sentiment Overall sentiment (POSITIVE, NEGATIVE, NEUTRAL)
 * @param score Sentiment score (-1.0 to 1.0)
 * @param details Detailed sentiment breakdown by source
 * @param timestamp Analysis timestamp
 *
 * @author TradeMaster Development Team
 * @version 2.0.0 (Java 24 + Records)
 */
public record SentimentResponse(
    String symbol,
    String sentiment,
    Double score,
    Map<String, Object> details,
    Instant timestamp
) {}
