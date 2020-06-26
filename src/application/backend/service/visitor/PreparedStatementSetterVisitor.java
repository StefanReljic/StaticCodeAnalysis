package application.backend.service.visitor;

import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import application.backend.dto.ExpressionInfo;
import application.backend.dto.types.ExpressionTypes;
import application.backend.dto.types.MethodNames;
import application.backend.dto.types.MethodSignatures;
import application.backend.service.utils.MethodUtils;

public class PreparedStatementSetterVisitor extends ASTVisitor {

	private CompilationUnit compilationUnit;
	private MethodUtils methodUtils;
	private ExpressionInfo expressionInfo;
	private int parentMethodEndLineNumber;
	private int numberOfSetters;
	private boolean executeMethodProcessed;

	public PreparedStatementSetterVisitor(ExpressionInfo expressionInfo) {
		this.compilationUnit = expressionInfo.getCompilationUnit();
		this.methodUtils = new MethodUtils(compilationUnit);
		this.expressionInfo = expressionInfo;
		this.numberOfSetters = 0;
		this.parentMethodEndLineNumber = getEndOfMethodDeclaration(expressionInfo.getLeftSide());
		this.executeMethodProcessed = false;
	}

	public void processCurrentExpression() {
		this.expressionInfo.getCompilationUnit().accept(this);
	}

	public int getNumberOfSetters() {
		return numberOfSetters;
	}

	@Override
	public boolean visit(MethodInvocation methodInvocation) {
		if (executeMethodProcessed)
			return false;

		int lineNumber = compilationUnit.getLineNumber(methodInvocation.getStartPosition());
		if (lineNumber >= expressionInfo.getDeclarationLineNumber() && lineNumber <= parentMethodEndLineNumber) {

			if (isSetterMethod(methodInvocation)) {
				numberOfSetters++;
			}
			String methodName = methodInvocation.getName().toString();
			executeMethodProcessed = methodName.equals(MethodNames.EXECUTE.getName()) || methodName.equals(MethodNames.EXECUTE_QUERY.getName())
					|| methodName.equals(MethodNames.EXECUTE_UPDATE.getName());
		}

		return false;
	}

	private int getEndOfMethodDeclaration(ASTNode node) {

		while (!(node instanceof MethodDeclaration || node == null))
			node = node.getParent();

		if (node instanceof MethodDeclaration)
			return compilationUnit.getLineNumber(node.getStartPosition() + node.getLength());

		return 0;
	}

	private boolean isSetterMethod(MethodInvocation methodInvocation) {

		IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();
		if (methodBinding != null) {
			String className = methodBinding.getDeclaringClass().getBinaryName();
			if (className.equals(ExpressionTypes.PREPARED_STATEMENT.getType())) {
				Optional<String> methodSignature = methodUtils.createMethodSignature(methodInvocation);
				if (methodSignature.isPresent()) {
					for (MethodSignatures signature : MethodSignatures.values())
						if (signature.getSignature().equals(methodSignature.get())) {
							return true;
						}
				}
			}
		}

		return false;
	}
}
