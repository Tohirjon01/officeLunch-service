package uz.company.lunchbot.dto.response;

import uz.company.lunchbot.entity.LunchUser;

public record RegistrationResultResponse(LunchUser user, boolean created) {
}
