package application.backend.service.processor;

import java.util.Optional;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import application.backend.dto.ExpressionInfo;
import application.backend.dto.types.ExpressionTypes;

public class FieldDeclarationProcessor extends VariableProcessor {

	public FieldDeclarationProcessor(CompilationUnit compilationUnit, Expression expression) {
		this.compilationUnit = compilationUnit;
		this.expression = expression;
	}

	@Override
	public Optional<ExpressionInfo> processNode(VariableDeclarationFragment node) {
		if (node == null || node.getParent() == null || node.getInitializer() == null) {
			return Optional.empty();
		}
		if (node.getParent() instanceof FieldDeclaration) {
			return processFieldDeclaration(node);
		}
		return Optional.empty();
	}

	private Optional<ExpressionInfo> processFieldDeclaration(VariableDeclarationFragment node) {
		FieldDeclaration declarationExpression = (FieldDeclaration) node.getParent();
		if (declarationExpression != null) {
			ITypeBinding typeBinding = declarationExpression.getType().resolveBinding();
			if (typeBinding != null) {
				String className = typeBinding.getBinaryName();
				boolean isPreparedStatementOrString = ExpressionTypes.PREPARED_STATEMENT.getType().equals(className)
						|| ExpressionTypes.STRING.getType().equals(className);
				if (isPreparedStatementOrString) {
					return getExpressionInfoFromFieldDeclaration(node, className);
				}
			}
		}
		return Optional.empty();
	}

	private Optional<ExpressionInfo> getExpressionInfoFromFieldDeclaration(VariableDeclarationFragment node, String className) {
		int nodeLineNumber = compilationUnit.getLineNumber(node.getStartPosition());
		Expression initializer = node.getInitializer();
		return Optional.of(new ExpressionInfo(compilationUnit, className, node.getName(), initializer, nodeLineNumber, null));
	}

}
