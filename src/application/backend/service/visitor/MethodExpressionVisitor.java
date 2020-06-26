package application.backend.service.visitor;

import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

import application.backend.dto.ExpressionInfo;
import application.backend.dto.types.ExpressionTypes;
import application.backend.dto.types.MethodNames;
import application.backend.service.ClassParser;
import application.backend.service.finder.Finder;
import application.backend.service.finder.VariableDeclarationFinder;
import application.backend.service.utils.MethodUtils;

public class MethodExpressionVisitor extends ASTVisitor {

	private ClassParser classParser;
	private CompilationUnit compilationUnit;
	private MethodUtils methodUtils;
	private Optional<ExpressionInfo> resultExpression;
	private IMethodBinding methodBinding;
	private Optional<String> expressionMethodSignature;

	public MethodExpressionVisitor(ClassParser classParser, Expression expression) {
		super();
		this.classParser = classParser;
		this.compilationUnit = classParser.getCompilationUnit();
		this.methodUtils = new MethodUtils(compilationUnit);
		this.resultExpression = Optional.empty();
		this.methodBinding = ((MethodInvocation) expression).resolveMethodBinding();
		this.expressionMethodSignature = methodUtils.createMethodSignature(expression);
	}

	public void processCurrentExpression() {
		this.resultExpression = Optional.empty();
		compilationUnit.accept(this);
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		IMethodBinding nodeMethodBinding = node.resolveBinding();
		String nodeMethodDeclaration = nodeMethodBinding.getMethodDeclaration().toString();
		boolean isMatchedMethodDeclarationInDifferentCompilationUnit = expressionMethodSignature.isPresent()
				&& nodeMethodDeclaration.contains(expressionMethodSignature.get());
		boolean isMatchedMethodDeclarationInCurrentCompilationUnit = methodBinding != null && methodBinding.equals(nodeMethodBinding);

		if (isMatchedMethodDeclarationInDifferentCompilationUnit || isMatchedMethodDeclarationInCurrentCompilationUnit) {
			Expression expression = getReturnStatementExpression(node);
			VariableDeclarationFinder declarationFinder = new VariableDeclarationFinder(classParser);
			if (expression instanceof MethodInvocation) {
				processReturnStatementAsMethod(expression, declarationFinder);
			} else {
				processReturnStatementAsVariable(expression, declarationFinder);
			}
			return true;
		}
		return false;
	}

	private Expression getReturnStatementExpression(MethodDeclaration node) {
		List<Statement> statements = node.getBody().statements();
		ReturnStatement returnStatement = (ReturnStatement) statements.get(statements.size() - 1);
		return returnStatement.getExpression();
	}

	private void processReturnStatementAsMethod(Expression expression, VariableDeclarationFinder declarationFinder) {
		MethodInvocation methodInvocation = (MethodInvocation) expression;
		IMethodBinding binding = methodInvocation.resolveMethodBinding();
		boolean isJarMethod = new MethodUtils(compilationUnit).isJarMethod(methodInvocation);
		if (isJarMethod) {
			processJarMethod(declarationFinder, methodInvocation, binding);
		} else {
			processProjectMethod(expression, declarationFinder);
		}
	}

	private void processProjectMethod(Expression expression, VariableDeclarationFinder declarationFinder) {
		int declarationLineNumber = compilationUnit.getLineNumber(expression.getStartPosition());
		ExpressionInfo expressionInfo = new ExpressionInfo(compilationUnit, expression.getClass().toString(), null, expression, declarationLineNumber,
				null);
		Optional<ExpressionInfo> methodProcessingResult = declarationFinder.findExpressionDeclaration(expression);
		if (methodProcessingResult.isPresent()) {
			expressionInfo.setParent(methodProcessingResult.get());
		}
		this.resultExpression = Optional.of(expressionInfo);
	}

	private void processJarMethod(VariableDeclarationFinder declarationFinder, MethodInvocation methodInvocation, IMethodBinding binding) {
		String methodClassName = binding.getDeclaringClass().getBinaryName();
		String methodName = methodInvocation.getName().toString();
		boolean isJavaSqlConnectionClass = ExpressionTypes.CONNECTION.getType().equals(methodClassName);
		boolean isPreparedStatement = MethodNames.PREPARE_STATEMENT.getName().equals(methodName);

		if (isJavaSqlConnectionClass && isPreparedStatement) {
			Expression sqlExpression = (Expression) methodInvocation.arguments().get(0);
			Optional<ExpressionInfo> sqlExpressionDeclaration = declarationFinder.findExpressionDeclaration(sqlExpression);
			if (sqlExpressionDeclaration.isPresent()) {
				Finder expressionFinder = new Finder(new ClassParser(compilationUnit));
				ExpressionInfo sqlExpressionInfo = sqlExpressionDeclaration.get();
				if (sqlExpressionInfo.getExpression() instanceof MethodInvocation) {
					ExpressionInfo result = expressionFinder.generateExpressionTree(sqlExpressionInfo);
					if (result != null) {
						sqlExpressionInfo = result;
					}
				} else {
					sqlExpressionInfo = expressionFinder.generateExpressionTree(sqlExpressionInfo);
				}
				this.resultExpression = Optional.of(sqlExpressionInfo);
			}
		}
	}

	private void processReturnStatementAsVariable(Expression expression, VariableDeclarationFinder declarationFinder) {
		Optional<ExpressionInfo> optionalExpressionInfo = declarationFinder.findExpressionDeclaration(expression);
		if (optionalExpressionInfo.isPresent()) {
			ExpressionInfo expressionInfo = optionalExpressionInfo.get();
			Finder expressionFinder = new Finder(new ClassParser(compilationUnit));
			expressionInfo = expressionFinder.generateExpressionTree(expressionInfo);
			this.resultExpression = Optional.of(expressionInfo);
		}
	}

	public Optional<ExpressionInfo> getResultExpression() {
		return resultExpression;
	}
}
