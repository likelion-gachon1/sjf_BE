package com.sjf.portal.controller;

import com.sjf.portal.dto.WorldRecommendRequest;
import com.sjf.portal.dto.WorldRecommendResponse;
import com.sjf.portal.service.WorldRecommendService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worlds")
public class WorldController {

    private final WorldRecommendService worldRecommendService;

    public WorldController(
            WorldRecommendService worldRecommendService
    ) {
        this.worldRecommendService = worldRecommendService;
    }

    @PostMapping("/recommend")
    public WorldRecommendResponse recommend(
            @RequestBody WorldRecommendRequest request
    ) {
        return worldRecommendService.recommend(request);
    }
}