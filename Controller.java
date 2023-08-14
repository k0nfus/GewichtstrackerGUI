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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    private TextField txtGroesse;

    @FXML
    private TextField txtGewicht;

    @FXML
    private Button btnBerechnen;

    @FXML
    private Label lblBMI;

    @FXML
    private Label lblBeschreibung;

    private double zielgewicht;

    @FXML
    private TextField zielgewichtTextField;

    public Controller() {
        gewichtDAO = new GewichtDAO();
        gewichtDAO.createTableIfNotExists();
    }

    private void initializeTooltips() {
        for (XYChart.Data<Number, Number> dataPoint : weightSeries.getData()) {
            GewichtEntry entry = gewichtDAO.getEntryById(dataPoint.getXValue().intValue());
            String id = String.valueOf(entry.getId());
            String datum = entry.getDatum();
            String gewicht = String.valueOf(entry.getGewicht());

            Tooltip tooltip = new Tooltip("ID: " + id + "\nDatum: " + datum + "\nGewicht: " + gewicht);
            tooltip.setStyle("-fx-font-size: 12px;");
            Tooltip.install(dataPoint.getNode(), tooltip);
        }
    }

    private int getHighestID() {
        List<GewichtEntry> entries = gewichtDAO.getAllEntries();
        int highestID = 0;
        for (GewichtEntry entry : entries) {
            if (entry.getId() > highestID) {
                highestID = entry.getId();
            }
        }
        return highestID;
    }

    private void updateLineChart() {
        weightSeries.getData().clear();
        List<GewichtEntry> entries = gewichtDAO.getAllEntries();
        for (GewichtEntry entry : entries) {
            weightSeries.getData().add(new XYChart.Data<>(entry.getId(), entry.getGewicht()));
        }
        updateTargetWeightLine();
    }

    private void updateTargetWeightLine() {
        lineChart.getData().removeIf(series -> series.getName().equals("Zielgewicht"));
        XYChart.Series<Number, Number> targetSeries = new XYChart.Series<>();
        targetSeries.setName("Zielgewicht");
        targetSeries.getData().add(new XYChart.Data<>(0, zielgewicht));
        targetSeries.getData().add(new XYChart.Data<>(getHighestID(), zielgewicht));
        Node targetSeriesNode = targetSeries.getNode();
        if (targetSeriesNode != null) {
            targetSeriesNode.lookup(".chart-series-line").setStyle("-fx-stroke: green;");
        }
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
        return 100.0;
    }

    private void saveHeightToFile(double height) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("user_height.txt"))) {
            writer.write(String.valueOf(height));
            System.out.println("Größe erfolgreich gespeichert.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double loadHeightFromFile() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("user_height.txt"));
            if (!lines.isEmpty()) {
                return Double.parseDouble(lines.get(0));
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return 1.91;
    }

    private double getWeightOfLastEntry() {
        int highestID = getHighestID();
        if (highestID > 0) {
            GewichtEntry lastEntry = gewichtDAO.getEntryById(highestID);
            return lastEntry.getGewicht();
        }
        return 0.0;
    }

    @FXML
    private void handleBerechnen() {
        try {
            double groesse = Double.parseDouble(txtGroesse.getText());
            double gewicht = Double.parseDouble(txtGewicht.getText());
            double bmi = gewicht / (groesse * groesse);
            saveHeightToFile(groesse);
            double roundedBMI = Math.round(bmi * 100.0) / 100.0;
            lblBMI.setText("BMI: " + roundedBMI);
            String einordnung;
            if (bmi < 18.5) {
                einordnung = "Untergewicht";
            } else if (bmi < 24.9) {
                einordnung = "Normalgewicht";
            } else if (bmi < 29.9) {
                einordnung = "Übergewicht";
            } else {
                einordnung = "Adipositas";
            }
            lblBeschreibung.setText(einordnung);
        } catch (NumberFormatException e) {
            lblBMI.setText("BMI: N/A");
            lblBeschreibung.setText("Bitte gültige Werte eingeben.");
        }
    }

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
    private void saveSettings() {
        String zielgewichtStr = zielgewichtTextField.getText();
        try {
            double zielgewicht = Double.parseDouble(zielgewichtStr);
            this.zielgewicht = zielgewicht;
            saveTargetWeightToFile(zielgewicht);
            updateTargetWeightLine();
        } catch (NumberFormatException e) {
            System.out.println("Ungültiges Zielgewicht: " + zielgewichtStr);
        }
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

    @FXML
    private void handleAdd() {
        String datum = datumTextField.getText();
        double gewicht = Double.parseDouble(gewichtTextField.getText());
        GewichtEntry newEntry = new GewichtEntry(0, datum, gewicht);
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

    @FXML
    private void initialize() {
        txtGroesse.setText("1.91");
        double loadedHeight = loadHeightFromFile();
        txtGroesse.setText(String.valueOf(loadedHeight));
        double lastEntryWeight = getWeightOfLastEntry();
        txtGewicht.setText(String.valueOf(lastEntryWeight));
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        datumColumn.setCellValueFactory(new PropertyValueFactory<>("datum"));
        gewichtColumn.setCellValueFactory(new PropertyValueFactory<>("gewicht"));
        entryTableView.getItems().addAll(gewichtDAO.getAllEntries());
        weightSeries = new XYChart.Series<>();
        weightSeries.setName("Gewichtsverlauf");
        lineChart.getData().add(weightSeries);
        NumberAxis yAxis = (NumberAxis) lineChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(95);
        yAxis.setUpperBound(125);
        updateLineChart();
        initializeTooltips();
        XYChart.Series<Number, Number> targetSeries = new XYChart.Series<>();
        targetSeries.setName("Zielgewicht");
        targetSeries.getData().add(new XYChart.Data<>(0, 100));
        targetSeries.getData().add(new XYChart.Data<>(getHighestID(), 100));
        Node targetSeriesNode = targetSeries.getNode();
        if (targetSeriesNode != null) {
            targetSeriesNode.lookup(".chart-series-line").setStyle("-fx-stroke: green;");
        }
        lineChart.getData().add(targetSeries);
        double loadedTargetWeight = loadTargetWeightFromFile();
        zielgewichtTextField.setText(String.valueOf(loadedTargetWeight));
        this.zielgewicht = loadedTargetWeight;
        updateTargetWeightLine();
        entryTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                idTextField.setText(String.valueOf(newValue.getId()));
                datumTextField.setText(newValue.getDatum());
                gewichtTextField.setText(String.valueOf(newValue.getGewicht()));
            }
        });
    }
}
