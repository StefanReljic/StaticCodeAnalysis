package application;

import java.io.File;
import java.io.IOException;

import application.view.ProjectAnalysisProgressView;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class Main extends Application {

	private static final String APPLICATION_TITLE = "Static code analyzer";
	private static final String SELECT_PROJECT = "Select project";
	private static final String WELCOME_MESSGE = "Welcome!";

	private ProjectAnalysisProgressView projectAnalyzerView;
	private Stage stage;
	// C:\Users\reljics\eclipse-workspace\RMAN-copy

	public static void main(String[] args) throws IOException {
		launch(args);
	}

	@Override
	public void start(Stage stage) {
		this.stage = stage;
		stage.setTitle(APPLICATION_TITLE);

		Button selectProjectButton = new Button();
		selectProjectButton.setText(SELECT_PROJECT);
		selectProjectButton.setOnAction(e -> selectProject(stage, e));
		selectProjectButton.setTextFill(Color.web("#ffffff"));
		selectProjectButton.setBackground(new Background(new BackgroundFill(Color.web("#424242"), new CornerRadii(5), Insets.EMPTY)));

		Label welcomeLabel = new Label(WELCOME_MESSGE);
		welcomeLabel.setTextFill(Color.web("#61DBFB"));
		welcomeLabel.setFont(new Font(25));
		welcomeLabel.setPadding(new Insets(10, 10, 10, 10));

		Label titleLabel = new Label(APPLICATION_TITLE);
		titleLabel.setTextFill(Color.web("#61DBFB"));
		titleLabel.setFont(new Font(25));
		titleLabel.setPadding(new Insets(0, 10, 15, 10));

		VBox verticalLayout = new VBox();
		verticalLayout.setAlignment(Pos.CENTER);
		verticalLayout.getChildren().add(welcomeLabel);
		verticalLayout.getChildren().add(titleLabel);

		verticalLayout.getChildren().add(selectProjectButton);
		verticalLayout.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(5), Insets.EMPTY)));
		stage.setScene(new Scene(verticalLayout, 500, 200));
		stage.show();
	}

	private void selectProject(Stage stage, ActionEvent event) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(SELECT_PROJECT);
		File file = directoryChooser.showDialog(stage);
		if (file != null) {
			boolean hasSrcDirectory = false;
			for (File f : file.listFiles())
				if (f.isDirectory() && f.getName().equals("src")) {
					hasSrcDirectory = true;
					break;
				}
			if (hasSrcDirectory) {
				projectAnalyzerView = new ProjectAnalysisProgressView(file.getAbsolutePath() + File.separator + "src", e -> showDialog());
				stage.close();
				projectAnalyzerView.show();
			}
		}
	}

	private void showDialog() {
		stage.show();
	}

}
