package com.rental.controller;

import com.rental.common.Result;
import com.rental.entity.RepairRequest;
import com.rental.service.RepairRequestService;
import com.rental.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repair")
public class RepairRequestController {

    public RepairRequestController(RepairRequestService repairRequestService) {
        this.repairRequestService = repairRequestService;
    }

    private final RepairRequestService repairRequestService;

    @PostMapping("/add")
    public Result<?> add(@Valid @RequestBody RepairRequest repairRequest) {
        repairRequestService.add(repairRequest);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<?> update(@Valid @RequestBody RepairRequest repairRequest) {
        repairRequestService.update(repairRequest);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        repairRequestService.delete(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<PageResult<RepairRequest>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(repairRequestService.listByUser(current, size));
    }
}
