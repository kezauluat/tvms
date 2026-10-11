package rw.ac.auca.officer.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.ac.auca.auth.domain.User;
import rw.ac.auca.base.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Officer extends BaseEntity {

    @NotBlank(message = "Badge number is required")
    @Column(name = "badge_number", nullable = false, unique = true)
    private String badgeNumber;

    @NotBlank(message = "Full name is required")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "station")
    private String station;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+2507|07)[2389]\\d{7}$", message = "Phone number must be a valid Rwandan number")
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    /** Login account used by the officer to record violations. */
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}
