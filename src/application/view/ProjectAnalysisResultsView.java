package application.view;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import application.backend.dto.CriticalLineRecommendation;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class ProjectAnalysisResultsView extends Stage {

	private static final String PROCESSING_RESULTS = "Processing results";
	private static final String PROJECT = "Project";
	private static final String DATE = "Date";

	private String projectRoot;
	private Consumer<Void> showMainDialog;
	private List<CriticalLineRecommendation> criticalLineRecommendations;

	public ProjectAnalysisResultsView(String projectRoot, List<CriticalLineRecommendation> recommendations, Consumer<Void> showMainDialog) {
		this.projectRoot = projectRoot;
		this.criticalLineRecommendations = recommendations;
		this.showMainDialog = showMainDialog;
		setView();
	}

	private void setView() {
		String date = new SimpleDateFormat("dd.MM.yyyy.").format(new Date());
		VBox verticalLayout = new VBox();
		verticalLayout.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(5), Insets.EMPTY)));

		VBox verticalInfoLayout = new VBox();
		verticalInfoLayout.setBackground(new Background(new BackgroundFill(Color.web("#424242"), new CornerRadii(5), Insets.EMPTY)));
		Label projectLabel = getLabel(PROJECT + ": " + projectRoot);
		Label timeLabel = getLabel(DATE + ": " + date);
		verticalInfoLayout.getChildren().add(projectLabel);
		verticalInfoLayout.getChildren().add(timeLabel);
		CriticalLinesOverview criticalLinesOverview = new CriticalLinesOverview(criticalLineRecommendations);
		criticalLinesOverview.prefWidthProperty().bind(verticalLayout.widthProperty());
		criticalLinesOverview.prefHeightProperty().bind(verticalLayout.heightProperty());

		verticalLayout.getChildren().add(verticalInfoLayout);
		verticalLayout.getChildren().add(criticalLinesOverview);

		Scene scene = new Scene(verticalLayout, 1500, 800);
		setScene(scene);
		setTitle(PROCESSING_RESULTS);
		show();
		scene.getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, e -> showMainDialog.accept(null));
	}

	private Label getLabel(String text) {
		Label label = new Label(text);
		label.setTextFill(Color.WHITE);
		label.setFont(new Font(20));
		label.setPadding(new Insets(10, 10, 0, 10));
		return label;
	}
}
