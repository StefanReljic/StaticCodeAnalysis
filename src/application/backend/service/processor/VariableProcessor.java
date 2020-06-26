package application.backend.service.processor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public abstract class VariableProcessor implements ProcessVariable {

	protected CompilationUnit compilationUnit;
	protected Expression expression;

	public int getEndOfMethodDeclaration(ASTNode node) {

		while (!(node instanceof MethodDeclaration))
			node = node.getParent();

		if (node instanceof MethodDeclaration)
			return compilationUnit.getLineNumber(node.getStartPosition() + node.getLength());

		return -1;
	}
}
