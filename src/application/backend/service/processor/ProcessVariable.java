package application.backend.service.processor;

import java.util.Optional;

import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import application.backend.dto.ExpressionInfo;

public interface ProcessVariable {

	public Optional<ExpressionInfo> processNode(VariableDeclarationFragment node);
}
