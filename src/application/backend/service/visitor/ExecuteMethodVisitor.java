package application.backend.service.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import application.backend.dto.ExpressionInfo;
import application.backend.dto.types.ExpressionTypes;
import application.backend.dto.types.MethodNames;
import application.backend.service.ClassParser;
import application.backend.service.finder.VariableDeclarationFinder;

public class ExecuteMethodVisitor extends ASTVisitor {

	private ClassParser classParser;
	private VariableDeclarationFinder expressionDeclarationFinder;
	private List<ExpressionInfo> expressions;

	public ExecuteMethodVisitor(ClassParser classParser, VariableDeclarationFinder expressionDeclarationFinder) {
		super();
		this.classParser = classParser;
		this.expressionDeclarationFinder = expressionDeclarationFinder;
		this.expressions = new ArrayList<>();
	}

	public List<ExpressionInfo> findExecuteMethodExpressions() {
		expressions.clear();
		classParser.getCompilationUnit().accept(this);
		return expressions;
	}

	/**
	 * Finds all expression references. If execute method contains parameters, first
	 * parameter must be String as SQL query and it is added into list. If execute
	 * method does not contain parameters, Statement reference is put into the list.
	 * 
	 * @return List of used expressions.
	 */
	@Override
	public boolean visit(MethodInvocation method) {
		String methodName = method.getName().getIdentifier();
		if (MethodNames.isStatementExecuteMethod(methodName)) {
			IMethodBinding methodBinding = method.resolveMethodBinding();
			if (methodBinding != null) {
				ITypeBinding declaringClass = methodBinding.getDeclaringClass();
				if (declaringClass != null) {
					String methodParrentClass = methodBinding.getDeclaringClass().getQualifiedName();
					if (ExpressionTypes.isStatementClass(methodParrentClass)) {
						Optional<ExpressionInfo> newExpressionInfo = extractExpressionFromStatementMethod(method);
						if (newExpressionInfo.isPresent()) {
							expressions.add(newExpressionInfo.get());
						}
					}
				}
			}
		}
		return true;
	}

	private Optional<ExpressionInfo> extractExpressionFromStatementMethod(MethodInvocation method) {
		IMethodBinding methodBinding = method.resolveMethodBinding();
		if (methodBinding.isParameterizedMethod()) {
			return extractArgumentFromParametrizedStatement(method, methodBinding);
		} else {
			return expressionDeclarationFinder.findExpressionDeclaration(method.getExpression());
		}
	}

	private Optional<ExpressionInfo> extractArgumentFromParametrizedStatement(MethodInvocation method, IMethodBinding methodBinding) {
		IMethodBinding methodDeclaration = methodBinding.getMethodDeclaration();
		ITypeBinding[] typeBindings = methodDeclaration.getParameterTypes();
		String expressionClass = typeBindings[0].getBinaryName();
		Expression expression = (Expression) method.arguments().get(0);
		int declarationLineNumber = classParser.getCompilationUnit().getLineNumber(method.getStartPosition());
		if (expressionClass.equals(ExpressionTypes.STRING.getType())) {
			return Optional
					.of(new ExpressionInfo(classParser.getCompilationUnit(), expressionClass, expression, expression, declarationLineNumber, null));
		}
		return Optional.empty();
	}
}
