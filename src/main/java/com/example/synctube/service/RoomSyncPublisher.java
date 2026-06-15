package com.example.synctube.service;

import com.example.synctube.dto.response.RoomResponse;
import com.example.synctube.dto.websocket.SyncMessagePayload;
import com.example.synctube.entity.SyncAction;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomSyncPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(RoomResponse room, SyncAction action, Long senderId) {
        SyncMessagePayload payload = SyncMessagePayload.from(room, action, senderId);
        messagingTemplate.convertAndSend("/topic/room/" + room.getCode(), payload);
    }
}
