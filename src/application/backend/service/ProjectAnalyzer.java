package application.backend.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.CompilationUnit;

import application.backend.dto.CriticalLine;
import application.backend.dto.CriticalLineRecommendation;
import application.backend.dto.ExpressionInfo;
import application.backend.service.utils.CompilationUnitUtils;
import application.backend.service.visitor.PreparedStatementSetterVisitor;
import javafx.concurrent.Task;

public class ProjectAnalyzer extends Task<Void> {

	private String projectRoot;
	private List<CriticalLineRecommendation> criticalLineRecommendations;

	public ProjectAnalyzer(String projectRoot) {
		this.projectRoot = projectRoot;
		criticalLineRecommendations = new ArrayList<>();
	}

	public void analyse() throws IOException {
		File file = new File(projectRoot);
		if (file.exists()) {
			List<String> javaFileRelativePaths = getJavaFilesRelativePaths(file);
			for (int i = 0; i < javaFileRelativePaths.size(); ++i) {
				String javaClass = javaFileRelativePaths.get(i);
				ClassAnalyzer analyzer = new ClassAnalyzer(projectRoot, javaClass);
				analyzer.analyse();
				List<ExpressionInfo> analyzedExpressions = analyzer.getExpressions();
				checkExpressionsForCriticalLines(analyzedExpressions);
				updateProgress((i + 1) / (double) javaFileRelativePaths.size(), 1.0);
			}
		} else {
			throw new FileNotFoundException();
		}
	}

	private List<String> getJavaFilesRelativePaths(File file) throws IOException {
		return Files.walk(file.toPath()).filter(file1 -> file1.toAbsolutePath().toString().endsWith(".java"))
				.map(x -> x.toString().split("src\\\\")[1]).collect(Collectors.toList());
	}

	private void checkExpressionsForCriticalLines(List<ExpressionInfo> analyzedExpressions) {
		for (ExpressionInfo expressionInfo : analyzedExpressions) {
			ExpressionInfoCriticalChecker checkedExpressionInfo = new ExpressionInfoCriticalChecker(expressionInfo);
			PreparedStatementSetterVisitor setterVisitor = new PreparedStatementSetterVisitor(expressionInfo);
			setterVisitor.processCurrentExpression();
			int numberOfSetters = setterVisitor.getNumberOfSetters();
			List<CriticalLine> criticalLines = checkedExpressionInfo.getCriticalLines();
			if (!criticalLines.isEmpty()) {
				criticalLineRecommendations.addAll(createCriticalLineRecommendations(criticalLines, numberOfSetters));
			}
		}
	}

	private List<CriticalLineRecommendation> createCriticalLineRecommendations(List<CriticalLine> criticalLines, int numberOfSetters) {
		List<CriticalLineRecommendation> criticalLineRecommendations = new ArrayList<>();
		if (!criticalLines.isEmpty()) {
			List<CompilationUnit> criticalLinesCompilationUnits = criticalLines.stream().map(CriticalLine::getExpressionInfo)
					.map(ExpressionInfo::getCompilationUnit).distinct().collect(Collectors.toList());
			List<CriticalLine> criticalLineGroup = new ArrayList<>();
			Set<String> recommendations = new HashSet<>();

			for (CompilationUnit compilationUnit : criticalLinesCompilationUnits) {
				for (CriticalLine criticalLine : criticalLines) {
					CompilationUnit lineCompilationUnit = criticalLine.getExpressionInfo().getCompilationUnit();
					if (compilationUnit.equals(lineCompilationUnit)) {
						criticalLineGroup.add(criticalLine);
						recommendations.addAll(ExpressionInfoCriticalChecker.getCriticalLineRecommendations(criticalLine, numberOfSetters));
					}
				}
				String compilationUnitName = CompilationUnitUtils.getCompilationUnitName(compilationUnit);
				String compilationUnitRelativePath = CompilationUnitUtils.getCompiltaionUnitClassPath(compilationUnit);
				String classPath = CompilationUnitUtils.getClassPath(projectRoot, compilationUnitRelativePath);
				criticalLineRecommendations.add(new CriticalLineRecommendation(compilationUnitName, classPath, criticalLines, recommendations));
			}
		}
		return criticalLineRecommendations;
	}

	public List<CriticalLineRecommendation> getCriticalLineRecommendations() {
		return criticalLineRecommendations;
	}

	@Override
	protected Void call() throws Exception {
		analyse();
		return null;
	}

}
