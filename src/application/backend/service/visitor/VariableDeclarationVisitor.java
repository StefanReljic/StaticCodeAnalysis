package application.backend.service.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import application.backend.dto.ExpressionInfo;
import application.backend.service.processor.FieldDeclarationProcessor;
import application.backend.service.processor.VariableDeclarationExpressionProcessor;
import application.backend.service.processor.VariableDeclarationStatementProcessor;
import application.backend.service.processor.VariableProcessor;

public class VariableDeclarationVisitor extends ASTVisitor {

	private CompilationUnit compilationUnit;
	private Expression expression;
	private List<ExpressionInfo> expressions;
	private int expressionLineNumber;
	private int expressionScopeStart;

	public VariableDeclarationVisitor(CompilationUnit compilationUnit, Expression expression) {
		super();
		this.compilationUnit = compilationUnit;
		this.expression = expression;
		this.expressions = new ArrayList<>();
		this.expressionLineNumber = compilationUnit.getLineNumber(expression.getStartPosition());
		this.expressionScopeStart = getExpressionScopeStart(expression);
	}

	public void processCurrentExpression() {
		expressions.clear();
		compilationUnit.accept(this);
		filterExpressionsAfterVisiting();
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		int nodeLineNumber = compilationUnit.getLineNumber(node.getStartPosition());
		Optional<ExpressionInfo> resultExpressionInfo = Optional.empty();

		if (nodeLineNumber < expressionLineNumber) {
			resultExpressionInfo = processVariableDeclarationFragment(node);
		}
		if (resultExpressionInfo.isPresent()) {
			expressions.add(resultExpressionInfo.get());
		}

		return true;
	}

	private Optional<ExpressionInfo> processVariableDeclarationFragment(VariableDeclarationFragment node) {

		Optional<VariableProcessor> variableProcessor = getVariableProcessor(node);
		if (variableProcessor.isPresent()) {
			return variableProcessor.get().processNode(node);
		}
		return Optional.empty();
	}

	private Optional<VariableProcessor> getVariableProcessor(ASTNode node) {
		if (node == null || node.getParent() == null) {
			return Optional.empty();
		}
		ASTNode parent = node.getParent();

		if (parent instanceof FieldDeclaration) {
			return Optional.of(new FieldDeclarationProcessor(compilationUnit, expression));
		}
		if (parent instanceof VariableDeclarationStatement) {
			return Optional.of(new VariableDeclarationStatementProcessor(compilationUnit, expression));
		}
		if (parent instanceof VariableDeclarationExpression) {
			return Optional.of(new VariableDeclarationExpressionProcessor(compilationUnit, expression));
		}
		return Optional.empty();
	}

	private void filterExpressionsAfterVisiting() {

		boolean isExpressionInMethodScope = filterExpressionsInMethodScope();
		if (!isExpressionInMethodScope) {
			filterExpressionsInClassScope();
		}
	}

	private boolean filterExpressionsInMethodScope() {

		List<ExpressionInfo> helpList = new ArrayList<>(expressions);
		if (expressionScopeStart != 0) {
			helpList = helpList.stream().filter(eInfo -> eInfo.getDeclarationLineNumber() > expressionScopeStart).collect(Collectors.toList());
			helpList = helpList.stream().filter(eInfo -> eInfo.getExpression().toString().equals(expression.toString())
					|| expression.toString().contains(eInfo.getLeftSide().toString())).collect(Collectors.toList());
		}
		if (!helpList.isEmpty()) {
			expressions = helpList;
			return true;
		}

		return false;
	}

	private void filterExpressionsInClassScope() {
		expressions = expressions.stream().filter(eInfo -> getExpressionScopeStart(eInfo.getExpression()) == 0).collect(Collectors.toList());
		expressions = expressions.stream().filter(eInfo -> eInfo.getExpression().toString().equals(expression.toString())
				|| expression.toString().contains(eInfo.getLeftSide().toString())).collect(Collectors.toList());
	}

	public List<ExpressionInfo> getExpressions() {
		return expressions;
	}

	private int getExpressionScopeStart(ASTNode node) {
		while (!(node instanceof MethodDeclaration || node == null))
			node = node.getParent();

		if (node == null)
			return 0;

		return compilationUnit.getLineNumber(node.getStartPosition());
	}
}
