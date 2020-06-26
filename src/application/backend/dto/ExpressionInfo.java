package application.backend.dto;

import java.io.File;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.PackageDeclaration;

public class ExpressionInfo implements Comparable<ExpressionInfo> {

	private CompilationUnit compilationUnit;
	private String expressionClass;
	private Expression leftSide;
	private Expression expression;
	private int declarationLineNumber;
	private ExpressionInfo parent;

	public ExpressionInfo(CompilationUnit compilationUnit, String expressionClass, Expression leftSide, Expression expression,
			int declarationLineNumber, ExpressionInfo parent) {
		super();
		this.compilationUnit = compilationUnit;
		this.expressionClass = expressionClass;
		this.leftSide = leftSide;
		this.expression = expression;
		this.declarationLineNumber = declarationLineNumber;
		this.parent = parent;
	}

	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public void setCompilationUnit(CompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}

	public String getExpressionClass() {
		return expressionClass;
	}

	public void setExpressionClass(String expressionClass) {
		this.expressionClass = expressionClass;
	}

	public Expression getLeftSide() {
		return leftSide;
	}

	public void setLeftSide(Expression leftSide) {
		this.leftSide = leftSide;
	}

	public Expression getExpression() {
		return expression;
	}

	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	public int getDeclarationLineNumber() {
		return declarationLineNumber;
	}

	public void setDeclarationLineNumber(int declarationLineNumber) {
		this.declarationLineNumber = declarationLineNumber;
	}

	public ExpressionInfo getParent() {
		return parent;
	}

	public void setParent(ExpressionInfo parent) {
		this.parent = parent;
	}

	public void setParentTree(List<ExpressionInfo> expressionUsages) {
		if (!expressionUsages.isEmpty()) {
			setParent(expressionUsages.get(0));
			for (int i = 0; i < expressionUsages.size() - 1; ++i)
				expressionUsages.get(i).setParent(expressionUsages.get(i + 1));
		}
	}

	@Override
	public String toString() {
		return "Line " + declarationLineNumber + ": " + expression.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;

		if (obj instanceof ExpressionInfo) {
			ExpressionInfo expressionInfo = (ExpressionInfo) obj;
			boolean areCompilationUnitsEqual = expressionInfo.getCompilationUnit().equals(compilationUnit);
			boolean areLineNumbersEqual = expressionInfo.getDeclarationLineNumber() == declarationLineNumber;

			return areCompilationUnitsEqual && areLineNumbersEqual;
		}

		return super.equals(obj);
	}

	@Override
	public int compareTo(ExpressionInfo expressionInfo) {
		if (expressionInfo == null)
			return 1;

		int expressionLineNumber = expressionInfo.getDeclarationLineNumber();
		if (declarationLineNumber > expressionLineNumber)
			return 1;
		else if (declarationLineNumber < expressionLineNumber)
			return -1;

		return 0;
	}

}
