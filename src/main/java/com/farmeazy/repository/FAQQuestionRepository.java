package com.farmeazy.repository;

import com.farmeazy.entity.FAQQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FAQQuestionRepository extends JpaRepository<FAQQuestion, Long> {
	List<FAQQuestion> findByAddedToFAQTrue();

	// find unanswered questions that have not yet been marked read by an admin
	List<FAQQuestion> findByAnswerIsNullAndNotificationReadFalse();
    
	// fallback when DB schema doesn't have notificationRead column
	List<FAQQuestion> findByAnswerIsNull();
}
