package application.backend.service.processor;

import java.util.Optional;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import application.backend.dto.ExpressionInfo;
import application.backend.dto.types.ExpressionTypes;

/**
 * Processes node which variable contains result of some statement.
 * 
 * e.g. Blob blob=getBlobFromBytes(book.getPhoto() == null ? null :
 * book.getPhoto().getBytes()); List<StoreBookDetails> storeBookDetails=new
 * ArrayList<>();
 * 
 * @author Reljic Stefan
 *
 */
public class VariableDeclarationStatementProcessor extends VariableProcessor {

	public VariableDeclarationStatementProcessor(CompilationUnit compilationUnit, Expression expression) {
		this.compilationUnit = compilationUnit;
		this.expression = expression;
	}

	@Override
	public Optional<ExpressionInfo> processNode(VariableDeclarationFragment node) {
		if (node == null || node.getInitializer() == null)
			return Optional.empty();

		if (node.getParent() instanceof VariableDeclarationStatement) {
			return processVariableDeclarationStatement(node);
		}
		return Optional.empty();
	}

	private Optional<ExpressionInfo> processVariableDeclarationStatement(VariableDeclarationFragment node) {
		VariableDeclarationStatement declarationExpression = (VariableDeclarationStatement) node.getParent();
		if (declarationExpression != null) {
			ITypeBinding typeBinding = declarationExpression.getType().resolveBinding();
			if (typeBinding != null) {
				String className = typeBinding.getBinaryName();
				boolean isStatementClassString = ExpressionTypes.STRING.getType().equals(className);
				if (isStatementClassString) {
					return getExpressionInfoFromStringStatement(node, className);
				}
			}
		}
		return Optional.empty();
	}

	private Optional<ExpressionInfo> getExpressionInfoFromStringStatement(VariableDeclarationFragment node, String className) {
		Expression initializer = node.getInitializer();
		int nodeLineNumber = compilationUnit.getLineNumber(node.getStartPosition());
		return Optional.of(new ExpressionInfo(compilationUnit, className, node.getName(), initializer, nodeLineNumber, null));
	}

}
