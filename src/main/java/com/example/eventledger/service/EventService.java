package com.example.eventledger.service;

import com.example.eventledger.dto.BalanceResponse;
import com.example.eventledger.dto.EventRequest;
import com.example.eventledger.entity.Event;
import com.example.eventledger.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public EventService(EventRepository eventRepository, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Event createEvent(EventRequest request) {
        return eventRepository.findByEventId(request.getEventId())
                .orElseGet(() -> saveNewEvent(request));
    }

    private Event saveNewEvent(EventRequest request) {
        try {
            Event event = new Event();
            event.setEventId(request.getEventId());
            event.setAccountId(request.getAccountId());
            event.setType(request.getType());
            event.setAmount(request.getAmount());
            event.setCurrency(request.getCurrency());
            event.setEventTimestamp(request.getEventTimestamp());

            if (request.getMetadata() != null) {
                event.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            }

            return eventRepository.save(event);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to process event", ex);
        }
    }

    public Event getEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
    }

    public List<Event> getEventsByAccount(String accountId) {
        return eventRepository.findByAccountIdOrderByEventTimestampAsc(accountId);
    }

    public BalanceResponse getBalance(String accountId) {
        BigDecimal balance = eventRepository.calculateBalance(accountId);
        return new BalanceResponse(accountId, balance);
    }
}