package com.example.synctube.controller;

import com.example.synctube.dto.request.RoomRequest;
import com.example.synctube.dto.response.RoomResponse;
import com.example.synctube.security.SecurityUtils;
import com.example.synctube.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/code/{code}")
    public RoomResponse getRoomByCode(@PathVariable String code) {
        return roomService.getRoomByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@RequestBody RoomRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return roomService.createRoom(request, userId);
    }

    @DeleteMapping("/code/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable String code) {
        roomService.deleteRoom(code, SecurityUtils.requireUserId());
    }
}
