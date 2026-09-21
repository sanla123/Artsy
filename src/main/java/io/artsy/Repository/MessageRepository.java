package io.artsy.Repository;

import io.artsy.Model.Commission;
import io.artsy.Model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByCommissionOrderBySentAtAsc(Commission commission);
}