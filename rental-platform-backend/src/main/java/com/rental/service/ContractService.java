package com.rental.service;

import com.rental.entity.Contract;
import com.rental.entity.ReviewRecord;
import com.rental.common.PageResult;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface ContractService {
    Contract upload(MultipartFile file, String title);
    Map<String, Object> review(Long contractId);
    PageResult<Contract> listByUser(String keyword, long current, long size);
    Contract getDetail(Long contractId);
    List<ReviewRecord> getReviewRecords(Long contractId);
    void delete(Long contractId);
    byte[] exportReviewReport(Long contractId);
    ContractFilePayload loadContractFile(Long contractId);

    /**
     * 合同文件下载载荷
     */
    class ContractFilePayload {
        private final byte[] bytes;
        private final String fileName;
        private final String contentType;

        public ContractFilePayload(byte[] bytes, String fileName, String contentType) {
            this.bytes = bytes;
            this.fileName = fileName;
            this.contentType = contentType;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
