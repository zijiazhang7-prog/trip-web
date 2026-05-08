package com.trip.service;

import com.trip.dto.request.UserPreferenceRequest;
import com.trip.vo.response.UserPreferenceVO;

public interface UserPreferenceService {

    UserPreferenceVO getCurrentPreference();

    UserPreferenceVO saveCurrentPreference(UserPreferenceRequest request);
}
