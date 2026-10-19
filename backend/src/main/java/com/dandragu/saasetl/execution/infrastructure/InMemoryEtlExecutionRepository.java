package com.dandragu.saasetl.execution.infrastructure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.UnaryOperator;

import com.dandragu.saasetl.execution.application.EtlExecutionRepository;
import com.dandragu.saasetl.execution.application.EtlExecutionPage;
import com.dandragu.saasetl.execution.application.EtlExecutionQuery;
import com.dandragu.saasetl.execution.application.EtlExecutionSummary;
import com.dandragu.saasetl.execution.domain.EtlExecution;
import com.dandragu.saasetl.execution.domain.EtlExecutionStatus;

public class InMemoryEtlExecutionRepository implements EtlExecutionRepository {

	public static final int MAX_HISTORY_SIZE = 100;

	private final Map<UUID, EtlExecution> executions = new LinkedHashMap<>();
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	@Override
	public EtlExecution save(EtlExecution execution) {
		lock.writeLock().lock();
		try {
			executions.put(execution.id(), execution);
			removeOldestExecutionsOverLimit();
			return execution;
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public Optional<EtlExecution> findByCompanyIdAndId(UUID companyId, UUID id) {
		lock.readLock().lock();
		try {
			return Optional.ofNullable(executions.get(id))
					.filter(execution -> companyId.equals(execution.companyId()));
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Optional<EtlExecution> update(
			UUID companyId,
			UUID id,
			UnaryOperator<EtlExecution> updateOperation) {
		lock.writeLock().lock();
		try {
			EtlExecution currentExecution = executions.get(id);
			if (currentExecution == null || !companyId.equals(currentExecution.companyId())) {
				return Optional.empty();
			}

			EtlExecution updatedExecution = updateOperation.apply(currentExecution);
			executions.put(id, updatedExecution);
			return Optional.of(updatedExecution);
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public List<EtlExecution> findAllByCompanyId(UUID companyId) {
		lock.readLock().lock();
		try {
			List<EtlExecution> newestFirst = new ArrayList<>(executions.values());
			newestFirst.removeIf(execution -> !companyId.equals(execution.companyId()));
			newestFirst.sort((left, right) -> {
				int byDate = right.startedAt().compareTo(left.startedAt());
				return byDate != 0 ? byDate : right.id().compareTo(left.id());
			});
			return List.copyOf(newestFirst);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public EtlExecutionPage search(UUID companyId, EtlExecutionQuery query) {
		List<EtlExecution> filtered = findAllByCompanyId(companyId).stream()
				.filter(execution -> query.status() == null || execution.status() == query.status())
				.filter(execution -> query.startedFrom() == null
						|| !execution.startedAt().isBefore(query.startedFrom()))
				.filter(execution -> query.startedTo() == null
						|| !execution.startedAt().isAfter(query.startedTo()))
				.toList();
		int fromIndex = Math.min(query.page() * query.size(), filtered.size());
		int toIndex = Math.min(fromIndex + query.size(), filtered.size());
		return new EtlExecutionPage(
				filtered.subList(fromIndex, toIndex),
				filtered.size(),
				query.page(),
				query.size());
	}

	@Override
	public EtlExecutionSummary summarize(UUID companyId) {
		List<EtlExecution> all = findAllByCompanyId(companyId);
		long successful = all.stream().filter(execution -> execution.status() == EtlExecutionStatus.SUCCESS).count();
		long failed = all.stream().filter(execution -> execution.status() == EtlExecutionStatus.FAILED).count();
		long inProgress = all.stream()
				.filter(execution -> execution.status() == EtlExecutionStatus.PENDING
						|| execution.status() == EtlExecutionStatus.RUNNING)
				.count();
		return new EtlExecutionSummary(all.size(), successful, failed, inProgress);
	}

	private void removeOldestExecutionsOverLimit() {
		if (executions.size() <= MAX_HISTORY_SIZE) {
			return;
		}

		int executionsToRemove = executions.size() - MAX_HISTORY_SIZE;
		List<UUID> oldestIds = new ArrayList<>(executions.keySet());
		for (int index = 0; index < executionsToRemove; index++) {
			executions.remove(oldestIds.get(index));
		}
	}
}
