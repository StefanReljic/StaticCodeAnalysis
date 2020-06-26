package application.backend.service.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;

import application.backend.service.ClassParser;

public class ExpressionUtils {

	private ClassParser classParser;
	private CompilationUnit compilationUnit;

	public ExpressionUtils(ClassParser classParser) {
		this.classParser = classParser;
		this.compilationUnit = classParser.getCompilationUnit();
	}

	/**
	 * Returns relative path of class that is type of expression.
	 * 
	 * @param expression
	 * @return classpath
	 */
	@SuppressWarnings("unchecked")
	public Optional<String> getExpressionClassPath(Expression expression) {
		if (expression == null)
			return Optional.empty();

		ITypeBinding typeBinding = expression.resolveTypeBinding();
		String className = null;
		// ako je typeBinding null tada je rijec o statickom pozivu funkcije
		if (typeBinding != null)
			className = typeBinding.getQualifiedName();
		else
			className = expression.toString();

		File file = new File(this.classParser.getProjectRoot() + File.separator + className.replace(".", "\\") + ".java");
		if (file.exists()) {
			String classPath = className.replace(".", "\\");
			return Optional.of(classPath + ".java");
		}

		file = new File(this.classParser.getProjectRoot() + File.separator + "main\\java" + File.separator + className.replace(".", "\\") + ".java");
		if (file.exists()) {
			String classPath = className.replace(".", "\\");
			return Optional.of(classPath + ".java");
		}

		Optional<String> inCurrentPackage = isClassInCurrentPackage(className);
		if (inCurrentPackage.isPresent()) {
			return inCurrentPackage;
		}

		return getClassPathFromImports(className);
	}

	private Optional<String> getClassPathFromImports(String className) {
		String[] parts = className.split("\\.");
		className = parts[parts.length - 1];
		List<ImportDeclaration> imports = compilationUnit.imports();
		for (ImportDeclaration importDeclaration : imports) {
			if (importDeclaration.toString().contains(className + ";")) {
				String packagePath = importDeclaration.toString().split("import")[1].trim();
				packagePath = packagePath.substring(0, packagePath.length() - 1);
				return Optional.of(packagePath.trim().replace(".", "\\") + ".java");
			}
		}
		return Optional.empty();
	}

	private Optional<String> isClassInCurrentPackage(String className) {
		File file;
		// ako className ne sadrzi tacku, a prethodno file ne postoji, to znaci da je
		// trenutno naziv klase kao className. Provjera da li je klasa cije je ime u
		// className u istom paketu kao
		// i trenutna compilationUnit
		if (!className.contains(".")) {
			String compilationUnitPackage = compilationUnit.getPackage().toString();
			compilationUnitPackage = compilationUnitPackage.split("package")[1].trim();
			compilationUnitPackage = compilationUnitPackage.substring(0, compilationUnitPackage.length() - 1);
			className = compilationUnitPackage + "." + className;
			file = new File(this.classParser.getProjectRoot() + File.separator + className.replace(".", "\\") + ".java");
			if (file.exists()) {
				String classPath = className.replace(".", "\\");
				return Optional.of(classPath + ".java");
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns CompilationUnit that contains called method declaration.
	 * 
	 * @param methodInvocation
	 * @return
	 */
	public Optional<ClassParser> getExpressionClassParser(ClassParser oldClassParser, Expression expression) {
		if (expression == null) {
			return Optional.empty();
		}
		Optional<String> classPath = getExpressionClassPath(expression);
		if (classPath.isPresent()) {
			try {
				ClassParser newClassParser = new ClassParser(oldClassParser.getProjectRoot(), classPath.get());
				return Optional.of(newClassParser);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return Optional.empty();
	}
}
