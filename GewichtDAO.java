package gewichtstracker;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GewichtDAO {

    private static final String DATABASE_URL = "jdbc:sqlite:gewicht.db";

    public void createTableIfNotExists() {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS gewicht (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "datum TEXT NOT NULL," +
                    "gewicht REAL NOT NULL)";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insert(GewichtEntry entry) {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO gewicht (datum, gewicht) VALUES (?, ?)")) {
            pstmt.setString(1, entry.getDatum());
            pstmt.setDouble(2, entry.getGewicht());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<GewichtEntry> getAllEntries() {
        List<GewichtEntry> entries = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM gewicht ORDER BY datum DESC")) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String datum = rs.getString("datum");
                double gewicht = rs.getDouble("gewicht");
                entries.add(new GewichtEntry(id, datum, gewicht));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }

    public void update(GewichtEntry entry) {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE gewicht SET datum = ?, gewicht = ? WHERE id = ?")) {
            pstmt.setString(1, entry.getDatum());
            pstmt.setDouble(2, entry.getGewicht());
            pstmt.setInt(3, entry.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "DELETE FROM gewicht WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public GewichtEntry getEntryById(int id) {
        GewichtEntry entry = null;
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM gewicht WHERE id = ?")) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String datum = rs.getString("datum");
                    double gewicht = rs.getDouble("gewicht");
                    entry = new GewichtEntry(id, datum, gewicht);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entry;
    }

    public void insertAll(List<GewichtEntry> entries) {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO gewicht (datum, gewicht) VALUES (?, ?)")) {
            for (GewichtEntry entry : entries) {
                pstmt.setString(1, entry.getDatum());
                pstmt.setDouble(2, entry.getGewicht());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
