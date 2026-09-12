package com.meetclone.recording.repository;

import com.meetclone.recording.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordingRepository extends JpaRepository<Recording, UUID> {
    Optional<Recording> findByMeetingId(UUID meetingId);
}
