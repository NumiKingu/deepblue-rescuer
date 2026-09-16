package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {
    // Consulta A
    Optional<Animal> findByAnimalCode(String animalCode);

    Optional<Specialist> findByProfessionalCode(String professionalCode);

    // Consulta B
    List<Animal> findByCommonNameContainingIgnoreCase(String text);

    List<Animal> findByRescueCaseStatus(RescueStatus status);

    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);

    List<Treatment> findByAnimalAnimalCodeOrderByPerformedAtAsc(
            String animalCode);

    @Query("""
    select distinct a
    from Animal a
    join a.rescueCase rc
    join a.treatments t
    join t.specialist s
    join s.expertiseAreas e
    where rc.status = :status
    and lower(e.name) = lower(:expertiseName)
    """)
    List<Animal> findByStatusAndTreatingSpecialistExpertise(
            @Param("status") RescueStatus status,
            @Param("expertiseName") String expertiseName);
}