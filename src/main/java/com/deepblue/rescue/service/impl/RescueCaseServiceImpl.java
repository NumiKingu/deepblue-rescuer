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

        // TODO 1 y 2:
        RescueCase rescueCase = repository
                .findByCaseCode(caseCode)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Rescue case not found: " + caseCode
                        )
                );

        // TODO 3:
        RescueStatus currentStatus = rescueCase.getStatus();

        // TODO 4 y 5:
        if (!isValidTransition(currentStatus, request.status())) {
            throw new BusinessRuleException(
                    "Invalid transition from " + currentStatus
                            + " to " + request.status()
            );
        }

        // TODO 6:
        rescueCase.setStatus(request.status());

        // TODO 7:
        RescueCase saved = repository.save(rescueCase);

        // TODO 8:
        return mapper.toResponse(saved);
    }

}