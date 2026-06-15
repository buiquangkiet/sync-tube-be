package com.example.synctube.dto.response;

import com.example.synctube.entity.PlaylistItem;
import com.example.synctube.entity.Room;
import com.example.synctube.repository.PlaylistItemRepository;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RoomResponse {

    private Long id;
    private String name;
    private String code;
    private Long hostId;
    private String hostUsername;
    private String videoUrl;
    private Long currentPlaylistItemId;
    private boolean playing;
    private double currentTimeSeconds;
    private List<PlaylistItemResponse> playlistItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RoomResponse from(Room room, List<PlaylistItem> playlistItems) {
        PlaylistItem current = room.getCurrentPlaylistItem();
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .code(room.getCode())
                .hostId(room.getHost().getId())
                .hostUsername(room.getHost().getUsername())
                .videoUrl(current != null ? current.getVideoUrl() : null)
                .currentPlaylistItemId(current != null ? current.getId() : null)
                .playing(room.isPlaying())
                .currentTimeSeconds(room.getCurrentTimeSeconds())
                .playlistItems(playlistItems.stream().map(PlaylistItemResponse::from).toList())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    public static RoomResponse from(Room room, PlaylistItemRepository playlistItemRepository) {
        List<PlaylistItem> items = playlistItemRepository.findByRoomOrderByPositionAsc(room);
        return from(room, items);
    }
}
