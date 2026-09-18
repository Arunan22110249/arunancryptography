package com.orderflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class OrderFlowApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthEndpointShouldRespond() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk());
    }

    @Test
    void seededUserCanLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"admin@acme.test\",\"password\":\"demo-password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void productReadsAreTenantScoped() throws Exception {
        String adminToken = loginToken("admin@acme.test");
        String operatorToken = loginToken("ops@northwind.test");

        mockMvc.perform(get("/api/v1/products/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .header("Authorization", "Bearer " + operatorToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void repeatedOrderRequestReplaysTheSameOrder() throws Exception {
        String token = loginToken("admin@acme.test");
        String key = "test-idempotency-key";
        String body = "{\"items\":[{\"productId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"quantity\":1}]}";
        String first = mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode firstOrder = objectMapper.readTree(first);
        JsonNode secondOrder = objectMapper.readTree(second);
        org.junit.jupiter.api.Assertions.assertEquals(firstOrder.get("id").asText(), secondOrder.get("id").asText());
    }

    private String loginToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"demo-password\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }
}
