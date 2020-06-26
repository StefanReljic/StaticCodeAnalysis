package application.backend.dto.types;

public enum ExpressionTypes {

	PREPARED_STATEMENT("java.sql.PreparedStatement"), STATEMENT("java.sql.Statement"), STRING("java.lang.String"), CONNECTION("java.sql.Connection");

	private String type;

	private ExpressionTypes(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public static boolean isStatementClass(String className) {
		return PREPARED_STATEMENT.getType().equals(className) || STATEMENT.getType().equals(className);
	}
}
