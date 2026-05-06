package uz.company.lunchbot.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateMenuItemCategoryRequest(
        @Size(max = 128) String category
) {
}
