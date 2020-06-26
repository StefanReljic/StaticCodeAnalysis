package application.backend.dto.types;

public enum Recommendation {
	
	USE_PARAMETRIZED_QUERY("You have string interpolation. Use parametrized queries in statement."), 
	USE_PARAMETRIZED_QUERY_FULLY("You have string interpolation. Fully implement parametrized queries."), 
	DONT_USE_STRING_FORMAT("You have string formatting. Avoid using String.format() method."), 
	DONT_USE_CONCATENATION("You have string concatenetion. Avoid concatenation and use parametrized queries");
	
	private String recomendation;

	Recommendation(String recomendation) {
		this.recomendation = recomendation;
	}

	public String getRecomendation() {
		return recomendation;
	}
}
