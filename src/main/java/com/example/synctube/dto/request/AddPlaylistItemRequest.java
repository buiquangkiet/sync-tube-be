package com.example.synctube.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddPlaylistItemRequest {

    private String videoUrl;
    private String title;
}
