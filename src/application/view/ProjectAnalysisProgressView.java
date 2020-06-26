package application.view;

import java.util.List;
import java.util.function.Consumer;

import application.backend.dto.CriticalLineRecommendation;
import application.backend.service.ProjectAnalyzer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ProjectAnalysisProgressView extends Stage {

	private static final String PROCESSING_PROJECT = "Processing project...";

	private String projectRoot;
	private ProjectAnalyzer projectAnalyzer;
	private List<CriticalLineRecommendation> criticalLineRecommendations;
	private ProgressBar progressBar;
	private Consumer<Void> showMainDialog;

	public ProjectAnalysisProgressView(String projectRoot, Consumer<Void> showMainDialog) {
		this.projectRoot = projectRoot;
		this.projectAnalyzer = new ProjectAnalyzer(projectRoot);
		this.projectAnalyzer.setOnSucceeded(e -> analysisFinished());
		this.showMainDialog = showMainDialog;
		setView();
		final Thread thread = new Thread(projectAnalyzer);
		thread.setDaemon(true);
		thread.start();
	}

	private void setView() {
		HBox horiznotallLayout = new HBox();
		horiznotallLayout.setAlignment(Pos.CENTER);
		horiznotallLayout.setFillHeight(true);
		horiznotallLayout.setFillHeight(true);
		horiznotallLayout.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(5), Insets.EMPTY)));

		progressBar = new ProgressBar();
		progressBar.setStyle("-fx-box-border: goldenrod; -fx-accent: #5cb85c;");
		progressBar.prefWidthProperty().bind(horiznotallLayout.widthProperty().subtract(20));
		progressBar.prefHeightProperty().bind(horiznotallLayout.heightProperty().subtract(20));
		progressBar.progressProperty().bind(projectAnalyzer.progressProperty());
		horiznotallLayout.getChildren().add(progressBar);

		Scene scene = new Scene(horiznotallLayout, 1000, 50);
		setScene(scene);
		setTitle(PROCESSING_PROJECT);
		show();
	}

	private void analysisFinished() {
		criticalLineRecommendations = projectAnalyzer.getCriticalLineRecommendations();
		ProjectAnalysisResultsView projectAnalysisResultsView = new ProjectAnalysisResultsView(projectRoot, criticalLineRecommendations, showMainDialog);
		close();
		projectAnalysisResultsView.show();
	}

}
