package uz.company.lunchbot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "restaurant_votes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_restaurant_votes_session_user", columnNames = {"vote_session_id", "user_id"})
})
public class RestaurantVote extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "vote_session_id", nullable = false)
    private RestaurantVoteSession voteSession;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private LunchUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;
}
