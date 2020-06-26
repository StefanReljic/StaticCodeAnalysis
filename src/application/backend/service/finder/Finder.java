package application.backend.service.finder;

import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import application.backend.dto.ExpressionInfo;
import application.backend.dto.types.ExpressionTypes;
import application.backend.dto.types.MethodNames;
import application.backend.service.ClassParser;
import application.backend.service.utils.MethodUtils;
import application.backend.service.visitor.ExecuteMethodVisitor;
import application.backend.service.visitor.VariableAssignmentVisitor;

public class Finder {

	private ClassParser classParser;
	private VariableDeclarationFinder expressionDeclarationFinder;
	private MethodUtils methodUtils;

	public Finder(ClassParser classParser) {
		this.classParser = classParser;
		this.expressionDeclarationFinder = new VariableDeclarationFinder(classParser);
		this.methodUtils = new MethodUtils(classParser.getCompilationUnit());
	}

	/**
	 * Finds all execute method expressions.
	 * 
	 * @return List of references or method arguments that contains SQL queries.
	 */
	public List<ExpressionInfo> findExecuteMethodExpressions() {
		ExecuteMethodVisitor executeMethodVisitor = new ExecuteMethodVisitor(classParser, expressionDeclarationFinder);
		return executeMethodVisitor.findExecuteMethodExpressions();
	}

	/**
	 * Creates expression tree for current expression info. Parent of current
	 * expression info can be declaration of variable or variable usage.
	 * 
	 * 
	 * @param expressionInfo
	 * @return
	 */
	public ExpressionInfo generateExpressionTree(ExpressionInfo expressionInfo) {
		ASTVisitor visitor = new ASTVisitor() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean visit(VariableDeclarationFragment node) {

				if (expressionInfo.getExpression() instanceof MethodInvocation) {
					List<Expression> arguments = ((MethodInvocation) expressionInfo.getExpression()).arguments();
					if (!arguments.isEmpty() && arguments.get(0) instanceof StringLiteral) {
						Optional<ExpressionInfo> result = createStringLiteralExpressionInfo(expressionInfo.getExpression());
						if (result.isPresent()) {
							expressionInfo.setParent(result.get());
							return true;
						}
					}
				}

				int nodeLineNumber = classParser.getCompilationUnit().getLineNumber(node.getStartPosition());
				if (expressionInfo.getDeclarationLineNumber() == nodeLineNumber) {
					Expression currentNodeRightSide = node.getInitializer();
					Optional<ExpressionInfo> currentRightSideExpressionDeclaration = expressionDeclarationFinder
							.findExpressionDeclaration(expressionInfo.getExpression());
					// ako je desna strana string
					if (currentNodeRightSide instanceof StringLiteral) {
						return processStringLiteral(expressionInfo, node);
					}

					// ako je poziv metode iz druge CompilationUnit
					boolean isMethodInvocation = expressionInfo.getExpression() instanceof MethodInvocation;
					boolean isMethodFromAnotherClassInProject = isMethodInvocation
							&& !methodUtils.isJarMethod((MethodInvocation) expressionInfo.getExpression());
					if (isMethodFromAnotherClassInProject) {
						if (currentRightSideExpressionDeclaration.isPresent()) {
							expressionInfo.setParent(currentRightSideExpressionDeclaration.get());
						}
						return true;
					}

					if (currentRightSideExpressionDeclaration.isPresent()) {
						ExpressionInfo currentExpressionInfo = currentRightSideExpressionDeclaration.get();

						/*
						 * ako je desna strana poziv metode, ovde moze biti samo preparedStatement ako
						 * je neka druga metoda, obradjena je u definisanju
						 * currentRightSideExpressionDeclaration
						 */
						if (currentNodeRightSide instanceof MethodInvocation) {
							return processPreparedStatement(expressionInfo, currentNodeRightSide);
						}

						// desna strana je druga referenca
						if (currentNodeRightSide instanceof SimpleName) {
							return processSimpleName(expressionInfo, currentExpressionInfo);
						}

						// izraz sadrzi neki oblik konkatenacije
						if (currentNodeRightSide instanceof InfixExpression) {
							return processInfixExpression(expressionInfo, node);
						}
					}
				}
				return true;
			}

			@SuppressWarnings("unchecked")
			private Optional<ExpressionInfo> createStringLiteralExpressionInfo(Expression expression) {
				Optional<ExpressionInfo> result = Optional.empty();
				if (expression instanceof MethodInvocation) {
					MethodInvocation methodInvocation = (MethodInvocation) expression;
					List<Expression> arguments = methodInvocation.arguments();
					if (!arguments.isEmpty()) {
						int declarationLineNumber = classParser.getCompilationUnit().getLineNumber(expression.getStartPosition());
						ExpressionInfo expressionInfo = new ExpressionInfo(classParser.getCompilationUnit(), ExpressionTypes.STRING.getType(),
								expression, arguments.get(0), declarationLineNumber, null);
						result = Optional.of(expressionInfo);
					}
				}
				return result;
			}

			private boolean processPreparedStatement(ExpressionInfo expressionInfo, Expression currentRightSideExpression) {
				MethodInvocation methodInvocation = (MethodInvocation) currentRightSideExpression;
				IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
				if (methodBinding != null) {
					ITypeBinding typeBinding = methodBinding.getDeclaringClass();
					if (typeBinding != null) {
						String className = typeBinding.getQualifiedName();
						boolean isPreparedStatement = ExpressionTypes.CONNECTION.getType().equals(className)
								&& MethodNames.PREPARE_STATEMENT.getName().equals(methodInvocation.getName().toString());
						if (isPreparedStatement) {
							ITypeBinding[] typeBindings = methodBinding.getParameterTypes();
							Expression expression = (Expression) methodInvocation.arguments().get(0);
							String argumentClass = typeBindings[0].getBinaryName();
							if (ExpressionTypes.STRING.getType().equals(argumentClass)) {
								Optional<ExpressionInfo> argumentExpressionDeclaration = expressionDeclarationFinder
										.findExpressionDeclaration(expression);

								if (argumentExpressionDeclaration.isPresent()) {
									ExpressionInfo parent = generateExpressionTree(argumentExpressionDeclaration.get());
									expressionInfo.setParent(parent);
									return true;
								}
							}
						}
					}
				}
				return false;
			}

			private boolean processInfixExpression(ExpressionInfo expressionInfo, VariableDeclarationFragment node) {
				VariableAssignmentVisitor expressionVisitor = new VariableAssignmentVisitor(classParser.getCompilationUnit(), node.getName());
				expressionVisitor.processCurrentExpression();
				List<ExpressionInfo> expressionUsages = expressionVisitor.getExpressions();
				expressionInfo.setParentTree(expressionUsages);
				return true;
			}

			private boolean processSimpleName(ExpressionInfo expressionInfo, ExpressionInfo currentExpressionInfo) {
				expressionInfo.setParent(generateExpressionTree(currentExpressionInfo));
				return true;
			}

			private boolean processStringLiteral(ExpressionInfo expressionInfo, VariableDeclarationFragment node) {
				VariableAssignmentVisitor expressionVisitor = new VariableAssignmentVisitor(classParser.getCompilationUnit(), node.getName());
				expressionVisitor.processCurrentExpression();
				List<ExpressionInfo> expressionUsages = expressionVisitor.getExpressions();
				expressionInfo.setParentTree(expressionUsages);
				return true;
			}
		};
		classParser.getCompilationUnit().accept(visitor);

		return expressionInfo;
	}

}
