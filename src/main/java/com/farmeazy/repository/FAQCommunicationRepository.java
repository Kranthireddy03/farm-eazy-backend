package com.farmeazy.repository;

import com.farmeazy.entity.FAQCommunication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FAQCommunicationRepository extends JpaRepository<FAQCommunication, Long> {
	// Delete communications by referenced FAQ question id
	void deleteByFaqQuestionId(Long faqQuestionId);
}
