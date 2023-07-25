package gewichtstracker;

public class GewichtEntry {

    private int id;
    private String datum;
    private double gewicht;

    public GewichtEntry(int id, String datum, double gewicht) {
        this.id = id;
        this.datum = datum;
        this.gewicht = gewicht;
    }

    public int getId() {
        return id;
    }

    public String getDatum() {
        return datum;
    }

    public double getGewicht() {
        return gewicht;
    }
}
