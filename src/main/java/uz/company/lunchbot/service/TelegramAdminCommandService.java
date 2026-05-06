package uz.company.lunchbot.service;

import uz.company.lunchbot.entity.LunchUser;
public interface TelegramAdminCommandService {

    boolean supports(String text);

    AdminCommandResponse handle(LunchUser actor, String text);

    AdminCommandResponse buildMenuPage(LunchUser actor, Long restaurantId, int page);

    AdminCommandResponse buildOrderControl(LunchUser actor, Long sessionId);

    AdminCommandResponse buildVoteControl(LunchUser actor, Long voteSessionId);

    record AdminCommandResponse(String text, Object keyboard) {
    }
}
