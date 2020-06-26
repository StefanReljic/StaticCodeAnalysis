package application.backend.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * Used for parsing java file passed as class path to constructor.
 * 
 * @author Reljic Stefan
 *
 */
public class ClassParser {

	private CompilationUnit compilationUnit;
	private ASTParser parser;
	private String projectRoot;
	private String className;

	public ClassParser(String projectRoot, String relativeClassPath) throws IOException {
		String sourceCode = getSourceCode(projectRoot, relativeClassPath);
		setupParser(sourceCode);
		setupCompilationUnit();
	}

	public ClassParser(CompilationUnit compilationUnit) {
		this.compilationUnit = compilationUnit;
	}

	private String getSourceCode(String projectRoot, String unitClassPath) throws IOException {
		this.projectRoot = projectRoot;
		String[] classPathElements = unitClassPath.split("\\\\");
		this.className = classPathElements[classPathElements.length - 1];
		File file = new File(projectRoot + File.separator + unitClassPath);
		if (file.exists())
			return Files.readAllLines(file.toPath()).stream().collect(Collectors.joining("\n"));
		file = new File(projectRoot + File.separator + "main\\java" + File.separator + unitClassPath);
		return Files.readAllLines(file.toPath()).stream().collect(Collectors.joining("\n"));

	}

	private void setupParser(String sourceCode) {
		parser = ASTParser.newParser(AST.JLS11);
		parser.setSource(sourceCode.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		File file = new File(projectRoot.split("src")[0] + "bin");
		if (file.exists())
			parser.setEnvironment(new String[] { projectRoot.split("src")[0] + "bin" }, null, null, true);
		file = new File(projectRoot.split("src")[0] + "target" + File.separator + "classes");
		if (file.exists())
			parser.setEnvironment(new String[] { projectRoot.split("src")[0] + "target" + File.separator + "classes" }, null, null, true);
		parser.setUnitName(className);
	}

	private void setupCompilationUnit() {
		compilationUnit = (CompilationUnit) parser.createAST(null);
	}

	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public String getProjectRoot() {
		return projectRoot;
	}
}
