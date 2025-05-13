public class Pytanie {
    private String tresc;
    private String odpA;
    private String odpB;
    private String odpC;
    private String odpD;
    private String prawidlowaOdpowiedz;

    public Pytanie(String tresc, String odpA, String odpB, String odpC, String odpD, String prawidlowaOdpowiedz) {
        this.tresc = tresc;
        this.odpA = odpA;
        this.odpB = odpB;
        this.odpC = odpC;
        this.odpD = odpD;
        this.prawidlowaOdpowiedz = prawidlowaOdpowiedz;
    }

    public String getTresc() {
        return this.tresc + "\nA. " + this.odpA + "\nB. " + this.odpB + "\nC. " + this.odpC + "\nD. " + this.odpD;
    }

    public String getPrawidlowaOdpowiedz() {
        return this.prawidlowaOdpowiedz;
    }
}