package application.backend.service.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import application.backend.dto.ExpressionInfo;

public class VariableAssignmentVisitor extends ASTVisitor {

	private CompilationUnit compilationUnit;
	private Expression expression;
	private List<ExpressionInfo> expressions;
	private int expressionLineNumber;

	public VariableAssignmentVisitor(CompilationUnit compilationUnit, Expression expression) {
		this.compilationUnit = compilationUnit;
		this.expression = expression;
		this.expressions = new ArrayList<>();
		this.expressionLineNumber = compilationUnit.getLineNumber(expression.getStartPosition());
	}

	public void processCurrentExpression() {
		expressions.clear();
		compilationUnit.accept(this);
	}

	@Override
	public boolean visit(Assignment node) {

		int assignmentLineNumber = compilationUnit.getLineNumber(node.getStartPosition());
		int variableScopeEndLineNumber = getEndOfMethodDeclaration(expression);
		boolean isNodeInScope = assignmentLineNumber >= expressionLineNumber && assignmentLineNumber <= variableScopeEndLineNumber;
		if (isNodeInScope && node.getLeftHandSide().toString().equals(expression.toString())) {
			expressions.add(new ExpressionInfo(compilationUnit, expression.resolveTypeBinding().getBinaryName(), expression, node,
					assignmentLineNumber, null));
		}
		return true;
	}

	public int getEndOfMethodDeclaration(ASTNode node) {

		while (!(node instanceof MethodDeclaration || node == null))
			node = node.getParent();

		if (node instanceof MethodDeclaration)
			return compilationUnit.getLineNumber(node.getStartPosition() + node.getLength());

		return 0;
	}

	public List<ExpressionInfo> getExpressions() {
		return expressions;
	}

	public static Optional<ExpressionInfo> findVariableAssignment(ExpressionInfo resultExpressionInfo) {
		Optional<ExpressionInfo> result = Optional.empty();
		VariableAssignmentVisitor assignmentVisitor = new VariableAssignmentVisitor(resultExpressionInfo.getCompilationUnit(),
				resultExpressionInfo.getLeftSide());
		assignmentVisitor.processCurrentExpression();
		List<ExpressionInfo> assignmentExpressions = assignmentVisitor.getExpressions().stream().filter(exp -> {

			if (exp.getExpression() instanceof Assignment) {
				Assignment assignment = (Assignment) exp.getExpression();
				return assignment.getOperator().toString().equals("=");
			}

			return false;
		}).sorted().collect(Collectors.toList());
		if (!assignmentExpressions.isEmpty())
			result = Optional.of(assignmentExpressions.get(assignmentExpressions.size() - 1));
		return result;
	}
}
