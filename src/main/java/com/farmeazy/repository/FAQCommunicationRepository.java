package com.farmeazy.repository;

import com.farmeazy.entity.FAQCommunication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FAQCommunicationRepository extends JpaRepository<FAQCommunication, Long> {
	// Delete communications by referenced FAQ question id
	void deleteByFaqQuestionId(Long faqQuestionId);

	java.util.List<FAQCommunication> findByFaqQuestionIdOrderBySentAtAsc(Long faqQuestionId);

	boolean existsByBodyContaining(String text);
}
