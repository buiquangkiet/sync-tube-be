package com.example.synctube.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderPlaylistRequest {

    private List<Long> itemIds;
}
