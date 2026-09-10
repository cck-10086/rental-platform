package com.rental.controller;

import com.rental.common.Result;
import com.rental.entity.MoveOutChecklist;
import com.rental.service.MoveOutChecklistService;
import com.rental.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/move-out")
public class MoveOutChecklistController {

    public MoveOutChecklistController(MoveOutChecklistService moveOutChecklistService) {
        this.moveOutChecklistService = moveOutChecklistService;
    }

    private final MoveOutChecklistService moveOutChecklistService;

    @PostMapping("/create")
    public Result<?> create(@Valid @RequestBody MoveOutChecklist checklist) {
        moveOutChecklistService.create(checklist);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<?> update(@Valid @RequestBody MoveOutChecklist checklist) {
        moveOutChecklistService.update(checklist);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        moveOutChecklistService.delete(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<PageResult<MoveOutChecklist>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(moveOutChecklistService.listByUser(current, size));
    }

    @GetMapping("/detail/{id}")
    public Result<MoveOutChecklist> detail(@PathVariable Long id) {
        return Result.success(moveOutChecklistService.getDetail(id));
    }
}
