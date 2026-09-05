# DeepBlue Rescue
## Integrantes
**Cesar Acosta**

**Camilo Hincapie**

## Descripción

Cuando un centro de rescate encuentra un animal marino herido, registra un caso de
rescate, abre un expediente médico para el animal, y asigna especialistas —cada uno con
distintas áreas de experiencia— para aplicarle tratamientos a lo largo de su
rehabilitación. Este proyecto modela y persiste ese dominio, sin capa de servicio,
controladores ni frontend: el alcance es exclusivamente la persistencia.

## Modelo de datos

```text
RescueCenter
RescueCase
Animal
MedicalRecord
Specialist
Expertise
Treatment
```

Tablas físicas:

```text
rescue_centers
rescue_cases
animals
medical_records
specialists
expertise
specialist_expertise   (tabla asociativa)
treatments
```

## Relaciones

| Relación | Tipo | Dónde vive la FK |
|---|---|---|
| RescueCenter — RescueCase | 1:N | `rescue_cases.rescue_center_id` |
| RescueCase — Animal | 1:1 | `animals.rescue_case_id` (con `UNIQUE`, para forzar cardinalidad 1:1) |
| Animal — MedicalRecord | 1:1 | `medical_records.animal_id` (con `UNIQUE`) |
| Specialist — Expertise | N:M | tabla intermedia `specialist_expertise` |
| Animal — Treatment | 1:N | `treatments.animal_id` |
| Specialist — Treatment | 1:N | `treatments.specialist_id` |

## Cómo ejecutar el proyecto

Requisitos: Java 21, Maven (o el wrapper `mvnw`/`mvnw.cmd` incluido), y una instancia de
PostgreSQL accesible (por defecto en `localhost:5432`, base `deepblue`, usuario/clave
`postgres`, configurable con las variables de entorno `DB_URL`, `DB_USER`, `DB_PASSWORD`).

```bash
./mvnw spring-boot:run
```

Flyway ejecuta las migraciones automáticamente al arrancar.

## Cómo ejecutar los tests

Requiere Docker corriendo localmente (Testcontainers levanta un contenedor PostgreSQL
real para cada suite de pruebas, no se usa H2 ni mocks).

```bash
./mvnw clean test
```

## Flyway

Flyway es responsable exclusivo de crear y evolucionar el esquema de la base de datos.
Las migraciones se aplican en orden de versión, y una vez aplicadas, no se modifican
(los cambios posteriores se agregan como una nueva migración `Vn`).

```text
V1__create_schema.sql                    -> crea las 8 tablas, PKs, FKs, UNIQUE y CHECK
V2__insert_expertise_catalog.sql         -> catálogo inicial de Expertise
V3__add_tracking_device_to_animal.sql    -> agrega tracking_device_code a animals
```

La aplicación usa `spring.jpa.hibernate.ddl-auto: validate`: Hibernate **nunca** crea
ni modifica tablas, solo valida en cada arranque que las entidades JPA coincidan
exactamente con el esquema que Flyway ya construyó. Si un `@Column` no coincide con
la base real, el arranque falla de inmediato, evitando desincronizaciones silenciosas
entre el código Java y la base de datos.

## Testcontainers

Las pruebas de integración no usan una base de datos en memoria (H2): levantan un
contenedor Docker con PostgreSQL real (`@Testcontainers` + `@ServiceConnection`),
para que las constraints (`UNIQUE`, `CHECK`, `NOT NULL`, FKs) se prueben contra el motor
real de base de datos, con el mismo dialecto SQL que se usará en producción.

## Query Methods implementados

| Repository | Método |
|---|---|
| `RescueCenterRepository` | `findByCode(String code)` |
| `RescueCaseRepository` | `findByCaseCode(String caseCode)` |
| `RescueCaseRepository` | `findByStatusOrderByRescueDateAsc(RescueStatus status)` |
| `RescueCaseRepository` | `findByRescueCenterCode(String code)` |
| `RescueCaseRepository` | `findByRescueDateAfterOrderByRescueDateDesc(LocalDate date)` |
| `AnimalRepository` | `findByAnimalCode(String animalCode)` |
| `AnimalRepository` | `findByCommonNameContainingIgnoreCase(String text)` |
| `AnimalRepository` | `findByRescueCaseStatus(RescueStatus status)` |
| `AnimalRepository` | `findByRescueCaseRescueCenterCode(String centerCode)` — navega dos relaciones |
| `ExpertiseRepository` | `findByNameIgnoreCase(String name)` |
| `TreatmentRepository` | `findByAnimalIdOrderByPerformedAtAsc(Long animalId)` |

## Consultas JPQL implementadas (`@Query`)

| Repository | Método | Qué resuelve |
|---|---|---|
| `SpecialistRepository` | `findActiveByExpertise(String expertiseName)` | especialistas activos con determinada expertise, ignorando mayúsculas |
| `TreatmentRepository` | `findBetweenDates(LocalDateTime start, LocalDateTime end)` | tratamientos realizados en un intervalo de fechas |
| `TreatmentRepository` | `findByRescueCenterCode(String centerCode)` | tratamientos de animales de un centro, navegando `animal.rescueCase.rescueCenter` |
| `TreatmentRepository` | `findBySpecialistExpertise(String expertiseName)` | tratamientos realizados por especialistas con determinada expertise |
| `AnimalRepository` | `findByStatusAndTreatingSpecialistExpertise(RescueStatus status, String expertiseName)` | animales con determinado status que recibieron al menos un tratamiento de un especialista con determinada expertise (usa `DISTINCT` para evitar duplicados cuando hay varios tratamientos calificados) |

Estas cinco se eligieron sobre Query Methods porque combinan varias relaciones,
condiciones sobre entidades intermedias, o lógica (`DISTINCT`, `BETWEEN`, `lower(...)`)
que un nombre de método haría ilegible o inviable.
