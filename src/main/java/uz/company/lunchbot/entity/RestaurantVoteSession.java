package uz.company.lunchbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import uz.company.lunchbot.enums.RestaurantVoteSessionStatus;

@Getter
@Setter
@Entity
@Table(name = "restaurant_vote_sessions")
public class RestaurantVoteSession extends BaseEntity {

    @Column(name = "vote_date", nullable = false)
    private LocalDate voteDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RestaurantVoteSessionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    @ManyToOne
    @JoinColumn(name = "winner_restaurant_id")
    private Restaurant winnerRestaurant;

    @Column(name = "group_message_id")
    private Long groupMessageId;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private LunchUser createdBy;
}
