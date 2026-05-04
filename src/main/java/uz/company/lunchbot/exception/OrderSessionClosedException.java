package uz.company.lunchbot.exception;

public class OrderSessionClosedException extends RuntimeException {

    public OrderSessionClosedException(String message) {
        super(message);
    }
}
