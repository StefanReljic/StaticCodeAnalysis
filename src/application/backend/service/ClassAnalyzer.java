package application.backend.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import application.backend.dto.ExpressionInfo;
import application.backend.service.finder.Finder;
import application.backend.service.finder.VariableDeclarationFinder;

public class ClassAnalyzer {

	private ClassParser classParser;
	private List<ExpressionInfo> expressions;
	private Finder finder;

	public ClassAnalyzer(String projectRoot, String relativeClassPath) throws IOException {
		classParser = new ClassParser(projectRoot, relativeClassPath);
		finder = new Finder(classParser);
	}

	/**
	 * Analyses current java class and looks for all execute method occurrences.
	 */
	public void analyse() {
		makeExpressionsListDistinct();
		if (expressions.isEmpty()) {
			return;
		}
		for (int i = 0; i < expressions.size(); ++i) {
			ExpressionInfo expressionInfo = expressions.get(i);
			if (isExpressionNullValue(expressionInfo)) {
				VariableDeclarationFinder variableDeclarationFinder = new VariableDeclarationFinder(classParser);
				variableDeclarationFinder.findNextAssignmentOfExpression(expressionInfo);
			}
			ExpressionInfo expressionInfoForVisit = getLastDefinedReference(expressionInfo);
			finder.generateExpressionTree(expressionInfoForVisit);
		}
	}

	private boolean isExpressionNullValue(ExpressionInfo expressionInfo) {
		return expressionInfo.getExpression().toString().equals("null");
	}

	private void makeExpressionsListDistinct() {
		expressions = finder.findExecuteMethodExpressions();
		expressions = expressions.stream().distinct().collect(Collectors.toList());
	}

	private ExpressionInfo getLastDefinedReference(ExpressionInfo expressionInfo) {
		ExpressionInfo expressionInfoForVisit = expressionInfo;
		while (expressionInfoForVisit.getParent() != null)
			expressionInfoForVisit = expressionInfoForVisit.getParent();
		return expressionInfoForVisit;
	}

	public List<ExpressionInfo> getExpressions() {
		return expressions;
	}
}
