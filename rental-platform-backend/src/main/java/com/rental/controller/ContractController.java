package com.rental.controller;

import com.rental.common.Result;
import com.rental.entity.Contract;
import com.rental.entity.ReviewFeedback;
import com.rental.entity.ReviewRecord;
import com.rental.service.ContractService;
import com.rental.service.ReviewFeedbackService;
import com.rental.common.PageResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contract")
public class ContractController {

    public ContractController(ContractService contractService, ReviewFeedbackService reviewFeedbackService) {
        this.contractService = contractService;
        this.reviewFeedbackService = reviewFeedbackService;
    }

    private final ContractService contractService;

    private final ReviewFeedbackService reviewFeedbackService;

    @PostMapping("/upload")
    public Result<Contract> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "title", required = false) String title) {
        return Result.success(contractService.upload(file, title));
    }

    @PostMapping("/review/{contractId}")
    public Result<Map<String, Object>> review(@PathVariable Long contractId) {
        return Result.success(contractService.review(contractId));
    }

    @GetMapping("/list")
    public Result<PageResult<Contract>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(contractService.listByUser(keyword, current, size));
    }

    @GetMapping("/detail/{contractId}")
    public Result<Contract> detail(@PathVariable Long contractId) {
        return Result.success(contractService.getDetail(contractId));
    }

    @GetMapping("/records/{contractId}")
    public Result<List<ReviewRecord>> records(@PathVariable Long contractId) {
        return Result.success(contractService.getReviewRecords(contractId));
    }

    /** 提交或更新当前用户对某条审查记录的 有用/误报 反馈 */
    @PostMapping("/feedback/{recordId}")
    public Result<?> submitFeedback(@PathVariable Long recordId, @RequestBody Map<String, String> body) {
        reviewFeedbackService.submit(recordId, body.get("feedback"));
        return Result.success();
    }

    /** 查询当前用户对某合同全部条款的反馈（报告页回显按钮状态） */
    @GetMapping("/feedback")
    public Result<List<ReviewFeedback>> listFeedback(@RequestParam Long contractId) {
        return Result.success(reviewFeedbackService.listMyFeedback(contractId));
    }

    @GetMapping("/report/{contractId}")
    public ResponseEntity<byte[]> exportReport(@PathVariable Long contractId) {
        byte[] data = contractService.exportReviewReport(contractId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=review-report-" + contractId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/file/{contractId}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long contractId,
            @RequestParam(defaultValue = "false") boolean download) {
        ContractService.ContractFilePayload payload = contractService.loadContractFile(contractId);
        ContentDisposition disposition = download
                ? ContentDisposition.attachment().filename(payload.getFileName()).build()
                : ContentDisposition.inline().filename(payload.getFileName()).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(payload.getContentType()))
                .body(payload.getBytes());
    }

    @DeleteMapping("/{contractId}")
    public Result<?> delete(@PathVariable Long contractId) {
        contractService.delete(contractId);
        return Result.success();
    }
}
