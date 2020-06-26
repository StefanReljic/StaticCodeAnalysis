package application.backend.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import application.backend.dto.CriticalLine;
import application.backend.dto.ExpressionInfo;
import application.backend.dto.types.CriticalLineDescriptor;
import application.backend.dto.types.ExpressionTypes;
import application.backend.dto.types.MethodNames;
import application.backend.dto.types.Recommendation;

public class ExpressionInfoCriticalChecker {

	private ExpressionInfo expressionInfo;
	private List<Expression> infixExpressionParts;
	private Set<String> criticalLineDescriptors;
	private List<CriticalLine> criticalLines;

	public ExpressionInfoCriticalChecker(ExpressionInfo expressionInfo) {
		this.expressionInfo = expressionInfo;
		this.infixExpressionParts = new ArrayList<>();
		this.criticalLineDescriptors = new HashSet<>();
		this.criticalLines = new ArrayList<>();
	}

	public List<CriticalLine> getCriticalLines() {
		ExpressionInfo pom = expressionInfo;

		while (pom != null) {
			Expression expression = pom.getExpression();
			if (expression instanceof InfixExpression) {
				checkInfixExpressionPartsForInterpolation(expression);
			} else if (expression instanceof MethodInvocation) {
				checkMethodInvocationForFormatOrConcatenation(expression);
			} else if (expression instanceof Assignment) {
				checkAssignmentParts(expression);
			}
			if (!criticalLineDescriptors.isEmpty()) {
				criticalLines.add(new CriticalLine(pom, criticalLineDescriptors));
				criticalLineDescriptors = new HashSet<>();
			}
			pom = pom.getParent();
		}

		return criticalLines;
	}

	private void createCriticalLineDescriptorFromAssignmentParts(Expression expression) {
		if (expression instanceof Assignment) {
			Assignment assignment = (Assignment) expression;
			boolean isConcatenation = assignment.getOperator().equals(Operator.PLUS_ASSIGN);
			boolean containsInterpolation = criticalLineDescriptors.contains(CriticalLineDescriptor.STRING_INTERPOLATION.getDescriptor());
			if (isConcatenation && containsInterpolation) {
				criticalLineDescriptors.add(CriticalLineDescriptor.STRING_CONCATENATION.getDescriptor());
			} else if (!containsInterpolation) {
				criticalLineDescriptors.remove(CriticalLineDescriptor.STRING_CONCATENATION.getDescriptor());
			}
		}
	}

	private void checkAssignmentParts(Expression expression) {
		if (!(expression instanceof Assignment))
			return;

		Assignment assignment = (Assignment) expression;
		Expression rightSide = assignment.getRightHandSide();
		if (rightSide instanceof InfixExpression) {
			checkInfixExpressionPartsForInterpolation(rightSide);
		} else if (rightSide instanceof MethodInvocation) {
			checkMethodInvocationForFormatOrConcatenation(rightSide);
		}
		createCriticalLineDescriptorFromAssignmentParts(expression);
	}

	private void checkMethodInvocationForFormatOrConcatenation(Expression expression) {
		if (!(expression instanceof MethodInvocation))
			return;

		MethodInvocation methodInvocation = (MethodInvocation) expression;
		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		if (methodBinding != null) {
			String className = methodBinding.getDeclaringClass().getBinaryName();
			String methodName = methodBinding.getName();
			if (className.equals(ExpressionTypes.STRING.getType()) && methodName.equals(MethodNames.FORMAT.getName())) {
				criticalLineDescriptors.add(CriticalLineDescriptor.STRING_FORMAT.getDescriptor());
			} else if (className.equals(ExpressionTypes.STRING.getType()) && methodName.equals(MethodNames.CONCAT.getName())) {
				criticalLineDescriptors.add(CriticalLineDescriptor.STRING_CONCATENATION.getDescriptor());
			}
		}
	}

	private void checkInfixExpressionPartsForInterpolation(Expression expression) {
		if (!(expression instanceof InfixExpression)) {
			return;
		}
		InfixExpression infixExpression = (InfixExpression) expression;
		infixExpressionParts.add(infixExpression.getLeftOperand());
		infixExpressionParts.add(infixExpression.getRightOperand());
		infixExpressionParts.addAll(infixExpression.extendedOperands());
		removeConstantsInInfixExpressionParts();
		if (!infixExpressionParts.isEmpty()) {
			criticalLineDescriptors.add(CriticalLineDescriptor.STRING_INTERPOLATION.getDescriptor());
		}
	}

	private void removeConstantsInInfixExpressionParts() {
		infixExpressionParts = infixExpressionParts.stream().filter(expression -> {
			String expressionString = expression.toString();
			boolean isConstant = expressionString.startsWith("\"") && expressionString.endsWith("\"");
			return !isConstant;
		}).collect(Collectors.toList());
	}

	public static Set<String> getCriticalLineRecommendations(CriticalLine criticalLine, int numberOfSetters) {
		Set<String> recommendations = new HashSet<>();
		if (criticalLine.getCriticalLineDescriptors().contains(CriticalLineDescriptor.STRING_INTERPOLATION.getDescriptor())) {
			if (numberOfSetters == 0) {
				recommendations.add(Recommendation.USE_PARAMETRIZED_QUERY.getRecomendation());
			} else {
				recommendations.add(Recommendation.USE_PARAMETRIZED_QUERY_FULLY.getRecomendation());
			}
			if (criticalLine.getCriticalLineDescriptors().contains(CriticalLineDescriptor.STRING_CONCATENATION.getDescriptor())) {
				recommendations.add(Recommendation.DONT_USE_CONCATENATION.getRecomendation());
			}
		}
		if (criticalLine.getCriticalLineDescriptors().contains(CriticalLineDescriptor.STRING_FORMAT.getDescriptor())) {
			recommendations.add(Recommendation.DONT_USE_STRING_FORMAT.getRecomendation());
		}
		return recommendations;
	}

}
