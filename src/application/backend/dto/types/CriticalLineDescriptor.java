package application.backend.dto.types;

public enum CriticalLineDescriptor {

	STRING_INTERPOLATION("Current line contains string interpolation"), 
	STRING_FORMAT("Current line contains string formatting"), 
	STRING_CONCATENATION("Current line contains string concatenation");

	private String descriptor;

	private CriticalLineDescriptor(String descriptor) {
		this.descriptor = descriptor;
	}

	public String getDescriptor() {
		return descriptor;
	}
}
