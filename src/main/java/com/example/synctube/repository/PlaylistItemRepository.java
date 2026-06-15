package com.example.synctube.repository;

import com.example.synctube.entity.PlaylistItem;
import com.example.synctube.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistItemRepository extends JpaRepository<PlaylistItem, Long> {

    List<PlaylistItem> findByRoomOrderByPositionAsc(Room room);

    Optional<PlaylistItem> findByIdAndRoom(Long id, Room room);

    int countByRoom(Room room);
}
