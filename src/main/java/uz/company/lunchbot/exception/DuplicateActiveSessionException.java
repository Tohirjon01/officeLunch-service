package uz.company.lunchbot.exception;

public class DuplicateActiveSessionException extends RuntimeException {

    public DuplicateActiveSessionException(String message) {
        super(message);
    }
}
