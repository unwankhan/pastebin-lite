package com.pastebinlite.repository;

import com.pastebinlite.model.Paste;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasteRepository extends MongoRepository<Paste, String> {
    Optional<Paste> findByPasteId(String pasteId);

    @Query("{ 'expiresAt': { $lt: ?0 }, 'isActive': true }")
    List<Paste> findExpiredPastes(Instant now);

}