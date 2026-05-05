package uz.company.lunchbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "restaurants")
public class Restaurant extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "container_enabled", nullable = false)
    private boolean containerEnabled;

    @Column(name = "default_container_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal defaultContainerPrice;

    @Column(name = "delivery_enabled", nullable = false)
    private boolean deliveryEnabled;

    @Column(name = "default_delivery_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal defaultDeliveryPrice;
}
