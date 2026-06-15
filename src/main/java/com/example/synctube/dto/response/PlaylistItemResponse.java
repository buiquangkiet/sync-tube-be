package com.example.synctube.dto.response;

import com.example.synctube.entity.PlaylistItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PlaylistItemResponse {

    private Long id;
    private String videoUrl;
    private String title;
    private int position;
    private Long addedById;
    private String addedByUsername;
    private LocalDateTime createdAt;

    public static PlaylistItemResponse from(PlaylistItem item) {
        return PlaylistItemResponse.builder()
                .id(item.getId())
                .videoUrl(item.getVideoUrl())
                .title(item.getTitle())
                .position(item.getPosition())
                .addedById(item.getAddedBy() != null ? item.getAddedBy().getId() : null)
                .addedByUsername(item.getAddedBy() != null ? item.getAddedBy().getUsername() : null)
                .createdAt(item.getCreatedAt())
                .build();
    }
}
