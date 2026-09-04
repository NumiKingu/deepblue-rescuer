package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {
    // Consulta A
    Optional<Animal> findByAnimalCode(String animalCode);

    // Consulta B
    List<Animal> findByCommonNameContainingIgnoreCase(String text);

    List<Animal> findByRescueCaseStatus(RescueStatus status);
}