package application.backend.dto;

import java.util.List;
import java.util.Set;

public class CriticalLineRecommendation {

	private String className;
	private String classPath;
	private List<CriticalLine> criticalLines;
	private Set<String> recommendations;

	public CriticalLineRecommendation(String className, String classPath, List<CriticalLine> criticalLines, Set<String> recommendations) {
		this.className = className;
		this.classPath = classPath;
		this.criticalLines = criticalLines;
		this.recommendations = recommendations;
	}

	public String getClassName() {
		return className;
	}

	public String getClassPath() {
		return classPath;
	}

	public List<CriticalLine> getCriticalLines() {
		return criticalLines;
	}

	public Set<String> getRecommendations() {
		return recommendations;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		if (!criticalLines.isEmpty()) {
			for (CriticalLine criticalLine : criticalLines) {
				stringBuilder.append(criticalLine.toString() + "\n");
			}
			stringBuilder.append("Recommendations:\n");
			for (String recommendation : recommendations) {
				stringBuilder.append("	-" + recommendation + "\n");
			}
		} else {
			stringBuilder.append("No critical lines");
		}
		stringBuilder.append("\n\n\n\n");
		return stringBuilder.toString();
	}
}
