package com.meetclone.meeting.repository;

import com.meetclone.meeting.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    List<Participant> findByMeetingId(UUID meetingId);
    Optional<Participant> findByMeetingIdAndUserId(UUID meetingId, UUID userId);
}
