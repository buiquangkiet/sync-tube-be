package com.example.synctube.controller;

import com.example.synctube.dto.request.SeekRequest;
import com.example.synctube.dto.response.RoomResponse;
import com.example.synctube.security.SecurityUtils;
import com.example.synctube.service.PlaybackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/code/{code}/playback")
@RequiredArgsConstructor
public class PlaybackController {

    private final PlaybackService playbackService;

    @PostMapping("/play")
    public RoomResponse play(@PathVariable String code) {
        return playbackService.play(code, SecurityUtils.requireUserId());
    }

    @PostMapping("/pause")
    public RoomResponse pause(@PathVariable String code) {
        return playbackService.pause(code, SecurityUtils.requireUserId());
    }

    @PostMapping("/seek")
    public RoomResponse seek(@PathVariable String code, @RequestBody SeekRequest request) {
        double time = request.getCurrentTimeSeconds() != null ? request.getCurrentTimeSeconds() : 0.0;
        return playbackService.seek(code, time, SecurityUtils.requireUserId());
    }

    @PostMapping("/next")
    public RoomResponse next(@PathVariable String code) {
        return playbackService.next(code, SecurityUtils.requireUserId());
    }

    @PostMapping("/prev")
    public RoomResponse prev(@PathVariable String code) {
        return playbackService.prev(code, SecurityUtils.requireUserId());
    }

    @PostMapping("/ended")
    public RoomResponse ended(@PathVariable String code) {
        return playbackService.onEnded(code, SecurityUtils.requireUserId());
    }

    @PostMapping("/anchor")
    public RoomResponse anchor(@PathVariable String code, @RequestBody SeekRequest request) {
        double time = request.getCurrentTimeSeconds() != null ? request.getCurrentTimeSeconds() : 0.0;
        return playbackService.pushAnchor(code, time, SecurityUtils.requireUserId());
    }
}
