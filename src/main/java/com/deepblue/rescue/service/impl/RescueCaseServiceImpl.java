package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.RescueCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RescueCaseServiceImpl
        implements RescueCaseService {

    private final RescueCaseRepository repository;

    private final RescueCaseMapper mapper;

    public RescueCaseServiceImpl(
            RescueCaseRepository repository,
            RescueCaseMapper mapper) {

        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public RescueCaseResponse findByCode(
            String caseCode) {

        return repository
                .findByCaseCode(caseCode)
                .map(mapper::toResponse)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Rescue case not found: "
                                        + caseCode
                        )
                );
    }

    private boolean isValidTransition(
            RescueStatus current,
            RescueStatus next) {

        return switch (current) {

            case ADMITTED ->
                    next == RescueStatus.UNDER_EVALUATION;

            case UNDER_EVALUATION ->
                    next == RescueStatus.IN_REHABILITATION;

            case IN_REHABILITATION ->
                    next == RescueStatus.READY_FOR_RELEASE;

            case READY_FOR_RELEASE ->
                    next == RescueStatus.RELEASED;

            default -> false;
        };
    }

    @Override
    public List<RescueCaseResponse> findByStatus(
            RescueStatus status) {

        return repository
                .findByStatusOrderByRescueDateAsc(status)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RescueCaseResponse changeStatus(
            String caseCode,
            ChangeRescueStatusRequest request) {

        // 1. Buscar el RescueCase
        RescueCase rescueCase = repository
                .findByCaseCode(caseCode)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Rescue case not found: " + caseCode
                        )
                );

        // 2. Obtener el estado actual
        RescueStatus currentStatus = rescueCase.getStatus();

        // 3. Validar la transición
        if (!isValidTransition(currentStatus, request.status())) {
            throw new BusinessRuleException(
                    "Cannot change status from "
                            + currentStatus
                            + " to "
                            + request.status()
            );
        }

        // 4. Cambiar el status  ← ESTA es la línea que probablemente te falta
        rescueCase.setStatus(request.status());

        // 5. Guardar
        RescueCase saved = repository.save(rescueCase);

        // 6. Transformar a Response y retornar
        return mapper.toResponse(saved);
    }

}