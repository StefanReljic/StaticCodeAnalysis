package application.backend.dto;

import java.util.Set;

public class CriticalLine {

	private ExpressionInfo expressionInfo;
	private Set<String> criticalLineDescriptors;

	public CriticalLine(ExpressionInfo expressionInfo, Set<String> criticalLineDescriptors) {
		super();
		this.expressionInfo = expressionInfo;
		this.criticalLineDescriptors = criticalLineDescriptors;
	}

	public ExpressionInfo getExpressionInfo() {
		return expressionInfo;
	}

	public void setExpressionInfo(ExpressionInfo expressionInfo) {
		this.expressionInfo = expressionInfo;
	}

	public Set<String> getCriticalLineDescriptors() {
		return criticalLineDescriptors;
	}

	public void setCriticalLineDescriptors(Set<String> criticalLineDescriptors) {
		this.criticalLineDescriptors = criticalLineDescriptors;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Critical line:\n	" + expressionInfo.toString() + "\n");
		stringBuilder.append("Description:	");
		for (String descriptor : criticalLineDescriptors)
			stringBuilder.append(descriptor + ", ");
		String result = stringBuilder.toString();
		result = result.substring(0, result.length() - 2);
		return result;
	}

}
