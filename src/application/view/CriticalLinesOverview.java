package application.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import application.backend.dto.CriticalLine;
import application.backend.dto.CriticalLineRecommendation;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class CriticalLinesOverview extends HBox {

	private List<CriticalLineRecommendation> criticalLineRecommendations;
	private List<CriticalLineRecommendation> selectedClassCriticalLines;
	private String selectedClassPath;
	private String[] selectedClassCode;

	private VBox leftControl;
	private VBox rightControlCode;
	private HBox selectedElement;
	private VBox rightControl;
	private VBox rightControlCodeDescription;
	private VBox rightScrollPane;
	private ScrollPane codeDescriptionScrollPane;
	private ScrollPane codeScrollPane;

	public CriticalLinesOverview(List<CriticalLineRecommendation> criticalLineRecommendations) {
		this.criticalLineRecommendations = criticalLineRecommendations;
		this.selectedClassCriticalLines = new ArrayList<>();
		selectedClassCode = new String[0];
		setView();
	}

	private void setView() {
		SplitPane splitPane = createSplitPane();
		splitPane.prefWidthProperty().bind(this.widthProperty());
		splitPane.prefHeightProperty().bind(this.heightProperty());
		getChildren().add(splitPane);
	}

	private SplitPane createSplitPane() {
		SplitPane splitPane = new SplitPane();
		ScrollPane leftScrollPane = getLeftSideControl();
		rightScrollPane = getRightSideControl();
		splitPane.getItems().addAll(leftScrollPane, rightScrollPane);
		splitPane.setDividerPosition(0, 0.2);
		return splitPane;
	}

	private ScrollPane getLeftSideControl() {
		ScrollPane scrollPane = new ScrollPane();
		leftControl = new VBox();
		leftControl.prefWidthProperty().bind(scrollPane.widthProperty());
		leftControl.prefHeightProperty().bind(scrollPane.heightProperty());
		List<String> classNames = getJavaClassNames();
		classNames.forEach(className -> leftControl.getChildren().add(getLeftSideElement(className)));
		scrollPane.setContent(leftControl);
		return scrollPane;
	}

	private List<String> getJavaClassNames() {
		return criticalLineRecommendations.stream().map(CriticalLineRecommendation::getClassName).distinct().collect(Collectors.toList());
	}

	private VBox getRightSideControl() {
		rightControl = new VBox();
		SplitPane splitPane = new SplitPane();
		splitPane.prefWidthProperty().bind(rightControl.widthProperty().divide(2));
		splitPane.prefHeightProperty().bind(rightControl.heightProperty());

		codeScrollPane = new ScrollPane();
		codeScrollPane.prefWidthProperty().bind(splitPane.widthProperty().divide(2));
		codeScrollPane.prefHeightProperty().bind(splitPane.heightProperty());

		codeDescriptionScrollPane = new ScrollPane();
		codeDescriptionScrollPane.prefWidthProperty().bind(splitPane.widthProperty().divide(2));
		codeDescriptionScrollPane.prefHeightProperty().bind(splitPane.heightProperty());

		rightControlCode = new VBox();
		rightControlCode.prefWidthProperty().bind(codeScrollPane.widthProperty());
		rightControlCode.prefHeightProperty().bind(codeScrollPane.heightProperty());
		codeScrollPane.setContent(rightControlCode);

		rightControlCodeDescription = new VBox();
		rightControlCodeDescription.prefWidthProperty().bind(codeDescriptionScrollPane.widthProperty());
		rightControlCodeDescription.prefHeightProperty().bind(codeDescriptionScrollPane.heightProperty());
		codeDescriptionScrollPane.setContent(rightControlCodeDescription);

		splitPane.getItems().addAll(codeScrollPane, codeDescriptionScrollPane);
		splitPane.setDividerPosition(0, 0.5);

		rightControl.getChildren().add(splitPane);
		populateRightSideCode();
		populateRightSideCodeDescription();
		return rightControl;
	}

	private void populateRightSideCode() {
		rightControlCode.getChildren().clear();
		for (int i = 0; i < selectedClassCode.length; ++i)
			rightControlCode.getChildren().add(getLineLayout(i + 1, selectedClassCode[i]));
	}

	private void populateRightSideCodeDescription() {
		rightControlCodeDescription.getChildren().clear();
		for (int i = 0; i < selectedClassCriticalLines.size(); ++i)
			rightControlCodeDescription.getChildren().add(new Label(selectedClassCriticalLines.get(i).toString()));
	}

	private HBox getLineLayout(int lineNumber, String line) {
		HBox lineLayout = new HBox();
		Label lineLabel = new Label(lineNumber + " " + line);
		lineLayout.getChildren().add(lineLabel);
		addLineLayoutStyles(lineNumber, lineLayout);
		return lineLayout;
	}

	private void addLineLayoutStyles(int lineNumber, HBox lineLayout) {
		boolean isCriticalLine = false;
		int i = 0;
		int j = 0;
		lineLayout.prefWidthProperty().bind(rightControlCode.widthProperty());

		for (i = 0; i < selectedClassCriticalLines.size(); ++i) {
			List<CriticalLine> criticalLines = selectedClassCriticalLines.get(i).getCriticalLines();
			for (j = 0; j < criticalLines.size(); ++j)
				if (criticalLines.get(j).getExpressionInfo().getDeclarationLineNumber() == lineNumber) {
					isCriticalLine = true;
					break;
				}

			if (isCriticalLine) {
				break;
			}
		}
		final int recommendationIndex = i;
		if (isCriticalLine) {
			lineLayout.setBackground(new Background(new BackgroundFill(Color.web("#ff726f"), CornerRadii.EMPTY, Insets.EMPTY)));
			Tooltip tooltip = new Tooltip(selectedClassCriticalLines.get(recommendationIndex).toString());
			Tooltip.install(lineLayout, tooltip);
		} else {
			lineLayout.setBackground(new Background(new BackgroundFill(Color.web("#ccf7cc"), CornerRadii.EMPTY, Insets.EMPTY)));
		}
	}

	private HBox getLeftSideElement(String className) {
		HBox element = new HBox();
		Label classNameLabel = new Label(className);
		classNameLabel.setFont(new Font(14));
		element.getChildren().add(classNameLabel);
		element.setOnMouseClicked(e -> selectClass(element, className));
		return element;
	}

	private void selectClass(HBox element, String className) {
		selectedClassCriticalLines = criticalLineRecommendations.stream().filter(criticalLine -> criticalLine.getClassName().equals(className))
				.collect(Collectors.toList());
		selectedClassPath = selectedClassCriticalLines.get(0).getClassPath();
		readClassCode();
		changeSelectedClassColor(element);
		populateRightSideCode();
		populateRightSideCodeDescription();
		codeScrollPane.setVvalue(0);
		codeDescriptionScrollPane.setVvalue(0);
	}

	private void readClassCode() {
		File file = new File(selectedClassPath);
		try {
			selectedClassCode = Files.readAllLines(file.toPath()).stream().toArray(String[]::new);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void changeSelectedClassColor(HBox element) {
		if (selectedElement != null) {
			selectedElement.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
			Label selectedElementLabel = (Label) selectedElement.getChildren().get(0);
			selectedElementLabel.setTextFill(Color.BLACK);
		}

		element.setBackground(new Background(new BackgroundFill(Color.web("#424242"), CornerRadii.EMPTY, Insets.EMPTY)));
		Label selectedElementLabel = (Label) element.getChildren().get(0);
		selectedElementLabel.setTextFill(Color.WHITE);
		selectedElement = element;
	}

}
