package com.example.eventledger;

import com.example.eventledger.dto.EventRequest;
import com.example.eventledger.entity.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EventLedgerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldHandleDuplicateEvents() throws Exception {

        EventRequest request = buildRequest("evt-001", "acct-1",
                EventType.CREDIT, "100.00", "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnEventsOrderedByTimestamp() throws Exception {

        EventRequest newer = buildRequest("evt-002", "acct-2",
                EventType.CREDIT, "100.00", "2026-05-15T12:00:00Z");

        EventRequest older = buildRequest("evt-003", "acct-2",
                EventType.DEBIT, "20.00", "2026-05-15T09:00:00Z");

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newer)));

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(older)));

        mockMvc.perform(get("/events?account=acct-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt-003"))
                .andExpect(jsonPath("$[1].eventId").value("evt-002"));
    }

    @Test
    void shouldCalculateBalanceCorrectly() throws Exception {

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        buildRequest("evt-010", "acct-3",
                                EventType.CREDIT, "200.00", "2026-05-15T10:00:00Z"))));

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        buildRequest("evt-011", "acct-3",
                                EventType.DEBIT, "50.00", "2026-05-15T11:00:00Z"))));

        mockMvc.perform(get("/accounts/acct-3/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));
    }

    @Test
    void shouldRejectInvalidAmount() throws Exception {

        EventRequest request = buildRequest("evt-100", "acct-4",
                EventType.CREDIT, "-10.00", "2026-05-15T10:00:00Z");

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private EventRequest buildRequest(String eventId,
                                      String accountId,
                                      EventType type,
                                      String amount,
                                      String timestamp) {

        EventRequest request = new EventRequest();
        request.setEventId(eventId);
        request.setAccountId(accountId);
        request.setType(type);
        request.setAmount(new BigDecimal(amount));
        request.setCurrency("USD");
        request.setEventTimestamp(Instant.parse(timestamp));

        return request;
    }

    @Test
    void duplicateEventShouldNotChangeBalance() throws Exception {

        createEvent(
                "evt-dup-balance",
                "acct-dup",
                EventType.CREDIT,
                "100.00"
        );

        createEvent(
                "evt-dup-balance",
                "acct-dup",
                EventType.CREDIT,
                "100.00"
        );

        mockMvc.perform(get("/accounts/acct-dup/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void balancesShouldBeIndependentAcrossAccounts() throws Exception {

        createEvent("evt-a1", "acct-A", EventType.CREDIT, "200.00");

        createEvent("evt-a2", "acct-B", EventType.CREDIT, "500.00");

        createEvent("evt-a3", "acct-A", EventType.DEBIT, "50.00");

        mockMvc.perform(get("/accounts/acct-A/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));

        mockMvc.perform(get("/accounts/acct-B/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void shouldStoreMetadataSuccessfully() throws Exception {

        String request = """
            {
              "eventId": "evt-meta",
              "accountId": "acct-meta",
              "type": "CREDIT",
              "amount": 100,
              "currency": "USD",
              "eventTimestamp": "2026-05-15T10:00:00Z",
              "metadata": {
                "source": "batch",
                "batchId": "B-100"
              }
            }
            """;

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-meta"))
                .andExpect(jsonPath("$.metadata").exists());
    }

    @Test
    void shouldRejectMissingCurrency() throws Exception {

        String request = """
            {
              "eventId": "evt-currency",
              "accountId": "acct-1",
              "type": "CREDIT",
              "amount": 100,
              "eventTimestamp": "2026-05-15T10:00:00Z"
            }
            """;

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingTimestamp() throws Exception {

        String request = """
            {
              "eventId": "evt-time",
              "accountId": "acct-1",
              "type": "CREDIT",
              "amount": 100,
              "currency": "USD"
            }
            """;

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleDecimalAmountsCorrectly() throws Exception {

        createEvent("evt-dec-1", "acct-dec", EventType.CREDIT, "100.55");

        createEvent("evt-dec-2", "acct-dec", EventType.DEBIT, "50.25");

        mockMvc.perform(get("/accounts/acct-dec/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.30));
    }

    @Test
    void shouldHandleLargeTransactionAmounts() throws Exception {

        createEvent(
                "evt-large",
                "acct-large",
                EventType.CREDIT,
                "999999999.99"
        );

        mockMvc.perform(get("/accounts/acct-large/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(999999999.99));
    }

    private void createEvent(
            String eventId,
            String accountId,
            EventType type,
            String amount
    ) throws Exception {

        EventRequest request = buildRequest(
                eventId,
                accountId,
                type,
                amount,
                "2026-05-15T10:00:00Z"
        );

        mockMvc.perform(post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }



}