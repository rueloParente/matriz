package com.jiraclone.issues.web;

import com.jiraclone.issues.IssueStatus;
import com.jiraclone.projects.ProjectRef;
import java.util.Map;

record ProjectSummary(ProjectRef project, Map<IssueStatus, Long> counts) {

	public long totalIssues() {
		return counts.values().stream().mapToLong(Long::longValue).sum();
	}

	public long completedIssues() {
		return counts.getOrDefault(IssueStatus.DONE, 0L);
	}

	public long completionPercent() {
		long total = totalIssues();
		return total == 0 ? 0 : Math.round(100.0 * completedIssues() / total);
	}
}
