package uz.company.lunchbot.util;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CallbackDataParser {

    public ParsedCallback parse(String callbackData) {
        List<String> parts = List.of(callbackData.split(":"));
        return new ParsedCallback(parts.getFirst(), parts.subList(1, parts.size()));
    }

    public record ParsedCallback(String action, List<String> args) {
    }
}
