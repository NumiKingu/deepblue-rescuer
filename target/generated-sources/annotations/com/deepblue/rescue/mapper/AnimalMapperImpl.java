package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-23T22:01:36-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class AnimalMapperImpl implements AnimalMapper {

    @Override
    public AnimalResponse toResponse(Animal animal) {
        if ( animal == null ) {
            return null;
        }

        String caseCode = null;
        RescueStatus rescueStatus = null;
        Long id = null;
        String animalCode = null;
        String commonName = null;
        String scientificName = null;
        String sex = null;

        caseCode = animalRescueCaseCaseCode( animal );
        rescueStatus = animalRescueCaseStatus( animal );
        id = animal.getId();
        animalCode = animal.getAnimalCode();
        commonName = animal.getCommonName();
        scientificName = animal.getScientificName();
        if ( animal.getSex() != null ) {
            sex = animal.getSex().name();
        }

        AnimalResponse animalResponse = new AnimalResponse( id, animalCode, commonName, scientificName, sex, caseCode, rescueStatus );

        return animalResponse;
    }

    private String animalRescueCaseCaseCode(Animal animal) {
        RescueCase rescueCase = animal.getRescueCase();
        if ( rescueCase == null ) {
            return null;
        }
        return rescueCase.getCaseCode();
    }

    private RescueStatus animalRescueCaseStatus(Animal animal) {
        RescueCase rescueCase = animal.getRescueCase();
        if ( rescueCase == null ) {
            return null;
        }
        return rescueCase.getStatus();
    }
}
