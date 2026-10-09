package com.dandragu.saasetl.execution.infrastructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.UnaryOperator;

import org.springframework.stereotype.Repository;

import com.dandragu.saasetl.execution.application.EtlExecutionRepository;
import com.dandragu.saasetl.execution.domain.EtlExecution;

@Repository
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
	public Optional<EtlExecution> findById(UUID id) {
		lock.readLock().lock();
		try {
			return Optional.ofNullable(executions.get(id));
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public Optional<EtlExecution> update(
			UUID id,
			UnaryOperator<EtlExecution> updateOperation) {
		lock.writeLock().lock();
		try {
			EtlExecution currentExecution = executions.get(id);
			if (currentExecution == null) {
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
	public List<EtlExecution> findAll() {
		lock.readLock().lock();
		try {
			List<EtlExecution> newestFirst = new ArrayList<>(executions.values());
			Collections.reverse(newestFirst);
			return List.copyOf(newestFirst);
		}
		finally {
			lock.readLock().unlock();
		}
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
