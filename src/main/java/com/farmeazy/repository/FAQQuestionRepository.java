package com.farmeazy.repository;

import com.farmeazy.entity.FAQQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

import java.time.OffsetDateTime;

public interface FAQQuestionRepository extends JpaRepository<FAQQuestion, Long>, JpaSpecificationExecutor<FAQQuestion> {
	List<FAQQuestion> findByAddedToFAQTrue();

	List<FAQQuestion> findByEmailOrderBySubmittedAtDesc(String email);
	List<FAQQuestion> findByUserIdOrderBySubmittedAtDesc(String userId);
	List<FAQQuestion> findByEmailOrUserIdOrderBySubmittedAtDesc(String email, String userId);
	boolean existsByQuestionContaining(String text);

	// find unanswered questions that have not yet been marked read by an admin
	List<FAQQuestion> findByAnswerIsNullAndNotificationReadFalse();
	// notify on all pages with unread notification flag
	List<FAQQuestion> findByNotificationReadFalse();
	// fallback when DB schema doesn't have notificationRead column
	List<FAQQuestion> findByAnswerIsNull();

	long countByAnswerIsNull();

	List<FAQQuestion> findTop10BySubmittedAtAfterOrderBySubmittedAtDesc(OffsetDateTime start);
	List<FAQQuestion> findTop10ByAnswerIsNullOrderBySubmittedAtDesc();
	List<FAQQuestion> findTop10BySubmittedAtAfterAndAnswerIsNullOrderBySubmittedAtDesc(OffsetDateTime start);
}
