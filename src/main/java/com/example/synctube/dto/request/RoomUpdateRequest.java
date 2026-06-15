package com.example.synctube.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomUpdateRequest {

    private String name;
    private String videoUrl;
    private Boolean playing;
    private Double currentTimeSeconds;
}
