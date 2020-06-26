package application.backend.service.finder;

import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;

import application.backend.dto.ExpressionInfo;
import application.backend.dto.types.ExpressionTypes;
import application.backend.service.ClassParser;
import application.backend.service.utils.ExpressionUtils;
import application.backend.service.utils.MethodUtils;
import application.backend.service.visitor.InfixExpressionVisitor;
import application.backend.service.visitor.MethodExpressionVisitor;
import application.backend.service.visitor.VariableAssignmentVisitor;
import application.backend.service.visitor.VariableDeclarationVisitor;

public class VariableDeclarationFinder {

	private ClassParser classParser;
	private CompilationUnit compilationUnit;
	private ExpressionUtils expressionUtils;
	private MethodUtils methodUtils;

	public VariableDeclarationFinder(ClassParser classParser) {
		this.classParser = classParser;
		this.compilationUnit = classParser.getCompilationUnit();
		this.expressionUtils = new ExpressionUtils(classParser);
		this.methodUtils = new MethodUtils(compilationUnit);
	}

	@SuppressWarnings("unchecked")
	public Optional<ExpressionInfo> findExpressionDeclaration(Expression expression) {
		if (expression == null)
			return Optional.empty();

		if (expression instanceof InfixExpression)
			return findInfixExpressionDeclaration(expression);

		if (expression instanceof MethodInvocation) {
			if (!methodUtils.isJarMethod((MethodInvocation) expression)) {
				return findExpressionDeclarationInMethod(expression);
			} else {
				List<Expression> arguments = ((MethodInvocation) expression).arguments();
				if (!arguments.isEmpty()) {
					if (arguments.get(0) instanceof MethodInvocation) {
						return findExpressionDeclarationInMethod(arguments.get(0));
					} else {
						return findOtherExpressionDeclaration(arguments.get(0));
					}
				}
			}
		}

		return findOtherExpressionDeclaration(expression);
	}

	private Optional<ExpressionInfo> findOtherExpressionDeclaration(Expression expression) {
		if (expression == null)
			return Optional.empty();
		VariableDeclarationVisitor declarationVisitor = new VariableDeclarationVisitor(compilationUnit, expression);
		declarationVisitor.processCurrentExpression();
		return declarationVisitor.getExpressions().stream().max((x, y) -> x.compareTo(y));
	}

	private Optional<ExpressionInfo> findInfixExpressionDeclaration(Expression expression) {
		if (expression == null)
			return Optional.empty();

		InfixExpressionVisitor declarationVisitor = new InfixExpressionVisitor(compilationUnit, expression);
		declarationVisitor.processCurrentExpression();
		return declarationVisitor.getExpressionInfo();
	}

	private Optional<ExpressionInfo> findExpressionDeclarationInMethod(Expression expression) {
		if (!(expression instanceof MethodInvocation))
			return Optional.empty();

		Optional<ExpressionInfo> result = Optional.empty();
		MethodInvocation methodInvocation = (MethodInvocation) expression;
		Optional<MethodExpressionVisitor> methodExpressionVisitor = getMethodExpressionVisitor(methodInvocation);
		if (methodExpressionVisitor.isPresent()) {
			methodExpressionVisitor.get().processCurrentExpression();
			result = methodExpressionVisitor.get().getResultExpression();
		}
		return result;
	}

	/**
	 * 
	 * @param expression
	 * @return
	 */
	private Optional<MethodExpressionVisitor> getMethodExpressionVisitor(MethodInvocation methodInvocation) {
		if (methodUtils.isMethodFromAnotherClassInProject(methodInvocation)) {
			Optional<ClassParser> classParser = expressionUtils.getExpressionClassParser(this.classParser, methodInvocation.getExpression());
			if (classParser.isPresent()) {
				return Optional.of(new MethodExpressionVisitor(classParser.get(), methodInvocation));
			}
		}
		return Optional.of(new MethodExpressionVisitor(classParser, methodInvocation));
	}

	public void findNextAssignmentOfExpression(ExpressionInfo expressionInfo) {
		Optional<ExpressionInfo> assignmentExpressionInfo = VariableAssignmentVisitor.findVariableAssignment(expressionInfo);

		if (assignmentExpressionInfo.isPresent()) {
			Expression expression = assignmentExpressionInfo.get().getExpression();
			if (expression instanceof Assignment) {
				Assignment assignment = (Assignment) expression;
				if (assignment.getRightHandSide() instanceof MethodInvocation) {
					ExpressionInfo methodExpressionInfo = new ExpressionInfo(classParser.getCompilationUnit(), ExpressionTypes.STRING.getType(),
							assignment.getLeftHandSide(), assignment.getRightHandSide(),
							classParser.getCompilationUnit().getLineNumber(assignment.getStartPosition()), null);

					MethodInvocation methodInvocation = (MethodInvocation) assignment.getRightHandSide();
					VariableDeclarationFinder declarationFinder = new VariableDeclarationFinder(classParser);
					Optional<ExpressionInfo> argumentDeclaration = declarationFinder
							.findExpressionDeclaration((Expression) methodInvocation.arguments().get(0));
					if (argumentDeclaration.isPresent()) {
						methodExpressionInfo.setParent(argumentDeclaration.get());
						expressionInfo.setParent(methodExpressionInfo);
					} else {
						expressionInfo.setParent(assignmentExpressionInfo.get());
					}
				}
			}
		}
	}

}
