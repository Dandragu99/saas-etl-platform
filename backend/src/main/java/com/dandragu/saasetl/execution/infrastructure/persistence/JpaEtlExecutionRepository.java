package com.dandragu.saasetl.execution.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.execution.application.EtlExecutionPage;
import com.dandragu.saasetl.execution.application.EtlExecutionQuery;
import com.dandragu.saasetl.execution.application.EtlExecutionRepository;
import com.dandragu.saasetl.execution.application.EtlExecutionSummary;
import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;

@Repository
public class JpaEtlExecutionRepository implements EtlExecutionRepository {

	private static final EtlExecutionQuery DEFAULT_QUERY = new EtlExecutionQuery(null, null, null, 0, 100);

	private final SpringDataEtlExecutionRepository repository;

	public JpaEtlExecutionRepository(SpringDataEtlExecutionRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public EtlExecution save(EtlExecution execution) {
		return repository.save(EtlExecutionEntity.fromDomain(execution)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<EtlExecution> findByCompanyIdAndId(UUID companyId, UUID id) {
		return repository.findByCompanyIdAndId(companyId, id).map(EtlExecutionEntity::toDomain);
	}

	@Override
	@Transactional
	public Optional<EtlExecution> update(
			UUID companyId,
			UUID id,
			UnaryOperator<EtlExecution> updateOperation) {
		return repository.findByCompanyIdAndIdForUpdate(companyId, id)
				.map(entity -> {
					EtlExecution updated = updateOperation.apply(entity.toDomain());
					entity.updateFrom(updated);
					return updated;
				});
	}

	@Override
	@Transactional(readOnly = true)
	public List<EtlExecution> findAllByCompanyId(UUID companyId) {
		return search(companyId, DEFAULT_QUERY).content();
	}

	@Override
	@Transactional(readOnly = true)
	public EtlExecutionPage search(UUID companyId, EtlExecutionQuery query) {
		PageRequest pageable = PageRequest.of(
				query.page(),
				query.size(),
				Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id")));
		Specification<EtlExecutionEntity> specification = (root, criteriaQuery, builder) ->
				builder.equal(root.get("companyId"), companyId);
		if (query.status() != null) {
			specification = specification.and((root, criteriaQuery, builder) ->
					builder.equal(root.get("status"), query.status()));
		}
		if (query.startedFrom() != null) {
			specification = specification.and((root, criteriaQuery, builder) ->
					builder.greaterThanOrEqualTo(root.get("startedAt"), query.startedFrom()));
		}
		if (query.startedTo() != null) {
			specification = specification.and((root, criteriaQuery, builder) ->
					builder.lessThanOrEqualTo(root.get("startedAt"), query.startedTo()));
		}
		var result = repository.findAll(specification, pageable);
		return new EtlExecutionPage(
				result.getContent().stream().map(EtlExecutionEntity::toDomain).toList(),
				result.getTotalElements(),
				query.page(),
				query.size());
	}

	@Override
	@Transactional(readOnly = true)
	public EtlExecutionSummary summarize(UUID companyId) {
		return new EtlExecutionSummary(
				repository.countByCompanyId(companyId),
				repository.countByCompanyIdAndStatus(companyId, EtlExecutionStatus.SUCCESS),
				repository.countByCompanyIdAndStatus(companyId, EtlExecutionStatus.FAILED),
				repository.countByCompanyIdAndStatusIn(companyId, List.of(
						EtlExecutionStatus.PENDING,
						EtlExecutionStatus.RUNNING)));
	}
}
