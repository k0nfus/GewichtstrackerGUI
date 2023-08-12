package org.systemkritisch.GewichtstrackerGUI;

import javafx.scene.control.Tooltip;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

// Import & Export
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javafx.scene.control.TabPane;

public class Controller {

	@FXML
	private TableView<GewichtEntry> entryTableView;

	@FXML
	private TableColumn<GewichtEntry, Integer> idColumn;

	@FXML
	private TableColumn<GewichtEntry, String> datumColumn;

	@FXML
	private TableColumn<GewichtEntry, Double> gewichtColumn;

	@FXML
	private LineChart<Number, Number> lineChart;

	private XYChart.Series<Number, Number> weightSeries;

	@FXML
	private TextField idTextField;

	@FXML
	private TextField datumTextField;

	@FXML
	private TextField gewichtTextField;

	private GewichtDAO gewichtDAO;

	@FXML
	private TabPane tabPane;

	@FXML
	private void handleExport() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Gewichtsdaten exportieren");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-Dateien", "*.csv"));
		File file = fileChooser.showSaveDialog(new Stage());
		if (file != null) {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				for (GewichtEntry entry : entryTableView.getItems()) {
					String line = entry.getId() + "," + entry.getDatum() + "," + entry.getGewicht();
					writer.write(line);
					writer.newLine();
				}
				System.out.println("Daten wurden erfolgreich in " + file.getName() + " exportiert.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private TextField zielgewichtTextField; // Für die Benutzereingabe des Zielgewichts im Einstellungen-Tab
	private double zielgewicht;

	@FXML
	private void saveSettings() {
		String zielgewichtStr = zielgewichtTextField.getText();
		try {
			double zielgewicht = Double.parseDouble(zielgewichtStr);
			this.zielgewicht = zielgewicht;

			// Speichern Sie das Zielgewicht in der Datei
			saveTargetWeightToFile(zielgewicht);

			// Aktualisieren Sie die Zielgewichtslinie im Graphen
			updateTargetWeightLine();
		} catch (NumberFormatException e) {
			System.out.println("Ungültiges Zielgewicht: " + zielgewichtStr);
		}
	}

	private void updateTargetWeightLine() {
		// Entfernen Sie die vorhandene Zielgewichtslinie, wenn eine vorhanden ist
		lineChart.getData().removeIf(series -> series.getName().equals("Zielgewicht"));

		// Fügen Sie die Zielgewichtslinie basierend auf dem Benutzerzielgewicht hinzu
		XYChart.Series<Number, Number> targetSeries = new XYChart.Series<>();
		targetSeries.setName("Zielgewicht");
		targetSeries.getData().add(new XYChart.Data<>(0, zielgewicht));
		targetSeries.getData().add(new XYChart.Data<>(getHighestID(), zielgewicht));

		// Zeigen Sie die Linie in grün an
		Node targetSeriesNode = targetSeries.getNode();
		if (targetSeriesNode != null) {
			targetSeriesNode.lookup(".chart-series-line").setStyle("-fx-stroke: green;");
		}

		// Fügen Sie die Datenreihe für das Zielgewicht im Graphen hinzu
		lineChart.getData().add(targetSeries);
	}

	private void saveTargetWeightToFile(double targetWeight) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("target_weight.txt"))) {
			writer.write(String.valueOf(targetWeight));
			System.out.println("Zielgewicht erfolgreich gespeichert.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double loadTargetWeightFromFile() {
		try {
			List<String> lines = Files.readAllLines(Paths.get("target_weight.txt"));
			if (!lines.isEmpty()) {
				return Double.parseDouble(lines.get(0));
			}
		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
		}
		return 100.0; // Standardwert, falls keine Datei gefunden oder das Laden fehlgeschlagen ist
	}

	@FXML
	private void handleImport() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Gewichtsdaten importieren");
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-Dateien", "*.csv"));
		File file = fileChooser.showOpenDialog(new Stage());
		if (file != null) {
			List<GewichtEntry> importedEntries = new ArrayList<>();
			try {
				List<String> lines = Files.readAllLines(Paths.get(file.getAbsolutePath()));
				for (String line : lines) {
					String[] parts = line.split(",");
					if (parts.length == 3) {
						int id = Integer.parseInt(parts[0]);
						String datum = parts[1];
						double gewicht = Double.parseDouble(parts[2]);
						GewichtEntry entry = new GewichtEntry(id, datum, gewicht);
						importedEntries.add(entry);
					}
				}
				gewichtDAO.insertAll(importedEntries);
				System.out.println("Daten wurden erfolgreich aus " + file.getName() + " importiert.");
				entryTableView.getItems().addAll(importedEntries);
				updateLineChart();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Controller() {
		gewichtDAO = new GewichtDAO();
		gewichtDAO.createTableIfNotExists();
	}

	@FXML
	private void initialize() {
		// Map the columns of the TableView to the properties of GewichtEntry
		idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
		datumColumn.setCellValueFactory(new PropertyValueFactory<>("datum"));
		gewichtColumn.setCellValueFactory(new PropertyValueFactory<>("gewicht"));

		// Load existing entries into the TableView
		entryTableView.getItems().addAll(gewichtDAO.getAllEntries());

		// Initialize the LineChart and weightSeries
		weightSeries = new XYChart.Series<>();
		weightSeries.setName("Gewichtsverlauf");
		lineChart.getData().add(weightSeries);

		// Set the bounds for the yAxis of the LineChart
		NumberAxis yAxis = (NumberAxis) lineChart.getYAxis();
		yAxis.setAutoRanging(false);
		yAxis.setLowerBound(95);
		yAxis.setUpperBound(125);

		// Update the LineChart data
		updateLineChart();
		
	    // Initialize the Tooltips for data points in the LineChart
	    initializeTooltips();

		// Add a horizontal line (visual goal at y = 100) to the LineChart
		int highestid = getHighestID();
		XYChart.Series<Number, Number> targetSeries = new XYChart.Series<>();
		targetSeries.setName("Zielgewicht");
		targetSeries.getData().add(new XYChart.Data<>(0, 100));
		targetSeries.getData().add(new XYChart.Data<>(highestid, 100));

		// Show the line in green
		Node targetSeriesNode = targetSeries.getNode();
		if (targetSeriesNode != null) {
			targetSeriesNode.lookup(".chart-series-line").setStyle("-fx-stroke: green;");
		}

		// Add the data series for the visual goal to the LineChart
		lineChart.getData().add(targetSeries);

		// Laden Sie das Zielgewicht aus der Datei und verwenden Sie es, um die
		// Zielgewichtslinie zu aktualisieren
		double loadedTargetWeight = loadTargetWeightFromFile();
		zielgewichtTextField.setText(String.valueOf(loadedTargetWeight));
		this.zielgewicht = loadedTargetWeight;
		updateTargetWeightLine();

		// Set up the selection model for the TableView
		entryTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				idTextField.setText(String.valueOf(newValue.getId()));
				datumTextField.setText(newValue.getDatum());
				gewichtTextField.setText(String.valueOf(newValue.getGewicht()));
			}
		});
	}
	
	private void initializeTooltips() {
	    // Iterate through data points in the LineChart and add Tooltips
	    for (XYChart.Data<Number, Number> dataPoint : weightSeries.getData()) {
	        GewichtEntry entry = gewichtDAO.getEntryById(dataPoint.getXValue().intValue());
	        String id = String.valueOf(entry.getId());
	        String datum = entry.getDatum();
	        String gewicht = String.valueOf(entry.getGewicht());

	        Tooltip tooltip = new Tooltip("ID: " + id + "\nDatum: " + datum + "\nGewicht: " + gewicht);
	        tooltip.setStyle("-fx-font-size: 12px;"); // Optional formatting

	        // Tooltip displayed when the mouse hovers over the data point
	        Tooltip.install(dataPoint.getNode(), tooltip);
	    }
	}


	private int getHighestID() {
		// Get all entries from the DAO
		List<GewichtEntry> entries = gewichtDAO.getAllEntries();

		// Find the highest ID
		int highestID = 0;
		for (GewichtEntry entry : entries) {
			if (entry.getId() > highestID) {
				highestID = entry.getId();
			}
		}
		return highestID;
	}

	private void updateLineChart() {
		// Lösche alle Daten aus der LineChart-Serie
		weightSeries.getData().clear();

		// Füge die Einträge aus der Datenbank zur LineChart-Serie hinzu
		List<GewichtEntry> entries = gewichtDAO.getAllEntries();
		for (GewichtEntry entry : entries) {
			weightSeries.getData().add(new XYChart.Data<>(entry.getId(), entry.getGewicht()));
		}
		updateTargetWeightLine();
	}

	@FXML
	private void handleAdd() {
		String datum = datumTextField.getText();
		double gewicht = Double.parseDouble(gewichtTextField.getText());

		GewichtEntry newEntry = new GewichtEntry(0, datum, gewicht); // id will be auto-generated by the database
		gewichtDAO.insert(newEntry);

		entryTableView.getItems().clear();
		entryTableView.getItems().addAll(gewichtDAO.getAllEntries());
		updateLineChart();

		clearTextFields();
	}

	@FXML
	private void handleUpdate() {
		int id = Integer.parseInt(idTextField.getText());
		String datum = datumTextField.getText();
		double gewicht = Double.parseDouble(gewichtTextField.getText());

		GewichtEntry updatedEntry = new GewichtEntry(id, datum, gewicht);
		gewichtDAO.update(updatedEntry);

		entryTableView.getItems().clear();
		entryTableView.getItems().addAll(gewichtDAO.getAllEntries());
		updateLineChart();
		clearTextFields();
	}

	@FXML
	private void handleDelete() {
		int id = Integer.parseInt(idTextField.getText());

		gewichtDAO.delete(id);

		entryTableView.getItems().clear();
		entryTableView.getItems().addAll(gewichtDAO.getAllEntries());
		updateLineChart();
		clearTextFields();
	}

	private void clearTextFields() {
		idTextField.clear();
		datumTextField.clear();
		gewichtTextField.clear();
	}
}
