package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    // Paso 32
    @Test
    void shouldRegisterTreatmentWhenDataIsValid() {

        Animal animal = mock(Animal.class);
        Specialist specialist = mock(Specialist.class);
        RescueCase rescueCase = mock(RescueCase.class);
        Treatment savedTreatment = mock(Treatment.class);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        when(specialist.isActive()).thenReturn(true);

        when(animal.getRescueCase()).thenReturn(rescueCase);

        when(rescueCase.getStatus()).thenReturn(RescueStatus.IN_REHABILITATION);

        when(rescueCase.getRescueDate())
                .thenReturn(LocalDate.of(2026, 8, 20));

        when(treatmentRepository.save(any(Treatment.class)))
                .thenReturn(savedTreatment);

        TreatmentResponse response = new TreatmentResponse(
                1L,
                "AN-001",
                "SPEC-001",
                request.performedAt(),
                TreatmentType.WOUND_CARE,
                request.description()
        );

        when(mapper.toResponse(savedTreatment))
                .thenReturn(response);

        TreatmentResponse result = service.register(request);

        assertThat(result).isEqualTo(response);

        verify(treatmentRepository).save(any(Treatment.class));
    }

    // Paso 33
    @Test
    void shouldThrowWhenSpecialistIsInactive() {

        Animal animal = mock(Animal.class);
        Specialist specialist = mock(Specialist.class);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.OBSERVATION,
                "Routine check."
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        when(specialist.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    // Paso 34
    @Test
    void shouldThrowWhenRescueCaseIsReleased() {

        Animal animal = mock(Animal.class);
        Specialist specialist = mock(Specialist.class);
        RescueCase rescueCase = mock(RescueCase.class);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.OBSERVATION,
                "Routine check."
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        when(specialist.isActive()).thenReturn(true);

        when(animal.getRescueCase()).thenReturn(rescueCase);

        when(rescueCase.getStatus()).thenReturn(RescueStatus.RELEASED);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }
}