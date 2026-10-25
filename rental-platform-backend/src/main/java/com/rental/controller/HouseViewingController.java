package com.rental.controller;

import com.rental.common.Result;
import com.rental.dto.HouseViewingCreateDTO;
import com.rental.dto.HouseViewingUpdateDTO;
import com.rental.entity.HouseViewing;
import com.rental.service.HouseViewingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/house-viewing")
public class HouseViewingController {

    public HouseViewingController(HouseViewingService houseViewingService) {
        this.houseViewingService = houseViewingService;
    }

    private final HouseViewingService houseViewingService;

    @PostMapping("/add")
    public Result<?> add(@Valid @RequestBody HouseViewingCreateDTO dto) {
        houseViewingService.add(toEntity(dto));
        return Result.success();
    }

    @PutMapping("/update")
    public Result<?> update(@Valid @RequestBody HouseViewingUpdateDTO dto) {
        HouseViewing houseViewing = toEntity(dto);
        houseViewing.setId(dto.getId());
        houseViewingService.update(houseViewing);
        return Result.success();
    }

    @GetMapping("/list/house/{houseId}")
    public Result<List<HouseViewing>> listByHouse(@PathVariable Long houseId) {
        return Result.success(houseViewingService.listByHouse(houseId));
    }

    @GetMapping("/list")
    public Result<List<HouseViewing>> list() {
        return Result.success(houseViewingService.listByUser());
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        houseViewingService.delete(id);
        return Result.success();
    }

    private HouseViewing toEntity(HouseViewingCreateDTO dto) {
        HouseViewing houseViewing = new HouseViewing();
        houseViewing.setHouseId(dto.getHouseId());
        houseViewing.setViewingTime(dto.getViewingTime());
        houseViewing.setImpression(dto.getImpression());
        houseViewing.setPros(dto.getPros());
        houseViewing.setCons(dto.getCons());
        houseViewing.setScore(dto.getScore());
        return houseViewing;
    }
}
