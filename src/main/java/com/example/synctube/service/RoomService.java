package com.example.synctube.service;

import com.example.synctube.dto.request.RoomRequest;
import com.example.synctube.dto.response.RoomResponse;
import com.example.synctube.entity.PlaylistItem;
import com.example.synctube.entity.Room;
import com.example.synctube.entity.User;
import com.example.synctube.exception.BadRequestException;
import com.example.synctube.exception.ResourceNotFoundException;
import com.example.synctube.repository.PlaylistItemRepository;
import com.example.synctube.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final RoomRepository roomRepository;
    private final UserService userService;
    private final PlaylistItemRepository playlistItemRepository;
    private final SecureRandom random = new SecureRandom();

    public RoomResponse getRoomByCode(String code) {
        Room room = findRoomByCode(code);
        return RoomResponse.from(room, playlistItemRepository);
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request, Long hostUserId) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Room name is required");
        }

        User host = userService.findUserById(hostUserId);

        if (request.getVideoUrl() != null && !request.getVideoUrl().isBlank()) {
            validateYouTubeUrl(request.getVideoUrl());
        }

        Room room = Room.builder()
                .name(request.getName())
                .code(generateUniqueCode())
                .host(host)
                .build();

        room = roomRepository.save(room);

        if (request.getVideoUrl() != null && !request.getVideoUrl().isBlank()) {
            PlaylistItem item = playlistItemRepository.save(PlaylistItem.builder()
                    .room(room)
                    .videoUrl(request.getVideoUrl().trim())
                    .position(0)
                    .addedBy(host)
                    .build());
            room.setCurrentPlaylistItem(item);
            room = roomRepository.save(room);
        }

        return RoomResponse.from(room, playlistItemRepository);
    }

    @Transactional
    public void deleteRoom(String code, Long userId) {
        Room room = findRoomByCode(code);
        if (!room.getHost().getId().equals(userId)) {
            throw new BadRequestException("Only the host can delete this room");
        }
        roomRepository.delete(room);
    }

    Room findRoomByCode(String code) {
        return roomRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with code: " + code));
    }

    RoomResponse toResponse(Room room) {
        return RoomResponse.from(room, playlistItemRepository);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = randomCode();
            if (!roomRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new BadRequestException("Unable to generate unique room code");
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    static void validateYouTubeUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            return;
        }
        String lower = videoUrl.toLowerCase();
        if (!lower.contains("youtube.com") && !lower.contains("youtu.be")) {
            throw new BadRequestException("Invalid YouTube URL");
        }
    }
}
