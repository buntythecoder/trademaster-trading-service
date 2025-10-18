package com.trademaster.trading.validation.impl;

import com.trademaster.common.functional.Validation;
import com.trademaster.trading.dto.OrderRequest;
import com.trademaster.trading.entity.Order;
import com.trademaster.trading.model.OrderSide;
import com.trademaster.trading.model.OrderType;
import com.trademaster.trading.model.TimeInForce;
import com.trademaster.trading.validation.OrderValidator;
import com.trademaster.trading.validation.ValidationError;
import com.trademaster.trading.validation.ValidationResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Functional Order Validator
 *
 * MANDATORY: Rule #3 - Functional Programming First (no if-else, functional patterns)
 * MANDATORY: Rule #9 - Immutability & Records (Validation monad, Records for errors)
 * MANDATORY: Rule #11 - Error Handling Patterns (Validation monad for error accumulation)
 * MANDATORY: Rule #13 - Stream API Mastery (functional validation chains)
 * MANDATORY: Rule #14 - Pattern Matching Excellence (switch expressions for error handling)
 * MANDATORY: Rule #15 - Structured Logging & Monitoring (Prometheus metrics)
 *
 * Comprehensive order validation using functional patterns and the Validation monad
 * from the common library for error accumulation.
 *
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FunctionalOrderValidator implements OrderValidator {

    private final MeterRegistry meterRegistry;

    // Metrics
    private static final String VALIDATION_METRIC = "trading.validation";
    private static final String VALIDATION_ERROR_METRIC = "trading.validation.errors";

    // Configuration constants (externalized in production)
    private static final int MAX_QUANTITY = 1_000_000;
    private static final int MIN_QUANTITY = 1;
    private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(0.01);
    private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(100_000_000);
    private static final int MAX_SYMBOL_LENGTH = 20;

    @Override
    public ValidationResult validate(OrderRequest orderRequest, Long userId) {
        long startTime = System.nanoTime();

        // Use Validation monad for error accumulation (Rule #11)
        Validation<OrderRequest, ValidationError> result = validateOrderFunctionally(orderRequest, userId);

        // Convert Validation to ValidationResult for interface compatibility
        ValidationResult validationResult = adaptToValidationResult(result);

        // Record metrics (Rule #15)
        recordValidationMetrics(validationResult, System.nanoTime() - startTime);

        log.debug("Functional validation completed for user {} - valid: {}, errors: {}",
                userId, validationResult.isValid(), validationResult.getErrorMessages().size());

        return validationResult;
    }

    @Override
    public ValidationResult validateModification(Order existingOrder, OrderRequest modificationRequest, Long userId) {
        long startTime = System.nanoTime();

        // Functional validation for modification (Rule #3)
        Validation<OrderRequest, ValidationError> result = validateModificationFunctionally(
                existingOrder, modificationRequest, userId
        );

        ValidationResult validationResult = adaptToValidationResult(result);
        recordValidationMetrics(validationResult, System.nanoTime() - startTime);

        return validationResult;
    }

    /**
     * Comprehensive functional validation using Validation monad
     * MANDATORY: Rule #3 - No if-else, functional composition only
     */
    private Validation<OrderRequest, ValidationError> validateOrderFunctionally(
            OrderRequest request, Long userId) {

        // Compose all validations using Validation.validateWith (Rule #3, #11)
        return Validation.validateWith(request, List.of(
                this::validateSymbol,
                this::validateQuantity,
                this::validatePriceRequirements,
                this::validateOrderType,
                this::validateTimeInForce,
                this::validateUserPermissions
        ));
    }

    /**
     * Validate symbol existence, tradeability, and suspension status
     * MANDATORY: Rule #14 - Pattern matching over if-else
     */
    private Validation<OrderRequest, ValidationError> validateSymbol(OrderRequest request) {
        return Optional.ofNullable(request.symbol())
                .filter(Predicate.not(String::isBlank))
                .filter(s -> s.length() <= MAX_SYMBOL_LENGTH)
                .filter(s -> s.matches("^[A-Z0-9_]+$"))
                .map(symbol -> checkSymbolStatus(symbol, request))
                .orElseGet(() -> Validation.invalid(
                        ValidationError.invalidSymbolFormat(request.symbol())
                ));
    }

    /**
     * Check symbol status (tradeable, not suspended)
     * MANDATORY: Rule #14 - Pattern matching with switch expression
     */
    private Validation<OrderRequest, ValidationError> checkSymbolStatus(String symbol, OrderRequest request) {
        // In production, this would query a symbol service
        // For now, use pattern matching to validate symbol format and known restrictions

        return switch (symbol) {
            case String s when s.startsWith("TEST_") ->
                Validation.invalid(ValidationError.symbolSuspended(symbol));
            case String s when s.length() > MAX_SYMBOL_LENGTH ->
                Validation.invalid(ValidationError.invalidSymbolFormat(symbol));
            default -> Validation.valid(request);
        };
    }

    /**
     * Validate quantity with lot size, min/max constraints
     * MANDATORY: Rule #3 - Functional composition, no if-else
     */
    private Validation<OrderRequest, ValidationError> validateQuantity(OrderRequest request) {
        return Optional.ofNullable(request.quantity())
                .filter(q -> q >= MIN_QUANTITY)
                .map(q -> validateQuantityMaximum(q, request))
                .orElseGet(() -> Validation.invalid(
                        ValidationError.quantityBelowMinimum(request.quantity(), MIN_QUANTITY)
                ))
                .andThen(r -> validateLotSize(r.quantity(), request));
    }

    /**
     * Validate quantity maximum
     * MANDATORY: Rule #14 - Pattern matching with switch
     */
    private Validation<OrderRequest, ValidationError> validateQuantityMaximum(Integer quantity, OrderRequest request) {
        return switch (Integer.compare(quantity, MAX_QUANTITY)) {
            case 1 -> Validation.invalid(ValidationError.quantityAboveMaximum(quantity, MAX_QUANTITY));
            default -> Validation.valid(request);
        };
    }

    /**
     * Validate lot size (1 for equities, configurable for others)
     * MANDATORY: Rule #14 - Pattern matching for exchange-specific lot sizes
     */
    private Validation<OrderRequest, ValidationError> validateLotSize(Integer quantity, OrderRequest request) {
        int lotSize = switch (request.exchange()) {
            case "NSE", "BSE" -> 1;  // Indian exchanges: lot size 1 for equities
            case "NYSE", "NASDAQ" -> 1;  // US exchanges: lot size 1
            case "LSE" -> 1;  // London: lot size 1
            default -> 1;  // Default lot size
        };

        return switch (quantity % lotSize) {
            case 0 -> Validation.valid(request);
            default -> Validation.invalid(ValidationError.invalidLotSize(quantity, lotSize));
        };
    }

    /**
     * Validate price requirements based on order type
     * MANDATORY: Rule #3 - Functional composition, no if-else
     */
    private Validation<OrderRequest, ValidationError> validatePriceRequirements(OrderRequest request) {
        return validateOrderTypeSpecificPrices(request)
                .andThen(this::validatePriceRanges)
                .andThen(this::validateTickSize)
                .andThen(this::validateStopLimitPriceRelationship);
    }

    /**
     * Validate order type specific price requirements
     * MANDATORY: Rule #14 - Pattern matching with switch expression
     */
    private Validation<OrderRequest, ValidationError> validateOrderTypeSpecificPrices(OrderRequest request) {
        return switch (request.orderType()) {
            case LIMIT -> Optional.ofNullable(request.limitPrice())
                    .map(p -> Validation.<OrderRequest, ValidationError>valid(request))
                    .orElseGet(() -> Validation.invalid(ValidationError.priceRequired("limitPrice")));

            case STOP_LOSS -> Optional.ofNullable(request.stopPrice())
                    .map(p -> Validation.<OrderRequest, ValidationError>valid(request))
                    .orElseGet(() -> Validation.invalid(ValidationError.priceRequired("stopPrice")));

            case STOP_LIMIT -> {
                // STOP_LIMIT requires both limitPrice and stopPrice
                Validation<OrderRequest, ValidationError> limitCheck = Optional.ofNullable(request.limitPrice())
                        .map(p -> Validation.<OrderRequest, ValidationError>valid(request))
                        .orElseGet(() -> Validation.invalid(ValidationError.priceRequired("limitPrice")));

                Validation<OrderRequest, ValidationError> stopCheck = Optional.ofNullable(request.stopPrice())
                        .map(p -> Validation.<OrderRequest, ValidationError>valid(request))
                        .orElseGet(() -> Validation.invalid(ValidationError.priceRequired("stopPrice")));

                // Combine both validations to accumulate errors
                yield switch (limitCheck) {
                    case Validation.Valid<OrderRequest, ValidationError> v -> stopCheck;
                    case Validation.Invalid<OrderRequest, ValidationError> i1 -> switch (stopCheck) {
                        case Validation.Valid<OrderRequest, ValidationError> v -> limitCheck;
                        case Validation.Invalid<OrderRequest, ValidationError> i2 ->
                                Validation.invalid(Stream.concat(i1.errors().stream(), i2.errors().stream()).toList());
                    };
                };
            }

            case MARKET -> Validation.valid(request);
        };
    }

    /**
     * Validate price ranges
     * MANDATORY: Rule #13 - Stream API for functional processing
     */
    private Validation<OrderRequest, ValidationError> validatePriceRanges(OrderRequest request) {
        return Stream.of(
                Optional.ofNullable(request.limitPrice()).map(p -> validatePrice("limitPrice", p)),
                Optional.ofNullable(request.stopPrice()).map(p -> validatePrice("stopPrice", p))
        )
                .flatMap(Optional::stream)
                .reduce(
                        Validation.valid(request),
                        // Eliminates ternary using Optional.of().filter() for validation accumulation
                        (acc, validation) -> Optional.of(acc)
                            .filter(Validation::isValid)
                            .map(a -> validation)
                            .orElse(acc),
                        // Eliminates ternary using Optional.of().filter() for parallel combiner
                        (v1, v2) -> Optional.of(v1)
                            .filter(Validation::isValid)
                            .map(v -> v2)
                            .orElse(v1)
                );
    }

    /**
     * Validate individual price
     * MANDATORY: Rule #14 - Pattern matching for price validation
     */
    private Validation<OrderRequest, ValidationError> validatePrice(String field, BigDecimal price) {
        return switch (price.compareTo(MIN_PRICE)) {
            case -1 -> Validation.invalid(
                    new ValidationError.PriceError(field, "Price must be at least " + MIN_PRICE)
            );
            case 0, 1 -> switch (price.compareTo(MAX_PRICE)) {
                case 1 -> Validation.invalid(
                        new ValidationError.PriceError(field, "Price cannot exceed " + MAX_PRICE)
                );
                default -> Validation.valid(null);
            };
            default -> Validation.valid(null);
        };
    }

    /**
     * Validate tick size compliance
     * MANDATORY: Rule #3 - Functional pattern, no if-else
     */
    private Validation<OrderRequest, ValidationError> validateTickSize(OrderRequest request) {
        BigDecimal tickSize = getTickSize(request.exchange(), request.symbol());

        return Stream.of(
                Optional.ofNullable(request.limitPrice())
                        .map(p -> validatePriceTickSize("limitPrice", p, tickSize)),
                Optional.ofNullable(request.stopPrice())
                        .map(p -> validatePriceTickSize("stopPrice", p, tickSize))
        )
                .flatMap(Optional::stream)
                .reduce(
                        Validation.valid(request),
                        // Eliminates ternary using Optional.of().filter() for validation accumulation
                        (acc, validation) -> Optional.of(acc)
                            .filter(Validation::isValid)
                            .map(a -> validation)
                            .orElse(acc),
                        // Eliminates ternary using Optional.of().filter() for parallel combiner
                        (v1, v2) -> Optional.of(v1)
                            .filter(Validation::isValid)
                            .map(v -> v2)
                            .orElse(v1)
                );
    }

    /**
     * Get tick size for exchange/symbol
     * MANDATORY: Rule #14 - Pattern matching for exchange-specific tick sizes
     */
    private BigDecimal getTickSize(String exchange, String symbol) {
        return switch (exchange) {
            case "NSE", "BSE" -> BigDecimal.valueOf(0.05);  // Indian exchanges
            case "NYSE", "NASDAQ" -> BigDecimal.valueOf(0.01);  // US exchanges
            case "LSE" -> BigDecimal.valueOf(0.01);  // London
            default -> BigDecimal.valueOf(0.01);  // Default tick size
        };
    }

    /**
     * Validate price conforms to tick size
     */
    private Validation<OrderRequest, ValidationError> validatePriceTickSize(
            String field, BigDecimal price, BigDecimal tickSize) {

        BigDecimal remainder = price.remainder(tickSize);

        return switch (remainder.compareTo(BigDecimal.ZERO)) {
            case 0 -> Validation.valid(null);
            default -> Validation.invalid(
                    ValidationError.invalidTickSize(field, price.toString(), tickSize.toString())
            );
        };
    }

    /**
     * Validate stop-limit price relationship
     * MANDATORY: Rule #14 - Pattern matching for order side and type
     */
    private Validation<OrderRequest, ValidationError> validateStopLimitPriceRelationship(OrderRequest request) {
        return switch (request.orderType()) {
            case STOP_LIMIT -> Optional.ofNullable(request.limitPrice())
                    .flatMap(limitPrice -> Optional.ofNullable(request.stopPrice())
                            .map(stopPrice -> validateStopLimitRelationship(
                                    request.side(), limitPrice, stopPrice
                            ))
                    )
                    .orElse(Validation.valid(request));

            default -> Validation.valid(request);
        };
    }

    /**
     * Validate stop-limit relationship based on order side
     * MANDATORY: Rule #14 - Pattern matching with switch
     */
    private Validation<OrderRequest, ValidationError> validateStopLimitRelationship(
            OrderSide side, BigDecimal limitPrice, BigDecimal stopPrice) {

        return switch (side) {
            case BUY -> switch (stopPrice.compareTo(limitPrice)) {
                case -1 -> Validation.invalid(new ValidationError.BusinessRuleError(
                        "STOP_LIMIT_RELATIONSHIP",
                        "For BUY stop-limit orders, stop price must be >= limit price"
                ));
                default -> Validation.valid(null);
            };

            case SELL -> switch (stopPrice.compareTo(limitPrice)) {
                case 1 -> Validation.invalid(new ValidationError.BusinessRuleError(
                        "STOP_LIMIT_RELATIONSHIP",
                        "For SELL stop-limit orders, stop price must be <= limit price"
                ));
                default -> Validation.valid(null);
            };
        };
    }

    /**
     * Validate order type compatibility
     * MANDATORY: Rule #3 - Functional pattern
     */
    private Validation<OrderRequest, ValidationError> validateOrderType(OrderRequest request) {
        return Optional.ofNullable(request.orderType())
                .map(orderType -> Validation.<OrderRequest, ValidationError>valid(request))
                .orElseGet(() -> Validation.invalid(
                        ValidationError.incompatibleOrderType("null", "Order type is required")
                ));
    }

    /**
     * Validate time in force requirements
     * MANDATORY: Rule #14 - Pattern matching with switch
     */
    private Validation<OrderRequest, ValidationError> validateTimeInForce(OrderRequest request) {
        return switch (request.timeInForce()) {
            case GTD -> Optional.ofNullable(request.expiryDate())
                    .map(expiryDate -> validateExpiryDate(expiryDate, request))
                    .orElseGet(() -> Validation.invalid(
                            ValidationError.expiryDateRequired("GTD")
                    ));

            case DAY, GTC, IOC, FOK -> Validation.valid(request);
        };
    }

    /**
     * Validate expiry date
     * MANDATORY: Rule #14 - Pattern matching for date validation
     */
    private Validation<OrderRequest, ValidationError> validateExpiryDate(LocalDate expiryDate, OrderRequest request) {
        LocalDate today = LocalDate.now();

        return switch (expiryDate.compareTo(today)) {
            case -1, 0 -> Validation.invalid(
                    ValidationError.invalidExpiryDate("GTD", "Expiry date must be in the future")
            );
            case 1 -> switch (expiryDate.compareTo(today.plusDays(365))) {
                case 1 -> Validation.invalid(
                        ValidationError.invalidExpiryDate("GTD", "Expiry date cannot be more than 1 year in future")
                );
                default -> Validation.valid(request);
            };
            default -> Validation.valid(request);
        };
    }

    /**
     * Validate user permissions
     * MANDATORY: Rule #3 - Functional pattern
     */
    private Validation<OrderRequest, ValidationError> validateUserPermissions(OrderRequest request) {
        // In production, this would check user permissions from security context
        // For now, validate basic permission requirements

        return Validation.valid(request);  // Placeholder for actual permission checks
    }

    /**
     * Validate modification request
     * MANDATORY: Rule #3 - Functional composition
     */
    private Validation<OrderRequest, ValidationError> validateModificationFunctionally(
            Order existingOrder, OrderRequest modificationRequest, Long userId) {

        return Validation.validateWith(modificationRequest, List.of(
                r -> validateOrderModifiable(existingOrder),
                r -> validateCoreFieldsUnchanged(existingOrder, modificationRequest),
                r -> validateQuantityNotLessThanFilled(existingOrder, modificationRequest),
                r -> validateOrderFunctionally(r, userId)
        ));
    }

    /**
     * Validate order can be modified
     * MANDATORY: Rule #14 - Pattern matching for status check
     */
    private Validation<OrderRequest, ValidationError> validateOrderModifiable(Order existingOrder) {
        return switch (existingOrder.getStatus().isModifiable()) {
            case true -> Validation.valid(null);
            case false -> Validation.invalid(new ValidationError.BusinessRuleError(
                    "ORDER_NOT_MODIFIABLE",
                    "Order cannot be modified in status: " + existingOrder.getStatus()
            ));
        };
    }

    /**
     * Validate core fields unchanged
     * MANDATORY: Rule #13 - Stream API for field validation
     */
    private Validation<OrderRequest, ValidationError> validateCoreFieldsUnchanged(
            Order existingOrder, OrderRequest modificationRequest) {

        return Stream.of(
                validateFieldUnchanged("symbol", existingOrder.getSymbol(), modificationRequest.symbol()),
                validateFieldUnchanged("side", existingOrder.getSide().toString(), modificationRequest.side().toString()),
                validateFieldUnchanged("orderType", existingOrder.getOrderType().toString(), modificationRequest.orderType().toString())
        )
                .reduce(
                        Validation.valid(modificationRequest),
                        // Eliminates ternary using Optional.of().filter() for validation accumulation
                        (acc, validation) -> Optional.of(acc)
                            .filter(Validation::isValid)
                            .map(a -> validation)
                            .orElse(acc),
                        // Eliminates ternary using Optional.of().filter() for parallel combiner
                        (v1, v2) -> Optional.of(v1)
                            .filter(Validation::isValid)
                            .map(v -> v2)
                            .orElse(v1)
                );
    }

    /**
     * Validate field unchanged
     */
    private Validation<OrderRequest, ValidationError> validateFieldUnchanged(
            String fieldName, String existingValue, String newValue) {

        return switch (existingValue.equals(newValue)) {
            case true -> Validation.valid(null);
            case false -> Validation.invalid(new ValidationError.BusinessRuleError(
                    "FIELD_CANNOT_CHANGE",
                    fieldName + " cannot be changed during modification"
            ));
        };
    }

    /**
     * Validate quantity not less than filled
     * MANDATORY: Rule #14 - Pattern matching for comparison
     */
    private Validation<OrderRequest, ValidationError> validateQuantityNotLessThanFilled(
            Order existingOrder, OrderRequest modificationRequest) {

        return switch (Integer.compare(modificationRequest.quantity(), existingOrder.getFilledQuantity())) {
            case -1 -> Validation.invalid(new ValidationError.BusinessRuleError(
                    "QUANTITY_BELOW_FILLED",
                    "New quantity cannot be less than already filled quantity: " + existingOrder.getFilledQuantity()
            ));
            default -> Validation.valid(modificationRequest);
        };
    }

    /**
     * Adapt Validation monad to ValidationResult for interface compatibility
     * MANDATORY: Rule #14 - Pattern matching with sealed interface
     */
    private ValidationResult adaptToValidationResult(Validation<OrderRequest, ValidationError> validation) {
        return switch (validation) {
            case Validation.Valid<OrderRequest, ValidationError> v ->
                    ValidationResult.success(getValidatorName());

            case Validation.Invalid<OrderRequest, ValidationError> i -> {
                List<String> errorMessages = i.errors().stream()
                        .map(ValidationError::message)
                        .toList();

                // Record error metrics
                i.errors().forEach(error ->
                        meterRegistry.counter(VALIDATION_ERROR_METRIC,
                                "error_code", error.code(),
                                "validator", getValidatorName()
                        ).increment()
                );

                yield ValidationResult.failure(getValidatorName(), errorMessages);
            }
        };
    }

    /**
     * Record validation metrics
     * MANDATORY: Rule #15 - Structured logging and monitoring
     */
    private void recordValidationMetrics(ValidationResult result, long durationNanos) {
        Timer.builder(VALIDATION_METRIC)
                .tag("validator", getValidatorName())
                .tag("valid", String.valueOf(result.isValid()))
                .description("Order validation processing time")
                .register(meterRegistry)
                .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        meterRegistry.counter(VALIDATION_METRIC + ".total",
                "validator", getValidatorName(),
                "valid", String.valueOf(result.isValid())
        ).increment();
    }

    @Override
    public int getPriority() {
        return 1;  // Primary validator - comprehensive functional validation
    }

    @Override
    public String getValidatorName() {
        return "FunctionalOrderValidator";
    }
}
