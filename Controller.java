package gewichtstracker;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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
        // Map die Spalten der TableView auf die Eigenschaften von GewichtEntry
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        datumColumn.setCellValueFactory(new PropertyValueFactory<>("datum"));
        gewichtColumn.setCellValueFactory(new PropertyValueFactory<>("gewicht"));

        // Lade vorhandene Einträge in die TableView
        entryTableView.getItems().addAll(gewichtDAO.getAllEntries());

        // Füge einen Listener hinzu, um die Textfelder mit den Eintragsdaten zu füllen
        entryTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        idTextField.setText(String.valueOf(newValue.getId()));
                        datumTextField.setText(newValue.getDatum());
                        gewichtTextField.setText(String.valueOf(newValue.getGewicht()));
                    }
                });
        // Initialisiere die LineChart-Daten
        weightSeries = new XYChart.Series<>();
        weightSeries.setName("Gewichtsverlauf");
        lineChart.getData().add(weightSeries);
        
        NumberAxis yAxis = (NumberAxis) lineChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(95);
        yAxis.setUpperBound(125);

        // Aktualisiere die LineChart-Daten beim Start
        updateLineChart();

        // Datenpunkte im Linechart durchlaufen und Tooltips hinzufügen
        for (XYChart.Data<Number, Number> dataPoint : weightSeries.getData()) {
            GewichtEntry entry = gewichtDAO.getEntryById(dataPoint.getXValue().intValue());
            String id = String.valueOf(entry.getId());
            String datum = entry.getDatum();
            String gewicht = String.valueOf(entry.getGewicht());

            Tooltip tooltip = new Tooltip("ID: " + id + "\nDatum: " + datum + "\nGewicht: " + gewicht);
            tooltip.setStyle("-fx-font-size: 12px;"); // Optionale Formatierung

            // Tooltip anzeigen, wenn Maus über den Datenpunkt fährt
            Tooltip.install(dataPoint.getNode(), tooltip);
        }


    }
    
    
    private void updateLineChart() {
        // Lösche alle Daten aus der LineChart-Serie
        weightSeries.getData().clear();

        // Füge die Einträge aus der Datenbank zur LineChart-Serie hinzu
        List<GewichtEntry> entries = gewichtDAO.getAllEntries();
        for (GewichtEntry entry : entries) {
            weightSeries.getData().add(new XYChart.Data<>(entry.getId(), entry.getGewicht()));
        }
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