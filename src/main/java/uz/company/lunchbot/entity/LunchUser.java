package uz.company.lunchbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uz.company.lunchbot.enums.UserLanguage;
import uz.company.lunchbot.enums.UserRole;
import uz.company.lunchbot.enums.UserStatus;

@Getter
@Setter
@Entity
@Table(name = "users")
public class LunchUser extends BaseEntity {

    @Column(name = "telegram_user_id", nullable = false, unique = true)
    private Long telegramUserId;

    @Column(name = "username")
    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "private_chat_id")
    private Long privateChatId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 8)
    private UserLanguage language;

    @Column(name = "bot_blocked", nullable = false)
    private boolean botBlocked;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    public String getDisplayName() {
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (username != null && !username.isBlank()) {
            return "@" + username;
        }
        return "User-" + telegramUserId;
    }
}
