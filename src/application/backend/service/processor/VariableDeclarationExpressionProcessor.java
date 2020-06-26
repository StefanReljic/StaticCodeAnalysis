package application.backend.service.processor;

import java.util.Optional;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import application.backend.dto.ExpressionInfo;
import application.backend.dto.types.ExpressionTypes;

/**
 * Processes node which variable contains result of some expression.
 * 
 * e.g. Connection connection=C3P0DataSource.getInstance().getConnection()
 * PreparedStatement statement=connection.prepareStatement(query)
 * 
 * @author Reljic Stefan
 *
 */
public class VariableDeclarationExpressionProcessor extends VariableProcessor {

	public VariableDeclarationExpressionProcessor(CompilationUnit compilationUnit, Expression expression) {
		this.compilationUnit = compilationUnit;
		this.expression = expression;
	}

	@Override
	public Optional<ExpressionInfo> processNode(VariableDeclarationFragment node) {
		if (node == null || node.getInitializer() == null)
			return Optional.empty();

		if (node.getParent() instanceof VariableDeclarationExpression) {
			return processVariableDeclarationExpression(node);
		}
		return Optional.empty();
	}

	private Optional<ExpressionInfo> processVariableDeclarationExpression(VariableDeclarationFragment node) {
		VariableDeclarationExpression declarationExpression = (VariableDeclarationExpression) node.getParent();
		if (declarationExpression != null) {
			ITypeBinding typeBinding = declarationExpression.getType().resolveBinding();
			if (typeBinding != null) {
				String className = typeBinding.getBinaryName();
				boolean isPreparedStatementOrString = ExpressionTypes.PREPARED_STATEMENT.getType().equals(className)
						|| ExpressionTypes.STRING.getType().equals(className);
				if (isPreparedStatementOrString) {
					return getExpressionInfoFromDeclarationExpression(node, className);
				}
			}
		}
		return Optional.empty();
	}

	private Optional<ExpressionInfo> getExpressionInfoFromDeclarationExpression(VariableDeclarationFragment node, String className) {
		Expression initializer = node.getInitializer();
		int nodeLineNumber = compilationUnit.getLineNumber(node.getStartPosition());
		return Optional.of(new ExpressionInfo(compilationUnit, className, node.getName(), initializer, nodeLineNumber, null));
	}

}
