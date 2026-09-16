package com.deepblue.rescue.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "rescue_cases")
public class RescueCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_code", nullable = false, unique = true)
    private String caseCode;

    @Column(name = "rescue_date", nullable = false)
    private java.time.LocalDate rescueDate;

    @Column(name = "rescue_location", nullable = false)
    private String rescueLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RescueStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_center_id", nullable = false)
    private RescueCenter rescueCenter;

    protected RescueCase() {
    }

    public RescueCase(String caseCode, java.time.LocalDate rescueDate,
                      String rescueLocation, RescueStatus status) {
        this.caseCode = caseCode;
        this.rescueDate = rescueDate;
        this.rescueLocation = rescueLocation;
        this.status = status;
    }

    public void setRescueCenter(RescueCenter rescueCenter) {
        this.rescueCenter = rescueCenter;
    }

    // Getters
    public Long getId() { return id; }
    public String getCaseCode() { return caseCode; }
    public java.time.LocalDate getRescueDate() { return rescueDate; }
    public String getRescueLocation() { return rescueLocation; }
    public RescueStatus getStatus() { return status; }
    public RescueCenter getRescueCenter() { return rescueCenter; }

    //setter
    //setter
    public void setStatus(RescueStatus status) {
        this.status = status;
    }

    @OneToOne(
            mappedBy = "rescueCase",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Animal animal;

// Agregar este método junto a setRescueCenter:

    public void assignAnimal(Animal animal) {
        this.animal = animal;
        animal.setRescueCase(this);
    }

// Agregar su getter junto a los demás:

    public Animal getAnimal() {
        return animal;
    }
}
