package uz.company.lunchbot.dto.response;

public record RestaurantVoteCountResponse(
        Long restaurantId,
        String restaurantName,
        long voteCount
) {
}
