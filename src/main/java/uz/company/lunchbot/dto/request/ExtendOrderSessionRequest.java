package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExtendOrderSessionRequest(@NotNull @Min(1) Integer minutes) {
}
