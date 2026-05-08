package com.trip.service;

import com.trip.dto.request.FoodRecommendQuery;
import com.trip.dto.request.FoodSearchQuery;
import com.trip.vo.response.FoodVO;
import com.trip.vo.response.PageResultVO;

/**
 * 美食模块服务，负责基础查询、筛选和排序。
 */
public interface FoodService {

    PageResultVO<FoodVO> recommendFoods(FoodRecommendQuery query);

    PageResultVO<FoodVO> searchFoods(FoodSearchQuery query);
}
