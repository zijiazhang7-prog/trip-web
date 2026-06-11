package com.trip.controller;

import com.trip.common.ApiResponse;
import com.trip.common.CommentTargetType;
import com.trip.dto.request.CommentCreateRequest;
import com.trip.dto.request.CommentPageQuery;
import com.trip.dto.request.DiaryCreateRequest;
import com.trip.dto.request.DiaryFulltextSearchQuery;
import com.trip.dto.request.DiaryListQuery;
import com.trip.dto.request.DiaryRatingRequest;
import com.trip.dto.request.DiaryTitleSearchQuery;
import com.trip.service.CommentService;
import com.trip.service.DiaryRatingService;
import com.trip.service.DiaryService;
import com.trip.vo.response.CommentVO;
import com.trip.vo.response.DiaryCreateResponse;
import com.trip.vo.response.DiaryRatingVO;
import com.trip.vo.response.DiaryVO;
import com.trip.vo.response.PageResultVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 旅游日记发布、列表、详情和目的地关联查询入口。
 */
@Validated
@RestController
public class DiaryController {

    private final DiaryService diaryService;
    private final DiaryRatingService diaryRatingService;
    private final CommentService commentService;

    public DiaryController(
            DiaryService diaryService,
            DiaryRatingService diaryRatingService,
            CommentService commentService) {
        this.diaryService = diaryService;
        this.diaryRatingService = diaryRatingService;
        this.commentService = commentService;
    }

    @PostMapping("/api/v1/diaries")
    public ApiResponse<DiaryCreateResponse> create(@Valid @RequestBody DiaryCreateRequest request) {
        return ApiResponse.success("created", diaryService.createDiary(request));
    }

    @GetMapping("/api/v1/diaries")
    public ApiResponse<PageResultVO<DiaryVO>> list(@Valid @ModelAttribute DiaryListQuery query) {
        return ApiResponse.success(diaryService.listDiaries(query));
    }

    @GetMapping("/api/v1/diaries/{id}")
    public ApiResponse<DiaryVO> detail(@PathVariable Long id) {
        return ApiResponse.success(diaryService.getDiaryDetail(id));
    }

    @GetMapping("/api/v1/destinations/{id}/diaries")
    public ApiResponse<PageResultVO<DiaryVO>> destinationDiaries(
            @PathVariable Long id,
            @Valid @ModelAttribute DiaryListQuery query) {
        return ApiResponse.success(diaryService.listDestinationDiaries(id, query));
    }

    @GetMapping("/api/v1/diaries/search/title")
    public ApiResponse<PageResultVO<DiaryVO>> searchByTitle(@Valid @ModelAttribute DiaryTitleSearchQuery query) {
        return ApiResponse.success(diaryService.searchByTitle(query));
    }

    @GetMapping("/api/v1/diaries/search/fulltext")
    public ApiResponse<PageResultVO<DiaryVO>> searchFulltext(@Valid @ModelAttribute DiaryFulltextSearchQuery query) {
        return ApiResponse.success(diaryService.searchFulltext(query));
    }

    @PostMapping("/api/v1/diaries/{id}/ratings")
    public ApiResponse<Boolean> rateDiary(
            @PathVariable Long id,
            @Valid @RequestBody DiaryRatingRequest request) {
        return ApiResponse.success(diaryRatingService.rateDiary(id, request));
    }

    @GetMapping("/api/v1/diaries/{id}/ratings/me")
    public ApiResponse<DiaryRatingVO> getMyRating(@PathVariable Long id) {
        return ApiResponse.success(diaryRatingService.getMyRating(id));
    }

    @GetMapping("/api/v1/diaries/{id}/comments")
    public ApiResponse<PageResultVO<CommentVO>> comments(
            @PathVariable Long id,
            @Valid @ModelAttribute CommentPageQuery query) {
        return ApiResponse.success(commentService.listComments(CommentTargetType.DIARY, id, query));
    }

    @PostMapping("/api/v1/diaries/{id}/comments")
    public ApiResponse<CommentVO> createComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(
                "created",
                commentService.createComment(CommentTargetType.DIARY, id, request));
    }
}
