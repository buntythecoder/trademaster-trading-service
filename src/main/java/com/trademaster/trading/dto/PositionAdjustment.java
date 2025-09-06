package com.trademaster.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Position Adjustment DTO
 * 
 * Comprehensive position adjustment tracking with:
 * - Corporate action adjustments (splits, dividends, mergers)
 * - Manual position corrections and reconciliations
 * - Tax lot adjustments and basis modifications
 * - Regulatory and compliance adjustments
 * - Cost basis recalculations and optimizations
 * - Audit trail and approval workflows
 * 
 * @author TradeMaster Development Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionAdjustment {
    
    /**
     * Adjustment Identification
     */
    private String adjustmentId;
    private Long userId;
    private String symbol;
    private String adjustmentType; // CORPORATE_ACTION, MANUAL_CORRECTION, TAX_ADJUSTMENT, COMPLIANCE
    private String adjustmentReason;
    private String adjustmentCategory; // QUANTITY, PRICE, COST_BASIS, TAX_LOT
    
    /**
     * Adjustment Details
     */
    private AdjustmentDetails adjustmentDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustmentDetails {
        private Integer quantityBefore; // Position quantity before adjustment
        private Integer quantityAfter; // Position quantity after adjustment
        private Integer quantityAdjustment; // Net quantity change
        private BigDecimal priceBefore; // Average price before adjustment
        private BigDecimal priceAfter; // Average price after adjustment
        private BigDecimal priceAdjustment; // Price adjustment amount
        private BigDecimal costBasisBefore; // Cost basis before adjustment
        private BigDecimal costBasisAfter; // Cost basis after adjustment
        private BigDecimal costBasisAdjustment; // Cost basis adjustment amount
        private String adjustmentMethod; // HOW adjustment was calculated
    }
    
    /**
     * Corporate Action Details
     */
    private CorporateActionAdjustment corporateAction;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorporateActionAdjustment {
        private String actionType; // STOCK_SPLIT, STOCK_DIVIDEND, CASH_DIVIDEND, MERGER, SPINOFF
        private LocalDate announcementDate; // When corporate action announced
        private LocalDate exDate; // Ex-dividend/ex-split date
        private LocalDate recordDate; // Record date for eligibility
        private LocalDate paymentDate; // Payment/effective date
        private BigDecimal adjustmentRatio; // Split ratio (2:1 = 2.0), dividend rate
        private String newSymbol; // New symbol if changed (mergers/spinoffs)
        private BigDecimal cashPerShare; // Cash component per share
        private Integer rightsPerShare; // Rights issued per share
        private String actionStatus; // PENDING, PROCESSED, CANCELLED
    }
    
    /**
     * Tax Lot Adjustments
     */
    private List<TaxLotAdjustment> taxLotAdjustments;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxLotAdjustment {
        private String lotId; // Tax lot identifier
        private String adjustmentType; // QUANTITY, PRICE, DATE, METHOD
        private Integer quantityBefore; // Lot quantity before
        private Integer quantityAfter; // Lot quantity after
        private BigDecimal priceBefore; // Lot price before
        private BigDecimal priceAfter; // Lot price after
        private LocalDate dateBefore; // Purchase date before
        private LocalDate dateAfter; // Purchase date after
        private String termBefore; // SHORT_TERM, LONG_TERM before
        private String termAfter; // SHORT_TERM, LONG_TERM after
        private Boolean washSaleBefore; // Wash sale status before
        private Boolean washSaleAfter; // Wash sale status after
        private BigDecimal adjustmentImpact; // Financial impact of adjustment
    }
    
    /**
     * Regulatory and Compliance Adjustments
     */
    private ComplianceAdjustment complianceAdjustment;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceAdjustment {
        private String regulationType; // FINRA, SEC, IRS, CFTC
        private String complianceRule; // Specific regulation/rule
        private String violationType; // POSITION_LIMIT, WASH_SALE, PATTERN_DAY_TRADING
        private BigDecimal penaltyAmount; // Financial penalty
        private String correctionRequired; // Required corrective action
        private LocalDate complianceDate; // Date compliance required
        private String complianceStatus; // PENDING, COMPLETED, APPEALED
        private String approvalRequired; // WHO must approve adjustment
    }
    
    /**
     * Adjustment Impact Analysis
     */
    private ImpactAnalysis impactAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImpactAnalysis {
        private BigDecimal pnlImpact; // P&L impact of adjustment
        private BigDecimal taxImpact; // Tax liability impact
        private BigDecimal riskImpact; // Risk metric changes
        private BigDecimal marginImpact; // Margin requirement changes
        private BigDecimal portfolioImpact; // Portfolio weight changes
        private String performanceImpact; // Effect on performance metrics
        private List<String> downstreamEffects; // Other affected positions/accounts
        private BigDecimal netCashImpact; // Net cash flow impact
    }
    
    /**
     * Processing and Approval Workflow
     */
    private ProcessingWorkflow workflow;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingWorkflow {
        private String initiatedBy; // User who initiated adjustment
        private Instant initiatedAt; // When adjustment was initiated
        private String approvedBy; // User who approved adjustment
        private Instant approvedAt; // When adjustment was approved
        private String processedBy; // System/user who processed
        private Instant processedAt; // When adjustment was processed
        private String workflowStatus; // PENDING, APPROVED, REJECTED, PROCESSED
        private String rejectionReason; // Reason if rejected
        private List<String> approvalComments; // Comments from approvers
        private Boolean requiresManagerApproval; // Manager approval required
        private Boolean requiresComplianceApproval; // Compliance approval required
    }
    
    /**
     * Audit Trail
     */
    private AuditTrail auditTrail;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditTrail {
        private String originalRequestId; // Original request identifier
        private String sourceSystem; // System that generated adjustment
        private String sourceDocument; // Supporting document reference
        private List<String> supportingDocuments; // Additional supporting docs
        private String changeReason; // Business reason for change
        private String riskAssessment; // Risk assessment of change
        private String businessJustification; // Business justification
        private Instant createdAt; // When record was created
        private Instant lastModifiedAt; // Last modification timestamp
        private String lastModifiedBy; // Who made last modification
    }
    
    /**
     * Reconciliation Information
     */
    private ReconciliationInfo reconciliation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationInfo {
        private String reconciliationId; // Reconciliation batch identifier
        private LocalDate reconciliationDate; // Date of reconciliation
        private String custodianSource; // Source of custodian data
        private BigDecimal custodianQuantity; // Quantity per custodian
        private BigDecimal systemQuantity; // Quantity per our system
        private BigDecimal quantityVariance; // Difference (custodian - system)
        private BigDecimal custodianValue; // Value per custodian
        private BigDecimal systemValue; // Value per our system
        private BigDecimal valueVariance; // Value difference
        private String varianceReason; // Reason for variance
        private Boolean autoReconciled; // Automatically reconciled
        private String reconciliationMethod; // How variance was resolved
    }
    
    /**
     * Helper Methods
     */
    
    /**
     * Check if adjustment is pending approval
     */
    public boolean isPendingApproval() {
        return workflow != null && 
               "PENDING".equals(workflow.getWorkflowStatus()) &&
               workflow.getApprovedAt() == null;
    }
    
    /**
     * Check if adjustment has significant financial impact
     */
    public boolean hasSignificantImpact(BigDecimal threshold) {
        if (impactAnalysis == null) return false;
        
        BigDecimal totalImpact = BigDecimal.ZERO;
        if (impactAnalysis.getPnlImpact() != null) {
            totalImpact = totalImpact.add(impactAnalysis.getPnlImpact().abs());
        }
        if (impactAnalysis.getNetCashImpact() != null) {
            totalImpact = totalImpact.add(impactAnalysis.getNetCashImpact().abs());
        }
        
        return totalImpact.compareTo(threshold) > 0;
    }
    
    /**
     * Check if adjustment requires regulatory approval
     */
    public boolean requiresRegulatoryApproval() {
        return complianceAdjustment != null && 
               complianceAdjustment.getApprovalRequired() != null;
    }
    
    /**
     * Get processing time in hours
     */
    public Long getProcessingTimeHours() {
        if (workflow == null || 
            workflow.getInitiatedAt() == null || 
            workflow.getProcessedAt() == null) {
            return 0L;
        }
        
        return java.time.Duration.between(
            workflow.getInitiatedAt(), 
            workflow.getProcessedAt()
        ).toHours();
    }
    
    /**
     * Check if adjustment is corporate action related
     */
    public boolean isCorporateActionAdjustment() {
        return "CORPORATE_ACTION".equals(adjustmentType) && 
               corporateAction != null;
    }
    
    /**
     * Get adjustment summary for reporting
     */
    public String getAdjustmentSummary() {
        if (adjustmentDetails == null) return "No adjustment details";
        
        StringBuilder summary = new StringBuilder();
        summary.append("Type: ").append(adjustmentType != null ? adjustmentType : "UNKNOWN");
        
        if (adjustmentDetails.getQuantityAdjustment() != null && 
            adjustmentDetails.getQuantityAdjustment() != 0) {
            summary.append(", Qty: ").append(adjustmentDetails.getQuantityAdjustment());
        }
        
        if (adjustmentDetails.getPriceAdjustment() != null && 
            adjustmentDetails.getPriceAdjustment().compareTo(BigDecimal.ZERO) != 0) {
            summary.append(", Price: $").append(adjustmentDetails.getPriceAdjustment());
        }
        
        if (adjustmentDetails.getCostBasisAdjustment() != null && 
            adjustmentDetails.getCostBasisAdjustment().compareTo(BigDecimal.ZERO) != 0) {
            summary.append(", Cost Basis: $").append(adjustmentDetails.getCostBasisAdjustment());
        }
        
        return summary.toString();
    }
    
    /**
     * Static factory methods
     */
    
    public static PositionAdjustment corporateActionAdjustment(Long userId, String symbol, 
            String actionType, BigDecimal ratio, LocalDate exDate) {
        return PositionAdjustment.builder()
            .adjustmentId("CA_" + System.currentTimeMillis())
            .userId(userId)
            .symbol(symbol)
            .adjustmentType("CORPORATE_ACTION")
            .adjustmentReason("Corporate Action: " + actionType)
            .corporateAction(CorporateActionAdjustment.builder()
                .actionType(actionType)
                .adjustmentRatio(ratio)
                .exDate(exDate)
                .actionStatus("PENDING")
                .build())
            .workflow(ProcessingWorkflow.builder()
                .initiatedBy("SYSTEM")
                .initiatedAt(Instant.now())
                .workflowStatus("PENDING")
                .build())
            .build();
    }
    
    public static PositionAdjustment reconciliationAdjustment(Long userId, String symbol, 
            BigDecimal quantityVariance, String reason) {
        return PositionAdjustment.builder()
            .adjustmentId("RECON_" + System.currentTimeMillis())
            .userId(userId)
            .symbol(symbol)
            .adjustmentType("MANUAL_CORRECTION")
            .adjustmentReason("Reconciliation: " + reason)
            .adjustmentDetails(AdjustmentDetails.builder()
                .quantityAdjustment(quantityVariance.intValue())
                .adjustmentMethod("RECONCILIATION")
                .build())
            .reconciliation(ReconciliationInfo.builder()
                .reconciliationDate(LocalDate.now())
                .quantityVariance(quantityVariance)
                .varianceReason(reason)
                .autoReconciled(false)
                .build())
            .build();
    }
}