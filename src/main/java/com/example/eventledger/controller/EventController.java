package com.example.eventledger.controller;

import com.example.eventledger.dto.EventRequest;
import com.example.eventledger.entity.Event;
import com.example.eventledger.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@Valid @RequestBody EventRequest request) {

        Event existingOrCreated = eventService.createEvent(request);

        if (existingOrCreated.getEventId().equals(request.getEventId())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(existingOrCreated);
        }

        return ResponseEntity.ok(existingOrCreated);
    }

    @GetMapping("/{id}")
    public Event getEvent(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @GetMapping
    public List<Event> getEventsByAccount(@RequestParam("account") String accountId) {
        return eventService.getEventsByAccount(accountId);
    }
}