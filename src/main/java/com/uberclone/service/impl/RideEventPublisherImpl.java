package com.uberclone.service.impl;

import com.uberclone.dto.RideResponseDTO;
import com.uberclone.event.RideEvent;
import com.uberclone.service.RideEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RideEventPublisherImpl implements RideEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public RideEventPublisherImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishRideAccepted(RideResponseDTO ride) {
        messagingTemplate.convertAndSend("/topic/rides/" + ride.getId(), new RideEvent("ACCEPTED", ride));
    }

    @Override
    public void publishRideStarted(RideResponseDTO ride) {
        messagingTemplate.convertAndSend("/topic/rides/" + ride.getId(), new RideEvent("STARTED", ride));
    }

    @Override
    public void publishRideCompleted(RideResponseDTO ride) {
        messagingTemplate.convertAndSend("/topic/rides/" + ride.getId(), new RideEvent("COMPLETED", ride));
    }
}
