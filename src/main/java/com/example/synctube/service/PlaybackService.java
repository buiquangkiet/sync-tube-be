package com.example.synctube.service;

import com.example.synctube.dto.response.RoomResponse;
import com.example.synctube.entity.PlaylistItem;
import com.example.synctube.entity.Room;
import com.example.synctube.entity.SyncAction;
import com.example.synctube.exception.BadRequestException;
import com.example.synctube.repository.PlaylistItemRepository;
import com.example.synctube.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaybackService {

    private final RoomRepository roomRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final RoomSyncPublisher roomSyncPublisher;
    private final RoomService roomService;

    @Transactional
    public RoomResponse play(String code, Long userId) {
        Room room = requireCurrentTrack(roomService.findRoomByCode(code));
        room.setPlaying(true);
        roomRepository.save(room);
        RoomResponse response = roomService.toResponse(room);
        roomSyncPublisher.publish(response, SyncAction.PLAY, userId);
        return response;
    }

    @Transactional
    public RoomResponse pause(String code, Long userId) {
        Room room = roomService.findRoomByCode(code);
        room.setPlaying(false);
        roomRepository.save(room);
        RoomResponse response = roomService.toResponse(room);
        roomSyncPublisher.publish(response, SyncAction.PAUSE, userId);
        return response;
    }

    @Transactional
    public RoomResponse seek(String code, double currentTimeSeconds, Long userId) {
        Room room = roomService.findRoomByCode(code);
        if (currentTimeSeconds < 0) {
            throw new BadRequestException("Invalid seek position");
        }
        room.setCurrentTimeSeconds(currentTimeSeconds);
        roomRepository.save(room);
        RoomResponse response = roomService.toResponse(room);
        roomSyncPublisher.publish(response, SyncAction.SEEK, userId);
        return response;
    }

    @Transactional
    public RoomResponse next(String code, Long userId) {
        Room room = roomService.findRoomByCode(code);
        List<PlaylistItem> items = playlistItemRepository.findByRoomOrderByPositionAsc(room);
        if (items.isEmpty()) {
            throw new BadRequestException("Playlist is empty");
        }

        PlaylistItem current = room.getCurrentPlaylistItem();
        int currentIndex = indexOf(items, current);
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % items.size();
        applyTrackChange(room, items.get(nextIndex), true);
        RoomResponse response = roomService.toResponse(room);
        roomSyncPublisher.publish(response, SyncAction.NEXT, userId);
        return response;
    }

    @Transactional
    public RoomResponse prev(String code, Long userId) {
        Room room = roomService.findRoomByCode(code);
        List<PlaylistItem> items = playlistItemRepository.findByRoomOrderByPositionAsc(room);
        if (items.isEmpty()) {
            throw new BadRequestException("Playlist is empty");
        }

        PlaylistItem current = room.getCurrentPlaylistItem();
        int currentIndex = indexOf(items, current);
        int prevIndex = currentIndex <= 0 ? items.size() - 1 : currentIndex - 1;
        applyTrackChange(room, items.get(prevIndex), true);
        RoomResponse response = roomService.toResponse(room);
        roomSyncPublisher.publish(response, SyncAction.PREV, userId);
        return response;
    }

    @Transactional
    public RoomResponse onEnded(String code, Long userId) {
        Room room = roomService.findRoomByCode(code);
        List<PlaylistItem> items = playlistItemRepository.findByRoomOrderByPositionAsc(room);
        if (items.isEmpty()) {
            room.setPlaying(false);
            roomRepository.save(room);
            RoomResponse response = roomService.toResponse(room);
            roomSyncPublisher.publish(response, SyncAction.ENDED, userId);
            return response;
        }

        PlaylistItem current = room.getCurrentPlaylistItem();
        int currentIndex = indexOf(items, current);
        if (currentIndex < 0 || currentIndex >= items.size() - 1) {
            room.setPlaying(false);
            room.setCurrentTimeSeconds(0.0);
            roomRepository.save(room);
            RoomResponse response = roomService.toResponse(room);
            roomSyncPublisher.publish(response, SyncAction.ENDED, userId);
            return response;
        }

        applyTrackChange(room, items.get(currentIndex + 1), true);
        RoomResponse response = roomService.toResponse(room);
        roomSyncPublisher.publish(response, SyncAction.NEXT, userId);
        return response;
    }

    @Transactional
    public RoomResponse pushAnchor(String code, double currentTimeSeconds, Long userId) {
        Room room = roomService.findRoomByCode(code);
        if (!room.isPlaying()) {
            return roomService.toResponse(room);
        }
        room.setCurrentTimeSeconds(currentTimeSeconds);
        roomRepository.save(room);
        RoomResponse response = roomService.toResponse(room);
        roomSyncPublisher.publish(response, SyncAction.SEEK, userId);
        return response;
    }

    private Room requireCurrentTrack(Room room) {
        if (room.getCurrentPlaylistItem() == null) {
            throw new BadRequestException("No video in playlist");
        }
        return room;
    }

    private void applyTrackChange(Room room, PlaylistItem item, boolean playing) {
        room.setCurrentPlaylistItem(item);
        room.setCurrentTimeSeconds(0.0);
        room.setPlaying(playing);
        roomRepository.save(room);
    }

    private int indexOf(List<PlaylistItem> items, PlaylistItem current) {
        if (current == null) {
            return -1;
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(current.getId())) {
                return i;
            }
        }
        return -1;
    }
}
