package application.backend.service.visitor;

import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.SimpleName;

import application.backend.dto.ExpressionInfo;
import application.backend.dto.types.ExpressionTypes;

public class InfixExpressionVisitor extends ASTVisitor {

	private CompilationUnit compilationUnit;
	private Expression expression;
	private int expressionLineNumber;
	private Optional<ExpressionInfo> expressionInfo;

	public InfixExpressionVisitor(CompilationUnit compilationUnit, Expression expression) {
		this.compilationUnit = compilationUnit;
		this.expression = expression;
		this.expressionLineNumber = compilationUnit.getLineNumber(expression.getStartPosition());
	}

	public void processCurrentExpression() {
		expressionInfo = Optional.empty();
		compilationUnit.accept(this);
	}

	@Override
	public boolean visit(SimpleName node) {
		int nodeLineNumber = compilationUnit.getLineNumber(node.getStartPosition());
		if (nodeLineNumber == expressionLineNumber) {
			expressionInfo = Optional
					.of(new ExpressionInfo(compilationUnit, ExpressionTypes.STRING.getType(), node, expression, nodeLineNumber, null));
			return true;
		}
		return false;
	}

	public Optional<ExpressionInfo> getExpressionInfo() {
		return expressionInfo;
	}
}
