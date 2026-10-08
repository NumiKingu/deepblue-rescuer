package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        AnimalController.class
)
@Import(
        GlobalExceptionHandler.class
)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    private AnimalResponse animalResponse(
            String animalCode) {

        return new AnimalResponse(
                1L,
                animalCode,
                "Green Sea Turtle",
                "Chelonia mydas",
                "FEMALE",
                "RES-2026-001",
                RescueStatus.IN_REHABILITATION
        );
    }

    @Test
    void shouldReturnAnimalByCode()
            throws Exception {

        when(
                animalService.findByCode("AN-001")
        ).thenReturn(
                animalResponse("AN-001")
        );

        mockMvc.perform(
                        get(
                                "/api/animals/{animalCode}",
                                "AN-001"
                        )
                )
                .andExpect(
                        status().isOk()
                );

        verify(animalService)
                .findByCode("AN-001");
    }

    @Test
    void shouldReturnAnimalsInRehabilitation()
            throws Exception {

        AnimalResponse animal1 =
                animalResponse("AN-001");

        AnimalResponse animal2 =
                animalResponse("AN-002");

        when(
                animalService
                        .findAnimalsInRehabilitation()
        )
                .thenReturn(
                        List.of(animal1, animal2)
                );

        mockMvc.perform(
                        get("/api/animals/in-rehabilitation")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                );

        verify(animalService)
                .findAnimalsInRehabilitation();
    }

    @Test
    void shouldReturnAnimalTreatments()
            throws Exception {

        TreatmentResponse treatment =
                new TreatmentResponse(
                        100L,
                        "AN-001",
                        "SPEC-001",
                        LocalDateTime.of(
                                2026,
                                8,
                                21,
                                9,
                                0
                        ),
                        TreatmentType.WOUND_CARE,
                        "Cleaning and treatment of flipper injury."
                );

        when(
                treatmentService
                        .findByAnimalCode("AN-001")
        ).thenReturn(
                List.of(treatment)
        );

        mockMvc.perform(
                        get(
                                "/api/animals/{animalCode}/treatments",
                                "AN-001"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].animalCode")
                                .value("AN-001")
                );

        verify(treatmentService)
                .findByAnimalCode(
                        "AN-001"
                );
    }

    @Test
    void shouldReturnTreatmentEligibility()
            throws Exception {

        when(
                animalService
                        .canReceiveTreatment(
                                "AN-001"
                        )
        )
                .thenReturn(true);

        mockMvc.perform(
                        get(
                                "/api/animals/{animalCode}/treatment-eligibility",
                                "AN-001"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.animalCode")
                                .value("AN-001")
                )
                .andExpect(
                        jsonPath("$.eligible")
                                .value(true)
                );

        verify(animalService)
                .canReceiveTreatment(
                        "AN-001"
                );
    }
}