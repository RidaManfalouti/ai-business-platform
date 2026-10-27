package com.aibi.sales_service.controller;

import com.aibi.sales_service.dto.DailyTrendResponse;
import com.aibi.sales_service.dto.SalesStatusCountResponse;
import com.aibi.sales_service.dto.SalesSummaryAggregateResponse;
import com.aibi.sales_service.dto.TopProductAggregateResponse;
import com.aibi.sales_service.model.SaleStatus;
import com.aibi.sales_service.security.JwtRoleConverter;
import com.aibi.sales_service.service.SalesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalesAggregationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JwtRoleConverter.class)
class SalesAggregationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalesService salesService;

    @Test
    @DisplayName("GET /api/sales/aggregates/summary retourne le résumé agrégé")
    void testGetSummary() throws Exception {
        when(salesService.getSummaryAggregate(any(), any()))
                .thenReturn(new SalesSummaryAggregateResponse(new BigDecimal("15000.50"), 25L, 30L));

        mockMvc.perform(get("/api/sales/aggregates/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(15000.50))
                .andExpect(jsonPath("$.completedSalesCount").value(25))
                .andExpect(jsonPath("$.totalSalesCount").value(30));
    }

    @Test
    @DisplayName("GET /api/sales/aggregates/status-distribution retourne la répartition par statut")
    void testGetStatusDistribution() throws Exception {
        when(salesService.getStatusDistribution(any(), any()))
                .thenReturn(List.of(
                        new SalesStatusCountResponse(SaleStatus.COMPLETED, 25L),
                        new SalesStatusCountResponse(SaleStatus.PENDING, 5L)
                ));

        mockMvc.perform(get("/api/sales/aggregates/status-distribution")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].count").value(25))
                .andExpect(jsonPath("$[1].status").value("PENDING"))
                .andExpect(jsonPath("$[1].count").value(5));
    }

    @Test
    @DisplayName("GET /api/sales/aggregates/trends retourne la liste des tendances quotidiennes")
    void testGetDailyTrends() throws Exception {
        when(salesService.getDailyTrends(any(), any()))
                .thenReturn(List.of(
                        new DailyTrendResponse(LocalDate.of(2026, 10, 25), 10L, new BigDecimal("5000.00")),
                        new DailyTrendResponse(LocalDate.of(2026, 10, 26), 15L, new BigDecimal("10000.50"))
                ));

        mockMvc.perform(get("/api/sales/aggregates/trends")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-10-25"))
                .andExpect(jsonPath("$[0].orderCount").value(10))
                .andExpect(jsonPath("$[0].totalRevenue").value(5000.00))
                .andExpect(jsonPath("$[1].date").value("2026-10-26"))
                .andExpect(jsonPath("$[1].orderCount").value(15))
                .andExpect(jsonPath("$[1].totalRevenue").value(10000.50));
    }

    @Test
    @DisplayName("GET /api/sales/aggregates/top-products retourne les produits les plus vendus")
    void testGetTopProducts() throws Exception {
        when(salesService.getTopProducts(any(), any(), eq(5)))
                .thenReturn(List.of(
                        new TopProductAggregateResponse(1L, "Laptop Pro", 40L, new BigDecimal("48000.00")),
                        new TopProductAggregateResponse(2L, "Wireless Mouse", 30L, new BigDecimal("900.00"))
                ));

        mockMvc.perform(get("/api/sales/aggregates/top-products?limit=5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].productName").value("Laptop Pro"))
                .andExpect(jsonPath("$[0].totalQuantity").value(40))
                .andExpect(jsonPath("$[0].totalRevenue").value(48000.00));
    }
}
