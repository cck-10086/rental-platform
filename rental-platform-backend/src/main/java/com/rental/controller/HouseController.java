package com.rental.controller;

import com.rental.common.Result;
import com.rental.entity.House;
import com.rental.service.HouseService;
import com.rental.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/house")
public class HouseController {

    public HouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    private final HouseService houseService;

    @PostMapping("/add")
    public Result<?> add(@Valid @RequestBody House house) {
        houseService.add(house);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<?> update(@Valid @RequestBody House house) {
        houseService.update(house);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        houseService.delete(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<PageResult<House>> list(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(houseService.listByUser(current, size));
    }

    @GetMapping("/detail/{id}")
    public Result<House> detail(@PathVariable Long id) {
        return Result.success(houseService.getDetail(id));
    }
}
