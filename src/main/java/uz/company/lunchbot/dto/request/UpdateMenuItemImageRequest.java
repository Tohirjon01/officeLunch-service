package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateMenuItemImageRequest(
        @Size(max = 1000) String imageUrl,
        @Size(max = 512) String telegramImageFileId
) {
}
