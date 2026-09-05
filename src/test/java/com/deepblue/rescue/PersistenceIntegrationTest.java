package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void flywayHasExecutedMigrations() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class
        );

        assertThat(versions).contains("1", "2");
    }

    @Test
    void inheritedMethodsWork() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");

        RescueCenter saved = rescueCenterRepository.save(center);

        assertThat(saved.getId()).isNotNull();

        Optional<RescueCenter> found = rescueCenterRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("DB-CAR");

        assertThat(rescueCenterRepository.existsById(saved.getId())).isTrue();

        assertThat(rescueCenterRepository.count()).isEqualTo(1);
    }

    @Test
    void oneToManyRescueCenterAndRescueCases() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");

        RescueCase case1 = new RescueCase("CASE-001", LocalDate.of(2026, 1, 10),
                "Bahía Concha", RescueStatus.IN_REHABILITATION);
        RescueCase case2 = new RescueCase("CASE-002", LocalDate.of(2026, 2, 15),
                "Playa Blanca", RescueStatus.IN_REHABILITATION);

        center.addCase(case1);
        center.addCase(case2);

        rescueCenterRepository.save(center);

        RescueCenter found = rescueCenterRepository.findById(center.getId()).orElseThrow();

        assertThat(found.getCases()).hasSize(2);
        assertThat(found.getCases())
                .extracting(RescueCase::getRescueCenter)
                .containsOnly(found);
    }

    @Test
    void oneToOneRescueCaseAndAnimal() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");

        RescueCase rescueCase = new RescueCase("RES-2026-001", LocalDate.of(2026, 3, 1),
                "Bahía Concha", RescueStatus.ADMITTED);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle",
                "Chelonia mydas", AnimalSex.FEMALE);

        rescueCase.assignAnimal(animal);

        rescueCenterRepository.save(center);

        RescueCase foundCase = rescueCaseRepository.findByCaseCode("RES-2026-001").orElseThrow();

        assertThat(foundCase.getAnimal()).isNotNull();
        assertThat(foundCase.getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
        assertThat(foundCase.getAnimal().getRescueCase()).isEqualTo(foundCase);
    }

    @Test
    void oneToOneAnimalAndMedicalRecord() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCase rescueCase = new RescueCase("RES-2026-030", LocalDate.of(2026, 6, 1),
                "Bahía Concha", RescueStatus.ADMITTED);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-2026-002", "Green Sea Turtle",
                "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        MedicalRecord medicalRecord = new MedicalRecord(
                new BigDecimal("28.40"),
                "STABLE",
                "Left front flipper injury",
                null
        );
        animal.assignMedicalRecord(medicalRecord);

        rescueCenterRepository.save(center);

        Animal saved = animalRepository.findByAnimalCode("AN-2026-002").orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMedicalRecord().getId()).isNotNull();
    }

    @Test
    void manyToManySpecialistAndExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SP-001", "Elena", "Vargas",
                "elena.vargas@deepblue.org", true);

        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        specialistRepository.save(elena);

        Specialist found = specialistRepository.findById(elena.getId()).orElseThrow();

        assertThat(found.getExpertiseAreas()).hasSize(2);
        assertThat(found.getExpertiseAreas())
                .extracting(Expertise::getName)
                .containsExactlyInAnyOrder("Trauma", "Rehabilitation");
    }

    @Test
    void queryMethodByStatus() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");

        RescueCase case1 = new RescueCase("RES-001", LocalDate.of(2026, 1, 5),
                "Bahía Concha", RescueStatus.IN_REHABILITATION);
        RescueCase case2 = new RescueCase("RES-002", LocalDate.of(2026, 1, 10),
                "Playa Blanca", RescueStatus.READY_FOR_RELEASE);
        RescueCase case3 = new RescueCase("RES-003", LocalDate.of(2026, 1, 15),
                "Taganga", RescueStatus.IN_REHABILITATION);

        center.addCase(case1);
        center.addCase(case2);
        center.addCase(case3);

        rescueCenterRepository.save(center);

        List<RescueCase> inRehabilitation =
                rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(inRehabilitation).hasSize(2);
        assertThat(inRehabilitation)
                .extracting(RescueCase::getCaseCode)
                .containsExactly("RES-001", "RES-003");
    }

    @Test
    void queryMethodNavigatingRelations() {
        RescueCenter caribbean = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCenter pacific = new RescueCenter("DB-PAC", "DeepBlue Pacific Center", "Buenaventura");

        RescueCase caseCaribbean = new RescueCase("RES-CAR-001", LocalDate.of(2026, 1, 5),
                "Bahía Concha", RescueStatus.ADMITTED);
        caribbean.addCase(caseCaribbean);

        RescueCase casePacific = new RescueCase("RES-PAC-001", LocalDate.of(2026, 1, 8),
                "Isla Gorgona", RescueStatus.ADMITTED);
        pacific.addCase(casePacific);

        Animal turtle = new Animal("AN-CAR-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        caseCaribbean.assignAnimal(turtle);

        Animal dolphin = new Animal("AN-PAC-001", "Bottlenose Dolphin", "Tursiops truncatus", AnimalSex.MALE);
        casePacific.assignAnimal(dolphin);

        rescueCenterRepository.save(caribbean);
        rescueCenterRepository.save(pacific);

        List<Animal> caribbeanAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertThat(caribbeanAnimals).hasSize(1);
        assertThat(caribbeanAnimals)
                .extracting(Animal::getAnimalCode)
                .containsExactly("AN-CAR-001");
    }

    @Test
    void jpqlFindActiveSpecialistsByExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise marineBirds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        Specialist elena = new Specialist("SP-001", "Elena", "Vargas",
                "elena.vargas@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist mateo = new Specialist("SP-002", "Mateo", "Restrepo",
                "mateo.restrepo@deepblue.org", true);
        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);

        Specialist sofia = new Specialist("SP-003", "Sofia", "Herrera",
                "sofia.herrera@deepblue.org", true);
        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);

        specialistRepository.save(elena);
        specialistRepository.save(mateo);
        specialistRepository.save(sofia);

        List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");

        assertThat(traumaSpecialists).hasSize(2);
        assertThat(traumaSpecialists)
                .extracting(Specialist::getFirstName)
                .containsExactlyInAnyOrder("Elena", "Sofia");
    }

    @Test
    void queryMethodTreatmentsByAnimalOrderedChronologically() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCase rescueCase = new RescueCase("RES-2026-010", LocalDate.of(2026, 4, 1),
                "Bahía Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-2026-010", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        rescueCenterRepository.save(center);

        Specialist elena = new Specialist("SP-010", "Elena", "Vargas",
                "elena.v@deepblue.org", true);
        Specialist mateo = new Specialist("SP-011", "Mateo", "Restrepo",
                "mateo.r@deepblue.org", true);
        specialistRepository.save(elena);
        specialistRepository.save(mateo);

        Treatment treatment1 = new Treatment(animal, elena,
                LocalDateTime.of(2026, 4, 2, 9, 0), TreatmentType.WOUND_CARE, "Cleaning wound");
        Treatment treatment2 = new Treatment(animal, elena,
                LocalDateTime.of(2026, 4, 3, 9, 0), TreatmentType.HYDRATION, "Fluid therapy");
        Treatment treatment3 = new Treatment(animal, mateo,
                LocalDateTime.of(2026, 4, 4, 9, 0), TreatmentType.OBSERVATION, "General check-up");

        treatmentRepository.save(treatment1);
        treatmentRepository.save(treatment2);
        treatmentRepository.save(treatment3);

        List<Treatment> treatments =
                treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        assertThat(treatments).hasSize(3);
        assertThat(treatments)
                .extracting(Treatment::getDescription)
                .containsExactly("Cleaning wound", "Fluid therapy", "General check-up");
    }

    @Test
    void jpqlTreatmentsBetweenDates() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCase rescueCase = new RescueCase("RES-2026-020", LocalDate.of(2026, 7, 1),
                "Bahía Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-2026-020", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        rescueCenterRepository.save(center);

        Specialist elena = new Specialist("SP-020", "Elena", "Vargas",
                "elena.vargas.020@deepblue.org", true);
        specialistRepository.save(elena);

        Treatment early = new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Early treatment");
        Treatment middle = new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Middle treatment");
        Treatment late = new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "Late treatment");

        treatmentRepository.save(early);
        treatmentRepository.save(middle);
        treatmentRepository.save(late);

        List<Treatment> result = treatmentRepository.findBetweenDates(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 0, 0)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDescription()).isEqualTo("Middle treatment");
    }

    @Test
    void uniqueConstraintViolationOnDuplicateAnimalCode() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCase case1 = new RescueCase("RES-UQ-001", LocalDate.of(2026, 5, 1),
                "Bahía Concha", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("RES-UQ-002", LocalDate.of(2026, 5, 2),
                "Playa Blanca", RescueStatus.ADMITTED);
        center.addCase(case1);
        center.addCase(case2);
        rescueCenterRepository.save(center);

        Animal firstAnimal = new Animal("AN-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        case1.assignAnimal(firstAnimal);
        animalRepository.saveAndFlush(firstAnimal);

        Animal duplicateAnimal = new Animal("AN-100", "Bottlenose Dolphin", "Tursiops truncatus", AnimalSex.MALE);
        case2.assignAnimal(duplicateAnimal);

        assertThatThrownBy(() -> animalRepository.saveAndFlush(duplicateAnimal))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void integratorChallengeSeaTurtleScenario() {
        // 1. Centro
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");

        // 2. Caso
        RescueCase rescueCase = new RescueCase("RES-2026-100", LocalDate.of(2026, 8, 18),
                "Bahía Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);

        // 3. Animal
        Animal turtle = new Animal("AN-2026-100", "Green Sea Turtle",
                "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(turtle);

        // 4. Expediente médico
        MedicalRecord medicalRecord = new MedicalRecord(
                new BigDecimal("27.80"),
                "STABLE",
                "Injury caused by fishing net",
                "Possible plastic ingestion"
        );
        turtle.assignMedicalRecord(medicalRecord);

        // 5. Especialista con sus 3 áreas de expertise
        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas",
                "elena@deepblue.org", true);
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        // Persistimos el árbol RescueCenter -> RescueCase -> Animal -> MedicalRecord de una sola vez
        rescueCenterRepository.save(center);

        // 6. Tratamientos
        Treatment woundCare = new Treatment(turtle, elena,
                LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
        Treatment hydration = new Treatment(turtle, elena,
                LocalDateTime.of(2026, 8, 20, 9, 0),
                TreatmentType.HYDRATION, "Subcutaneous fluid therapy");

        treatmentRepository.save(woundCare);
        treatmentRepository.save(hydration);

        // ---- Consulta 1: ¿existe el caso RES-2026-100? ----
        assertThat(rescueCaseRepository.findByCaseCode("RES-2026-100")).isPresent();

        // ---- Consulta 2: casos IN_REHABILITATION ----
        List<RescueCase> inRehab =
                rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        assertThat(inRehab)
                .extracting(RescueCase::getCaseCode)
                .contains("RES-2026-100");

        // ---- Consulta 3: animales de DB-CAR ----
        List<Animal> centerAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
        assertThat(centerAnimals)
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");

        // ---- Consulta 4: nombre común contiene "turtle" ----
        List<Animal> turtles = animalRepository.findByCommonNameContainingIgnoreCase("turtle");
        assertThat(turtles)
                .extracting(Animal::getAnimalCode)
                .contains("AN-2026-100");

        // ---- Consulta 5: especialistas con Trauma ----
        List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(traumaSpecialists)
                .extracting(Specialist::getProfessionalCode)
                .contains("SPEC-001");

        // ---- Consulta 6: tratamientos del animal, ordenados cronológicamente ----
        List<Treatment> treatmentsOfTurtle =
                treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(turtle.getId());
        assertThat(treatmentsOfTurtle)
                .extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper", "Subcutaneous fluid therapy");

        // ---- Consulta 7: tratamientos por especialista con expertise Rehabilitation ----
        List<Treatment> rehabTreatments = treatmentRepository.findBySpecialistExpertise("Rehabilitation");
        assertThat(rehabTreatments).hasSize(2);

        // ---- Consulta 8: tratamientos entre fechas ----
        List<Treatment> betweenDates = treatmentRepository.findBetweenDates(
                LocalDateTime.of(2026, 8, 19, 0, 0),
                LocalDateTime.of(2026, 8, 19, 23, 59)
        );
        assertThat(betweenDates)
                .extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper");
    }

    @Test
    void jpqlAnimalsInRehabilitationTreatedByTraumaSpecialist() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();

        // Especialista CON Trauma
        Specialist elena = new Specialist("SP-TR-01", "Elena", "Vargas",
                "elena.tr@deepblue.org", true);
        elena.addExpertise(trauma);
        specialistRepository.save(elena);

        // Otro especialista CON Trauma también (para probar DISTINCT)
        Specialist mateo = new Specialist("SP-TR-02", "Mateo", "Restrepo",
                "mateo.tr@deepblue.org", true);
        mateo.addExpertise(trauma);
        specialistRepository.save(mateo);

        // Especialista SIN Trauma
        Specialist sofia = new Specialist("SP-TR-03", "Sofia", "Herrera",
                "sofia.tr@deepblue.org", true);
        sofia.addExpertise(marineMammals);
        specialistRepository.save(sofia);

        RescueCenter center = new RescueCenter("DB-TR", "DeepBlue Trauma Test Center", "Santa Marta");

        // Animal 1: IN_REHABILITATION + tratado por dos especialistas con Trauma -> debe aparecer UNA sola vez
        RescueCase case1 = new RescueCase("RES-TR-001", LocalDate.of(2026, 1, 10),
                "Bahía Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(case1);
        Animal matchingAnimal = new Animal("AN-TR-001", "Green Sea Turtle",
                "Chelonia mydas", AnimalSex.FEMALE);
        case1.assignAnimal(matchingAnimal);

        // Animal 2: IN_REHABILITATION pero tratado solo por especialista SIN Trauma -> NO debe aparecer
        RescueCase case2 = new RescueCase("RES-TR-002", LocalDate.of(2026, 1, 11),
                "Playa Blanca", RescueStatus.IN_REHABILITATION);
        center.addCase(case2);
        Animal wrongExpertiseAnimal = new Animal("AN-TR-002", "Bottlenose Dolphin",
                "Tursiops truncatus", AnimalSex.MALE);
        case2.assignAnimal(wrongExpertiseAnimal);

        // Animal 3: ADMITTED (no en rehabilitación) pero tratado por especialista CON Trauma -> NO debe aparecer
        RescueCase case3 = new RescueCase("RES-TR-003", LocalDate.of(2026, 1, 12),
                "Taganga", RescueStatus.ADMITTED);
        center.addCase(case3);
        Animal wrongStatusAnimal = new Animal("AN-TR-003", "Loggerhead Turtle",
                "Caretta caretta", AnimalSex.MALE);
        case3.assignAnimal(wrongStatusAnimal);

        rescueCenterRepository.save(center);

        Treatment t1 = new Treatment(matchingAnimal, elena,
                LocalDateTime.of(2026, 1, 13, 9, 0), TreatmentType.WOUND_CARE, "Wound cleaning");
        Treatment t2 = new Treatment(matchingAnimal, mateo,
                LocalDateTime.of(2026, 1, 14, 9, 0), TreatmentType.OBSERVATION, "Follow-up check");
        Treatment t3 = new Treatment(wrongExpertiseAnimal, sofia,
                LocalDateTime.of(2026, 1, 13, 9, 0), TreatmentType.HYDRATION, "Fluid therapy");
        Treatment t4 = new Treatment(wrongStatusAnimal, elena,
                LocalDateTime.of(2026, 1, 13, 9, 0), TreatmentType.WOUND_CARE, "Wound cleaning");

        treatmentRepository.save(t1);
        treatmentRepository.save(t2);
        treatmentRepository.save(t3);
        treatmentRepository.save(t4);

        List<Animal> result = animalRepository.findByStatusAndTreatingSpecialistExpertise(
                RescueStatus.IN_REHABILITATION, "trauma");

        assertThat(result)
                .extracting(Animal::getAnimalCode)
                .containsExactly("AN-TR-001");
    }
}