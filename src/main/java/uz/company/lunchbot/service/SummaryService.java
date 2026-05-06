package uz.company.lunchbot.service;

import uz.company.lunchbot.dto.response.RestaurantOrderTextResponse;
import uz.company.lunchbot.dto.response.SessionSummaryResponse;

public interface SummaryService {

    SessionSummaryResponse buildSummary(Long sessionId);

    RestaurantOrderTextResponse buildRestaurantTextResponse(Long sessionId);

    String buildGroupSummaryText(Long sessionId);

    String buildRestaurantText(Long sessionId);
}
