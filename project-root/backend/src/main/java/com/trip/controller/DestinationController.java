package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.common.CommentTargetType;
import com.trip.dto.request.CommentCreateRequest;
import com.trip.dto.request.CommentPageQuery;
import com.trip.dto.request.DestinationPlacesQuery;
import com.trip.dto.request.DestinationRecommendQuery;
import com.trip.dto.request.DestinationSearchQuery;
import com.trip.service.CommentService;
import com.trip.service.RecommendService;
import com.trip.vo.response.CommentVO;
import com.trip.vo.response.DestinationVO;
import com.trip.vo.response.PageResultVO;
import com.trip.vo.response.PlaceVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 目的地推荐、搜索和详情入口。
 */
@Validated
@RestController
@RequestMapping("/api/v1/destinations")
public class DestinationController {

    private final RecommendService recommendService;
    private final CommentService commentService;

    public DestinationController(RecommendService recommendService, CommentService commentService) {
        this.recommendService = recommendService;
        this.commentService = commentService;
    }

    @GetMapping("/recommend")
    public ApiResponse<PageResultVO<DestinationVO>> recommend(
            @Valid @ModelAttribute DestinationRecommendQuery query) {
        return ApiResponse.success(recommendService.recommendDestinations(query));
    }

    @GetMapping("/search")
    public ApiResponse<PageResultVO<DestinationVO>> search(
            @Valid @ModelAttribute DestinationSearchQuery query) {
        return ApiResponse.success(recommendService.searchDestinations(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<DestinationVO> detail(@PathVariable Long id) {
        return ApiResponse.success(recommendService.getDestinationDetail(id));
    }

    @GetMapping("/{id}/places")
    public ApiResponse<List<PlaceVO>> places(
            @PathVariable Long id,
            @Valid @ModelAttribute DestinationPlacesQuery query) {
        return ApiResponse.success(recommendService.listDestinationPlaces(id, query));
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<PageResultVO<CommentVO>> comments(
            @PathVariable Long id,
            @Valid @ModelAttribute CommentPageQuery query) {
        return ApiResponse.success(
                commentService.listComments(CommentTargetType.DESTINATION, id, query));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<CommentVO> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(
                "created",
                commentService.createComment(CommentTargetType.DESTINATION, id, request));
    }
}
