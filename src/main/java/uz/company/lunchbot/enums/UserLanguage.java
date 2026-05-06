package uz.company.lunchbot.enums;

public enum UserLanguage {
    UZ,
    RU;

    public static UserLanguage fromCode(String code) {
        if (code == null) {
            return UZ;
        }
        return switch (code.trim().toUpperCase()) {
            case "RU" -> RU;
            default -> UZ;
        };
    }
}
