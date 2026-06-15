package com.example.synctube.service;

import com.example.synctube.dto.request.AddPlaylistItemRequest;
import com.example.synctube.dto.request.ReorderPlaylistRequest;
import com.example.synctube.dto.response.PlaylistItemResponse;
import com.example.synctube.dto.response.RoomResponse;
import com.example.synctube.entity.PlaylistItem;
import com.example.synctube.entity.Room;
import com.example.synctube.entity.SyncAction;
import com.example.synctube.entity.User;
import com.example.synctube.exception.BadRequestException;
import com.example.synctube.exception.ResourceNotFoundException;
import com.example.synctube.repository.PlaylistItemRepository;
import com.example.synctube.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

    private final RoomRepository roomRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final UserService userService;
    private final RoomSyncPublisher roomSyncPublisher;
    private final RoomService roomService;

    public List<PlaylistItemResponse> getPlaylist(String code) {
        Room room = roomService.findRoomByCode(code);
        return playlistItemRepository.findByRoomOrderByPositionAsc(room).stream()
                .map(PlaylistItemResponse::from)
                .toList();
    }

    @Transactional
    public RoomResponse addItem(String code, AddPlaylistItemRequest request, Long userId) {
        if (request.getVideoUrl() == null || request.getVideoUrl().isBlank()) {
            throw new BadRequestException("Video URL is required");
        }
        RoomService.validateYouTubeUrl(request.getVideoUrl());

        Room room = roomService.findRoomByCode(code);
        User user = userService.findUserById(userId);
        List<PlaylistItem> items = playlistItemRepository.findByRoomOrderByPositionAsc(room);
        int nextPosition = items.isEmpty() ? 0 : items.get(items.size() - 1).getPosition() + 1;

        PlaylistItem item = playlistItemRepository.save(PlaylistItem.builder()
                .room(room)
                .videoUrl(request.getVideoUrl().trim())
                .title(request.getTitle())
                .position(nextPosition)
                .addedBy(user)
                .build());

        if (room.getCurrentPlaylistItem() == null) {
            room.setCurrentPlaylistItem(item);
            room.setPlaying(false);
            room.setCurrentTimeSeconds(0.0);
            roomRepository.save(room);
        }

        RoomResponse response = roomService.toResponse(room);
        roomSyncPublisher.publish(response, SyncAction.PLAYLIST_ADD, userId);
        return response;
    }

    @Transactional
    public RoomResponse removeItem(String code, Long itemId, Long userId) {
        Room room = roomService.findRoomByCode(code);
        PlaylistItem item = playlistItemRepository.findByIdAndRoom(itemId, room)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist item not found"));

        boolean isCurrent = room.getCurrentPlaylistItem() != null
                && room.getCurrentPlaylistItem().getId().equals(itemId);
        List<PlaylistItem> items = playlistItemRepository.findByRoomOrderByPositionAsc(room);

        if (isCurrent && items.size() <= 1) {
            throw new BadRequestException("Cannot remove the only track in the playlist");
        }

        playlistItemRepository.delete(item);

        List<PlaylistItem> remaining = playlistItemRepository.findByRoomOrderByPositionAsc(room);
        reindexPositions(remaining);

        if (isCurrent) {
            PlaylistItem next = remaining.isEmpty() ? null : remaining.get(0);
            room.setCurrentPlaylistItem(next);
            room.setCurrentTimeSeconds(0.0);
            room.setPlaying(next != null);
            roomRepository.save(room);
        }

        RoomResponse response = roomService.toResponse(room);
        roomSyncPublisher.publish(response, SyncAction.PLAYLIST_REMOVE, userId);
        return response;
    }

    @Transactional
    public RoomResponse reorder(String code, ReorderPlaylistRequest request, Long userId) {
        if (request.getItemIds() == null || request.getItemIds().isEmpty()) {
            throw new BadRequestException("Item IDs are required");
        }

        Room room = roomService.findRoomByCode(code);
        List<PlaylistItem> items = playlistItemRepository.findByRoomOrderByPositionAsc(room);
        Set<Long> existingIds = new HashSet<>(items.stream().map(PlaylistItem::getId).toList());

        if (request.getItemIds().size() != existingIds.size()
                || !existingIds.containsAll(request.getItemIds())) {
            throw new BadRequestException("Invalid playlist item order");
        }

        for (int i = 0; i < request.getItemIds().size(); i++) {
            Long itemId = request.getItemIds().get(i);
            PlaylistItem item = items.stream()
                    .filter(it -> it.getId().equals(itemId))
                    .findFirst()
                    .orElseThrow();
            item.setPosition(i);
            playlistItemRepository.save(item);
        }

        RoomResponse response = roomService.toResponse(room);
        roomSyncPublisher.publish(response, SyncAction.PLAYLIST_REORDER, userId);
        return response;
    }

    private void reindexPositions(List<PlaylistItem> items) {
        for (int i = 0; i < items.size(); i++) {
            PlaylistItem item = items.get(i);
            item.setPosition(i);
            playlistItemRepository.save(item);
        }
    }
}
