package application.backend.service.utils;

import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodUtils {

	private CompilationUnit compilationUnit;

	public MethodUtils(CompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}

	/**
	 * If node is null or node is not instance of MethodInvocation, returns false.
	 * If node IMethodBinding is null then method is part of another class and
	 * method returns true. If current method binding can be found in current class,
	 * method returns false.
	 * 
	 * @param node
	 * @return true or false, if method is part of another class in project
	 */
	public boolean isMethodFromAnotherClassInProject(MethodInvocation methodInvocation) {
		if (methodInvocation == null)
			return false;

		IMethodBinding binding = methodInvocation.resolveMethodBinding();
		if (binding != null) {
			return compilationUnit.findDeclaringNode(binding) == null;
		}
		return true;
	}

	/**
	 * Checks if current node is instance of MethodInvocation. If not method returns
	 * false.
	 * 
	 * If node is instance of MethodInvocation and node IMethodBinding is null, that
	 * means that method is part of another class in project.
	 * 
	 * If IMethodBinding is not null and current CompilationUnit package is not
	 * equal to method CompilationUnit package then method returns true.
	 * 
	 * @param node
	 * @return true or false, if node contains call of JAR method or not
	 */
	public boolean isJarMethod(MethodInvocation methodInvocation) {
		if (methodInvocation == null) {
			return false;
		}
		IMethodBinding binding = methodInvocation.resolveMethodBinding();
		if (binding == null) {
			return false;
		}
		String bindingClassPackage = binding.getDeclaringClass().getPackage().toString();
		String compilationUnitPackage = compilationUnit.getPackage().toString().split(";")[0];

		return !bindingClassPackage.equals(compilationUnitPackage);
	}

	@SuppressWarnings("unchecked")
	public Optional<String> createMethodSignature(Expression expression) {
		if (!(expression instanceof MethodInvocation))
			return Optional.empty();

		MethodInvocation methodInvocation = (MethodInvocation) expression;
		if (!isExpressionInCompilationUnit(expression) || isJarMethod(methodInvocation)) {
			String methodSignature = methodInvocation.getName().toString() + "(";
			List<Expression> arguments = methodInvocation.arguments();
			for (Expression argument : arguments) {
				ITypeBinding typeBinding = argument.resolveTypeBinding();
				if (typeBinding == null)
					return Optional.empty();
				methodSignature += typeBinding.getBinaryName() + ", ";
			}

			if (methodSignature.trim().endsWith("("))
				methodSignature += ")";
			else
				methodSignature = methodSignature.substring(0, methodSignature.length() - 2) + ")";

			return Optional.of(methodSignature);
		}

		return Optional.empty();
	}

	private boolean isExpressionInCompilationUnit(Expression expression) {
		ASTNode parent = expression.getParent();
		while (!(parent instanceof CompilationUnit))
			parent = parent.getParent();

		CompilationUnit cUnit = (CompilationUnit) parent;

		return cUnit.equals(compilationUnit);
	}
}
