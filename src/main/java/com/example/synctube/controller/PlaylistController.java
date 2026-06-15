package com.example.synctube.controller;

import com.example.synctube.dto.request.AddPlaylistItemRequest;
import com.example.synctube.dto.request.ReorderPlaylistRequest;
import com.example.synctube.dto.response.PlaylistItemResponse;
import com.example.synctube.dto.response.RoomResponse;
import com.example.synctube.security.SecurityUtils;
import com.example.synctube.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/code/{code}/playlist")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @GetMapping
    public List<PlaylistItemResponse> getPlaylist(@PathVariable String code) {
        return playlistService.getPlaylist(code);
    }

    @PostMapping("/items")
    public RoomResponse addItem(
            @PathVariable String code,
            @RequestBody AddPlaylistItemRequest request) {
        return playlistService.addItem(code, request, SecurityUtils.requireUserId());
    }

    @DeleteMapping("/items/{itemId}")
    public RoomResponse removeItem(
            @PathVariable String code,
            @PathVariable Long itemId) {
        return playlistService.removeItem(code, itemId, SecurityUtils.requireUserId());
    }

    @PutMapping("/reorder")
    public RoomResponse reorder(
            @PathVariable String code,
            @RequestBody ReorderPlaylistRequest request) {
        return playlistService.reorder(code, request, SecurityUtils.requireUserId());
    }
}
