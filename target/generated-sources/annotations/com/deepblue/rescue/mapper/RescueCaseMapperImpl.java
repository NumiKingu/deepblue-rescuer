package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import java.time.LocalDate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-23T22:01:35-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class RescueCaseMapperImpl implements RescueCaseMapper {

    @Override
    public RescueCaseResponse toResponse(RescueCase rescueCase) {
        if ( rescueCase == null ) {
            return null;
        }

        String centerCode = null;
        String animalCode = null;
        Long id = null;
        String caseCode = null;
        LocalDate rescueDate = null;
        String rescueLocation = null;
        RescueStatus status = null;

        centerCode = rescueCaseRescueCenterCode( rescueCase );
        animalCode = rescueCaseAnimalAnimalCode( rescueCase );
        id = rescueCase.getId();
        caseCode = rescueCase.getCaseCode();
        rescueDate = rescueCase.getRescueDate();
        rescueLocation = rescueCase.getRescueLocation();
        status = rescueCase.getStatus();

        RescueCaseResponse rescueCaseResponse = new RescueCaseResponse( id, caseCode, rescueDate, rescueLocation, status, centerCode, animalCode );

        return rescueCaseResponse;
    }

    private String rescueCaseRescueCenterCode(RescueCase rescueCase) {
        RescueCenter rescueCenter = rescueCase.getRescueCenter();
        if ( rescueCenter == null ) {
            return null;
        }
        return rescueCenter.getCode();
    }

    private String rescueCaseAnimalAnimalCode(RescueCase rescueCase) {
        Animal animal = rescueCase.getAnimal();
        if ( animal == null ) {
            return null;
        }
        return animal.getAnimalCode();
    }
}
