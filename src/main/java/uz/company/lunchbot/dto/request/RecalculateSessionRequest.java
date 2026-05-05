package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.NotNull;
import uz.company.lunchbot.enums.RecalculationMode;

public record RecalculateSessionRequest(
        @NotNull RecalculationMode mode,
        Boolean forceConfirmed
) {
}
