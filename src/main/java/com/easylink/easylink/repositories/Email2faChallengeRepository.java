package com.easylink.easylink.repositories;

import com.easylink.easylink.entities.Email2faChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface Email2faChallengeRepository extends JpaRepository<Email2faChallenge, UUID> {
}
