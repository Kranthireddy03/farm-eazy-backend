package com.farmeazy.controller;

import com.farmeazy.dto.ApprovePayoutBatchRequest;
import com.farmeazy.dto.PayoutBatchListDto;
import com.farmeazy.dto.PayoutDetailDto;
import com.farmeazy.entity.BatchPayout;
import com.farmeazy.entity.PayoutBatch;
import com.farmeazy.exception.UnauthorizedException;
import com.farmeazy.service.PayoutBatchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/payouts")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "https://farm-eazy-backend.onrender.com"})
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminPayoutController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPayoutController.class);

    @Autowired
    private PayoutBatchService payoutBatchService;

    @GetMapping("/batches")
    public ResponseEntity<Map<String, Object>> listBatches(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PayoutBatch.BatchStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                parsedStatus = PayoutBatch.BatchStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
            }
        }

        Page<PayoutBatch> batches = payoutBatchService.getBatches(
                parsedStatus,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<PayoutBatchListDto> content = batches.getContent().stream()
                .map(this::toBatchListDto)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalElements", batches.getTotalElements());
        response.put("totalPages", batches.getTotalPages());
        response.put("currentPage", batches.getNumber());
        response.put("pageSize", batches.getSize());
        response.put("hasNext", batches.hasNext());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/batches/{batchId}/approve")
    public ResponseEntity<Map<String, Object>> approveBatch(
            @PathVariable Long batchId,
            @Valid @RequestBody ApprovePayoutBatchRequest request) {

        PayoutBatch batch = payoutBatchService.getBatchById(batchId);
        if (batch.getStatus() != PayoutBatch.BatchStatus.CREATED) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Only CREATED batches can be approved. Current status: " + batch.getStatus()));
        }

        payoutBatchService.approveBatchWithOtp(batchId, request.getOtp());

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            payoutBatchService.updateBatchNotes(batchId, request.getNotes().trim());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Batch approved successfully");
        response.put("batch", toBatchListDto(payoutBatchService.getBatchById(batchId)));

        logger.info("ADMIN_BATCH_APPROVED: batchId={}", batchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batches/{batchId}/payouts")
    public ResponseEntity<Map<String, Object>> getBatchPayouts(@PathVariable Long batchId) {
        PayoutBatch batch = payoutBatchService.getBatchById(batchId);
        List<BatchPayout> payouts = payoutBatchService.getBatchPayouts(batchId);

        List<PayoutDetailDto> payoutDtos = payouts.stream()
                .map(this::toPayoutDetailDto)
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", payouts.size());
        summary.put("completed", payouts.stream().filter(p -> p.getStatus() == BatchPayout.PayoutStatus.COMPLETED).count());
        summary.put("failed", payouts.stream().filter(p -> p.getStatus() == BatchPayout.PayoutStatus.FAILED).count());
        summary.put("totalAmount", batch.getTotalAmount());

        Map<String, Object> response = new HashMap<>();
        response.put("batchId", batch.getId());
        response.put("batchStatus", batch.getStatus().name());
        response.put("payouts", payoutDtos);
        response.put("summary", summary);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/batches/{batchId}/export-csv")
    public ResponseEntity<String> exportCsv(@PathVariable Long batchId) {
        try {
            String csv = payoutBatchService.generatePayoutCsv(batchId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payout_batch_" + batchId + ".csv")
                    .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                    .body(csv);
        } catch (UnauthorizedException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    private PayoutBatchListDto toBatchListDto(PayoutBatch batch) {
        return new PayoutBatchListDto(
                batch.getId(),
                batch.getBatchDate(),
                batch.getTotalVendors(),
                batch.getTotalAmount(),
                batch.getStatus().name(),
                batch.getCreatedByUser() != null ? batch.getCreatedByUser().getEmail() : null,
                batch.getCreatedAt(),
                batch.getApprovedByUser() != null ? batch.getApprovedByUser().getEmail() : null,
                batch.getApprovedAt());
    }

    private PayoutDetailDto toPayoutDetailDto(BatchPayout payout) {
        PayoutDetailDto dto = new PayoutDetailDto();
        dto.setId(payout.getId());
        dto.setBatchId(payout.getBatch() != null ? payout.getBatch().getId() : null);
        dto.setVendorId(payout.getVendor() != null ? payout.getVendor().getId() : null);
        dto.setVendorName(payout.getVendor() != null ? payout.getVendor().getUsername() : null);
        dto.setVendorEmail(payout.getVendor() != null ? payout.getVendor().getEmail() : null);
        dto.setAmount(payout.getAmount());
        dto.setStatus(payout.getStatus().name());
        dto.setTransactionReference(payout.getTransactionReference());
        dto.setRazorpayPayoutId(payout.getRazorpayPayoutId());
        dto.setFailureReason(payout.getFailureReason());
        dto.setRetryCount(payout.getRetryCount());
        dto.setMaxRetries(payout.getMaxRetries());

        if (payout.getBankDetail() != null) {
            dto.setAccountNumberMasked(payout.getBankDetail().getMaskedAccountNumber());
            dto.setIfscCode(payout.getBankDetail().getIfscCode());
            dto.setAccountHolderName(payout.getBankDetail().getAccountHolderName());
        }

        return dto;
    }
}
