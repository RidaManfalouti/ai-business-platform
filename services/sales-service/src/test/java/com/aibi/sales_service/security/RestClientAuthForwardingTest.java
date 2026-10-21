package com.aibi.sales_service.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
class RestClientAuthForwardingTest {

    @Autowired
    @Qualifier("productRestClient")
    private RestClient productRestClient;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Propagation de l'en-tête Authorization vers les appels HTTP sortants")
    void shouldForwardAuthorizationHeaderWhenPresentInRequestContext() {
        String testJwt = "Bearer eyJhbGciOiJIUzI1NiJ9.test.signature";

        // Simule une requête entrante avec token Bearer
        MockHttpServletRequest incomingRequest = new MockHttpServletRequest();
        incomingRequest.addHeader(HttpHeaders.AUTHORIZATION, testJwt);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(incomingRequest));

        RestClient.Builder clientBuilder = productRestClient.mutate();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(clientBuilder).build();
        RestClient client = clientBuilder.build();

        mockServer.expect(requestTo("http://localhost:8082/api/products/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, testJwt))
                .andRespond(withSuccess("{\"id\":1,\"name\":\"Test Product\",\"price\":10.0,\"stockQuantity\":5}", MediaType.APPLICATION_JSON));

        client.get()
                .uri("/api/products/1")
                .retrieve()
                .toBodilessEntity();

        mockServer.verify();
    }
}
