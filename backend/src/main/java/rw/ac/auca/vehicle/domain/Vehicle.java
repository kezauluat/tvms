package rw.ac.auca.vehicle.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.ac.auca.base.BaseEntity;
import rw.ac.auca.driver.domain.Driver;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = @Index(name = "ix_vehicle_owner", columnList = "driver_id"))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Vehicle extends BaseEntity {

    @NotBlank(message = "Plate number is required")
    @Column(name = "plate_number", nullable = false, unique = true)
    private String plateNumber;

    @NotBlank(message = "Make is required")
    @Column(name = "make", nullable = false)
    private String make;

    @NotBlank(message = "Model is required")
    @Column(name = "model", nullable = false)
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 1980, message = "Year must be 1980 or later")
    @Column(name = "manufacture_year", nullable = false)
    private Integer year;

    @NotNull(message = "Owner (driver) is required")
    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver owner;
}
