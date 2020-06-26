package application.backend.dto.types;

public enum MethodSignatures {

	SET_INT("setInt(I, I)"), SET_FLOAT("setFloat(I, F)"), SET_STRING("setString(I, java.lang.String)"), SET_OBJECT("setObject(I, java.lang.Object)"), 
	SET_BOOLEAN("setBoolean(I, Z)"), SET_BIG_DECIMAL("setBigDecimal(I, java.math.BigDecimal)"), SET_DATE("setDate(I, java.sql.Date)"), 
	SET_DOUBLE("setDouble(I, D)"), SET_BYTE("setByte(I, B)"), SET_BYTES("setBytes(I, [B)"), SET_LONG("setLong(I, J)");

	private String signature;

	MethodSignatures(String signature) {
		this.signature = signature;
	}

	public String getSignature() {
		return signature;
	}
}
