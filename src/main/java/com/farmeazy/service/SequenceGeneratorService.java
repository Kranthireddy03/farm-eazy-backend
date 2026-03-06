package com.farmeazy.service;

import com.farmeazy.entity.IdSequence;
import com.farmeazy.repository.SequenceGeneratorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SEQUENCE GENERATOR SERVICE
 * 
 * PURPOSE: Generates 5-digit sequential IDs for various entities.
 * Provides unique, human-readable IDs starting from 10000.
 * 
 * SUPPORTED SEQUENCES:
 * - USER_ID: USR10000, USR10001, ...
 * - ORDER_ID: ORD10000, ORD10001, ...
 * - SERVICE_REQUEST_ID: SRV10000, SRV10001, ...
 * - PAYMENT_ID: PAY10000, PAY10001, ...
 * - PAYOUT_ID: PYT10000, PYT10001, ...
 * - BANK_VERIFICATION_ID: BNK10000, BNK10001, ...
 * 
 * THREAD SAFETY:
 * - Uses pessimistic locking for concurrent access
 * - Wrapped in transaction for atomicity
 */
@Service
public class SequenceGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(SequenceGeneratorService.class);

    @Autowired
    private SequenceGeneratorRepository sequenceRepository;

    // Sequence name constants
    public static final String USER_SEQUENCE = "USER_ID";
    public static final String ORDER_SEQUENCE = "ORDER_ID";
    public static final String SERVICE_REQUEST_SEQUENCE = "SERVICE_REQUEST_ID";
    public static final String PAYMENT_SEQUENCE = "PAYMENT_ID";
    public static final String PAYOUT_SEQUENCE = "PAYOUT_ID";
    public static final String BANK_VERIFICATION_SEQUENCE = "BANK_VERIFICATION_ID";

    /**
     * Gets the next display ID for users.
     * Format: USR10000, USR10001, etc.
     */
    @Transactional
    public String getNextUserDisplayId() {
        return getNextDisplayId(USER_SEQUENCE);
    }

    /**
     * Gets the next display ID for orders.
     * Format: ORD10000, ORD10001, etc.
     */
    @Transactional
    public String getNextOrderDisplayId() {
        return getNextDisplayId(ORDER_SEQUENCE);
    }

    /**
     * Gets the next display ID for service requests.
     * Format: SRV10000, SRV10001, etc.
     */
    @Transactional
    public String getNextServiceRequestId() {
        return getNextDisplayId(SERVICE_REQUEST_SEQUENCE);
    }

    /**
     * Gets the next display ID for payments.
     * Format: PAY10000, PAY10001, etc.
     */
    @Transactional
    public String getNextPaymentDisplayId() {
        return getNextDisplayId(PAYMENT_SEQUENCE);
    }

    /**
     * Gets the next display ID for payouts.
     * Format: PYT10000, PYT10001, etc.
     */
    @Transactional
    public String getNextPayoutDisplayId() {
        return getNextDisplayId(PAYOUT_SEQUENCE);
    }

    /**
     * Gets the next display ID for bank verification.
     * Format: BNK10000, BNK10001, etc.
     */
    @Transactional
    public String getNextBankVerificationId() {
        return getNextDisplayId(BANK_VERIFICATION_SEQUENCE);
    }

    /**
     * Gets the next display ID for any sequence.
     * Uses pessimistic locking for thread safety.
     */
    @Transactional
    public String getNextDisplayId(String sequenceName) {
        // Get sequence with lock to prevent concurrent access
        IdSequence sequence = sequenceRepository.findBySequenceNameWithLock(sequenceName)
                .orElseGet(() -> createDefaultSequence(sequenceName));
        
        String displayId = sequence.getNextDisplayId();
        sequenceRepository.save(sequence);
        
        logger.debug("SEQUENCE_GENERATED: name={}, displayId={}", sequenceName, displayId);
        
        return displayId;
    }

    /**
     * Creates a default sequence if not exists.
     */
    private IdSequence createDefaultSequence(String sequenceName) {
        logger.info("SEQUENCE_CREATE: Creating new sequence {}", sequenceName);
        
        IdSequence sequence = new IdSequence();
        sequence.setSequenceName(sequenceName);
        sequence.setCurrentValue(10000L);
        sequence.setMinValue(10000L);
        sequence.setMaxValue(99999L);
        sequence.setIncrementBy(1);
        sequence.setIsCyclic(false);
        
        // Set prefix based on sequence name
        switch (sequenceName) {
            case USER_SEQUENCE -> sequence.setPrefix("USR");
            case ORDER_SEQUENCE -> sequence.setPrefix("ORD");
            case SERVICE_REQUEST_SEQUENCE -> sequence.setPrefix("SRV");
            case PAYMENT_SEQUENCE -> sequence.setPrefix("PAY");
            case PAYOUT_SEQUENCE -> sequence.setPrefix("PYT");
            case BANK_VERIFICATION_SEQUENCE -> sequence.setPrefix("BNK");
            default -> sequence.setPrefix("");
        }
        
        return sequenceRepository.save(sequence);
    }

    /**
     * Gets the current value without incrementing (for display purposes).
     */
    @Transactional(readOnly = true)
    public long getCurrentValue(String sequenceName) {
        return sequenceRepository.findBySequenceName(sequenceName)
                .map(IdSequence::getCurrentValue)
                .orElse(10000L);
    }

    /**
     * Initializes all sequences (call on application startup if needed).
     */
    @Transactional
    public void initializeSequences() {
        String[] sequences = {
                USER_SEQUENCE, ORDER_SEQUENCE, SERVICE_REQUEST_SEQUENCE,
                PAYMENT_SEQUENCE, PAYOUT_SEQUENCE, BANK_VERIFICATION_SEQUENCE
        };
        
        for (String seq : sequences) {
            if (!sequenceRepository.existsBySequenceName(seq)) {
                createDefaultSequence(seq);
                logger.info("SEQUENCE_INITIALIZED: {}", seq);
            }
        }
    }
}
