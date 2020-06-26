package application.backend.service.utils;

import java.io.File;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;

public class CompilationUnitUtils {

	public static String getClassPath(String projectRoot, String relativePath) {
		String path1 = projectRoot + File.separator + relativePath;
		String path2 = projectRoot + File.separator + "main\\java" + File.separator + relativePath;
		File file = new File(path1);
		if (file.exists()) {
			return path1;
		}
		return path2;
	}

	public static String getCompilationUnitName(CompilationUnit compilationUnit) {
		if (compilationUnit == null) {
			return null;
		}
		String compilationUnitText = compilationUnit.toString();
		String[] parts = compilationUnitText.split("public");
		if (parts.length != 0) {
			String part = parts[1].trim();
			String[] classSplitParts = part.split("class");
			if (classSplitParts.length != 0) {
				String classString = classSplitParts[1].trim();
				return classString.split(" ")[0] + ".java";
			}
		}
		return null;
	}

	public static String getCompiltaionUnitClassPath(CompilationUnit compilationUnit) {
		if (compilationUnit == null) {
			return null;
		}
		PackageDeclaration packageDeclaration = compilationUnit.getPackage();
		return packageDeclaration.getName().toString().replace(".", "\\") + File.separator + getCompilationUnitName(compilationUnit);
	}

}
