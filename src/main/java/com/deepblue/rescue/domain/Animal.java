package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_code", nullable = false, unique = true)
    private String animalCode;

    @Column(name = "common_name", nullable = false)
    private String commonName;

    @Column(name = "scientific_name", nullable = false)
    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnimalSex sex;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_case_id", unique = true)
    private RescueCase rescueCase;

    protected Animal() {
    }

    public Animal(String animalCode, String commonName,
                  String scientificName, AnimalSex sex) {
        this.animalCode = animalCode;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.sex = sex;
    }

    public void setRescueCase(RescueCase rescueCase) {
        this.rescueCase = rescueCase;
    }

    // Getters
    public Long getId() { return id; }
    public String getAnimalCode() { return animalCode; }
    public String getCommonName() { return commonName; }
    public String getScientificName() { return scientificName; }
    public AnimalSex getSex() { return sex; }
    public RescueCase getRescueCase() { return rescueCase; }

    @OneToOne(
            mappedBy = "animal",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private MedicalRecord medicalRecord;

    public void assignMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
        medicalRecord.setAnimal(this);
    }

    public MedicalRecord getMedicalRecord() {
        return medicalRecord;
    }

    @OneToMany(mappedBy = "animal")
    private List<Treatment> treatments = new ArrayList<>();

    public List<Treatment> getTreatments() { return treatments; }
}