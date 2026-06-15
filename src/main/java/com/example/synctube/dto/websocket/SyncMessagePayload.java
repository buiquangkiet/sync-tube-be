package com.example.synctube.dto.websocket;

import com.example.synctube.dto.response.PlaylistItemResponse;
import com.example.synctube.dto.response.RoomResponse;
import com.example.synctube.entity.SyncAction;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SyncMessagePayload {

    private String roomCode;
    private String action;
    private Boolean playing;
    private Double currentTimeSeconds;
    private String videoUrl;
    private Long currentPlaylistItemId;
    private List<PlaylistItemResponse> playlistItems;
    private Long senderId;

    public static SyncMessagePayload from(RoomResponse room, SyncAction action, Long senderId) {
        return SyncMessagePayload.builder()
                .roomCode(room.getCode())
                .action(action.name())
                .playing(room.isPlaying())
                .currentTimeSeconds(room.getCurrentTimeSeconds())
                .videoUrl(room.getVideoUrl())
                .currentPlaylistItemId(room.getCurrentPlaylistItemId())
                .playlistItems(room.getPlaylistItems())
                .senderId(senderId)
                .build();
    }
}
