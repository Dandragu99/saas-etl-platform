package com.dandragu.saasetl.company.infrastructure.context;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component
public class ActiveCompanySession {

	private static final String ATTRIBUTE = ActiveCompanySession.class.getName() + ".companyId";

	public Optional<UUID> find(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return Optional.empty();
		}
		Object value = session.getAttribute(ATTRIBUTE);
		return value instanceof UUID companyId ? Optional.of(companyId) : Optional.empty();
	}

	public void select(HttpServletRequest request, UUID companyId) {
		request.getSession().setAttribute(ATTRIBUTE, companyId);
	}

	public void clear(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute(ATTRIBUTE);
		}
	}
}
