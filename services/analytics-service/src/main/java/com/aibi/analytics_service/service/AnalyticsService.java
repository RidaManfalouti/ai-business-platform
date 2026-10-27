package com.aibi.analytics_service.service;

import com.aibi.analytics_service.client.CustomerServiceClient;
import com.aibi.analytics_service.client.ProductServiceClient;
import com.aibi.analytics_service.client.SalesServiceClient;
import com.aibi.analytics_service.client.dto.*;
import com.aibi.analytics_service.dto.*;
import com.aibi.analytics_service.exception.InvalidDateRangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final SalesServiceClient salesServiceClient;
    private final ProductServiceClient productServiceClient;
    private final CustomerServiceClient customerServiceClient;

    private final String currency;
    private final String defaultTimezone;
    private final int defaultLowStockThreshold;

    public AnalyticsService(
            SalesServiceClient salesServiceClient,
            ProductServiceClient productServiceClient,
            CustomerServiceClient customerServiceClient,
            @Value("${analytics.currency:EUR}") String currency,
            @Value("${analytics.default-timezone:UTC}") String defaultTimezone,
            @Value("${analytics.low-stock-threshold:5}") int defaultLowStockThreshold) {
        this.salesServiceClient = salesServiceClient;
        this.productServiceClient = productServiceClient;
        this.customerServiceClient = customerServiceClient;
        this.currency = currency;
        this.defaultTimezone = defaultTimezone;
        this.defaultLowStockThreshold = defaultLowStockThreshold;
    }

    public record ResolvedPeriod(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Instant startInstant,
            Instant endInstant,
            ZoneId zoneId
    ) {}

    public ResolvedPeriod resolveAndValidatePeriod(LocalDate from, LocalDate to, String timezoneParam) {
        ZoneId zoneId;
        try {
            zoneId = (timezoneParam != null && !timezoneParam.isBlank())
                    ? ZoneId.of(timezoneParam.trim())
                    : ZoneId.of(defaultTimezone);
        } catch (Exception ex) {
            throw new InvalidDateRangeException("Fuseau horaire invalide ou non reconnu : '" + timezoneParam + "'.");
        }

        LocalDate effectiveTo = (to != null) ? to : LocalDate.now(zoneId);
        LocalDate effectiveFrom = (from != null) ? from : effectiveTo.minusDays(30);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new InvalidDateRangeException(
                    "La date de début (" + effectiveFrom + ") doit être antérieure ou égale à la date de fin (" + effectiveTo + ")."
            );
        }

        long daysBetween = ChronoUnit.DAYS.between(effectiveFrom, effectiveTo);
        if (daysBetween > 365) {
            throw new InvalidDateRangeException(
                    "La plage temporelle demandée (" + daysBetween + " jours) dépasse la limite autorisée de 365 jours."
            );
        }

        LocalDateTime startDateTime = effectiveFrom.atStartOfDay();
        LocalDateTime endDateTime = effectiveTo.atTime(23, 59, 59, 999_999_999);

        Instant startInstant = startDateTime.atZone(zoneId).toInstant();
        Instant endInstant = endDateTime.atZone(zoneId).toInstant();

        return new ResolvedPeriod(startDateTime, endDateTime, startInstant, endInstant, zoneId);
    }

    public KpiSummaryResponse getKpiSummary(LocalDate from, LocalDate to, String timezoneParam) {
        ResolvedPeriod period = resolveAndValidatePeriod(from, to, timezoneParam);

        SalesSummaryDto salesSummary = salesServiceClient.getSummaryAggregate(period.startDateTime(), period.endDateTime());
        long totalCustomers = customerServiceClient.getTotalCustomersCount();
        long totalProducts = productServiceClient.getTotalProductsCount();

        BigDecimal totalRevenue = (salesSummary != null && salesSummary.totalRevenue() != null)
                ? salesSummary.totalRevenue().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        long completedSales = (salesSummary != null) ? salesSummary.completedSalesCount() : 0L;
        long totalSales = (salesSummary != null) ? salesSummary.totalSalesCount() : 0L;

        BigDecimal averageOrderValue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (completedSales > 0) {
            averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(completedSales), 2, RoundingMode.HALF_UP);
        }

        return new KpiSummaryResponse(
                totalRevenue,
                completedSales,
                averageOrderValue,
                totalSales,
                totalCustomers,
                totalProducts,
                currency,
                period.startInstant(),
                period.endInstant(),
                period.zoneId().getId()
        );
    }

    public TrendsResponse getTrends(LocalDate from, LocalDate to, String timezoneParam) {
        ResolvedPeriod period = resolveAndValidatePeriod(from, to, timezoneParam);

        List<DailyTrendDto> rawTrends = salesServiceClient.getDailyTrends(period.startDateTime(), period.endDateTime());
        List<RevenueTrendPoint> trendPoints = new ArrayList<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalOrders = 0;

        if (rawTrends != null) {
            for (DailyTrendDto pt : rawTrends) {
                BigDecimal rev = pt.totalRevenue() != null ? pt.totalRevenue() : BigDecimal.ZERO;
                totalRevenue = totalRevenue.add(rev);
                totalOrders += pt.orderCount();
                trendPoints.add(new RevenueTrendPoint(pt.date(), pt.orderCount(), rev.setScale(2, RoundingMode.HALF_UP), currency));
            }
        }

        return new TrendsResponse(
                trendPoints,
                totalRevenue.setScale(2, RoundingMode.HALF_UP),
                totalOrders,
                currency
        );
    }

    public TopProductsResponse getTopProducts(LocalDate from, LocalDate to, String timezoneParam, int limit) {
        ResolvedPeriod period = resolveAndValidatePeriod(from, to, timezoneParam);
        int boundedLimit = Math.max(1, Math.min(limit, 50));

        List<TopProductDto> rawTop = salesServiceClient.getTopProducts(period.startDateTime(), period.endDateTime(), boundedLimit);
        List<TopProductItem> items = new ArrayList<>();

        long totalQty = 0;
        BigDecimal totalRev = BigDecimal.ZERO;

        if (rawTop != null) {
            for (TopProductDto item : rawTop) {
                BigDecimal itemRev = item.totalRevenue() != null ? item.totalRevenue() : BigDecimal.ZERO;
                totalQty += item.totalQuantity();
                totalRev = totalRev.add(itemRev);
                items.add(new TopProductItem(
                        item.productId(),
                        item.productName(),
                        item.totalQuantity(),
                        itemRev.setScale(2, RoundingMode.HALF_UP),
                        currency
                ));
            }
        }

        return new TopProductsResponse(
                items,
                totalQty,
                totalRev.setScale(2, RoundingMode.HALF_UP),
                currency
        );
    }

    public StatusDistributionResponse getStatusDistribution(LocalDate from, LocalDate to, String timezoneParam) {
        ResolvedPeriod period = resolveAndValidatePeriod(from, to, timezoneParam);

        List<SalesStatusCountDto> counts = salesServiceClient.getStatusDistribution(period.startDateTime(), period.endDateTime());
        long totalOrders = 0;

        if (counts != null) {
            for (SalesStatusCountDto item : counts) {
                totalOrders += item.count();
            }
        }

        List<StatusDistributionItem> distribution = new ArrayList<>();
        if (counts != null) {
            for (SalesStatusCountDto item : counts) {
                double percentage = 0.0;
                if (totalOrders > 0) {
                    percentage = BigDecimal.valueOf((item.count() * 100.0) / totalOrders)
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue();
                }
                distribution.add(new StatusDistributionItem(item.status(), item.count(), percentage));
            }
        }

        return new StatusDistributionResponse(distribution, totalOrders);
    }

    public LowStockResponse getLowStock(Integer threshold, Integer limit) {
        int effectiveThreshold = (threshold != null && threshold >= 0) ? threshold : defaultLowStockThreshold;
        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;

        ProductPageDto page = productServiceClient.getProductsSortedByStock(effectiveLimit);
        List<LowStockItem> lowStockItems = new ArrayList<>();

        if (page != null && page.content() != null) {
            for (ProductItemDto prod : page.content()) {
                if (prod.stockQuantity() != null && prod.stockQuantity() <= effectiveThreshold) {
                    String stockStatus = prod.stockQuantity() == 0 ? "OUT_OF_STOCK" : "LOW_STOCK";
                    lowStockItems.add(new LowStockItem(
                            prod.id(),
                            prod.name(),
                            prod.category(),
                            prod.price(),
                            prod.stockQuantity(),
                            stockStatus
                    ));
                }
            }
        }

        return new LowStockResponse(lowStockItems, lowStockItems.size(), effectiveThreshold);
    }

    public DashboardAnalyticsResponse getDashboard(LocalDate from, LocalDate to, String timezoneParam, Integer topLimit, Integer lowStockThreshold) {
        KpiSummaryResponse kpi = getKpiSummary(from, to, timezoneParam);
        TrendsResponse trends = getTrends(from, to, timezoneParam);
        TopProductsResponse topProducts = getTopProducts(from, to, timezoneParam, topLimit != null ? topLimit : 5);
        StatusDistributionResponse statusDistribution = getStatusDistribution(from, to, timezoneParam);
        LowStockResponse lowStock = getLowStock(lowStockThreshold, 10);

        return new DashboardAnalyticsResponse(kpi, trends, topProducts, statusDistribution, lowStock);
    }
}
