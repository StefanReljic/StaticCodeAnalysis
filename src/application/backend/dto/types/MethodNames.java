package application.backend.dto.types;

public enum MethodNames {

	EXECUTE("execute"), EXECUTE_QUERY("executeQuery"), EXECUTE_UPDATE("executeUpdate"), PREPARE_STATEMENT("prepareStatement"), FORMAT(
			"format"), CONCAT("concat");

	private String name;

	MethodNames(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static boolean isStatementExecuteMethod(String methodName) {
		return MethodNames.EXECUTE.getName().equals(methodName) || MethodNames.EXECUTE_QUERY.getName().equals(methodName)
				|| MethodNames.EXECUTE_UPDATE.getName().equals(methodName);
	}
}
