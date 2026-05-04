package uz.company.lunchbot.exception;

public class UnauthorizedTelegramUserException extends RuntimeException {

    public UnauthorizedTelegramUserException(String message) {
        super(message);
    }
}
