import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KlientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Serwer serwer;
    private String studentId;
    private int aktualnePytanieIndex = 0;
    private int poprawneOdpowiedzi = 0;

    public KlientHandler(Socket clientSocket, Serwer serwer) {
        this.clientSocket = clientSocket;
        this.serwer = serwer;
    }

    public void run() {
        try {
            this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);

            // Identyfikacja studenta
            this.out.println("Podaj swoje ID studenta:");
            this.studentId = this.in.readLine();
            System.out.println("Student połączony z ID: " + this.studentId);
            this.serwer.zarejestrujStudenta(this.studentId);

            // Pobranie pytań
            List<Pytanie> pytania = this.serwer.getListaPytan();
            if (pytania.isEmpty()) {
                this.out.println("Brak dostępnych pytań.");
                return;
            }

            // Przeprowadzanie testu
            for (; this.aktualnePytanieIndex < pytania.size(); ++this.aktualnePytanieIndex) {
                Pytanie pytanie = pytania.get(this.aktualnePytanieIndex);
                String trescPytania = pytanie.getTresc();
                int czasNaOdpowiedz = this.serwer.getCzasNaOdpowiedz();

                System.out.println("Serwer wysyła do " + this.studentId + ": Pytanie='" +
                        trescPytania + "', Czas='" + czasNaOdpowiedz + "'");

                // Wysłanie pytania
                this.out.println(trescPytania);
                this.out.println(czasNaOdpowiedz);

                // Odbiór odpowiedzi
                String odpowiedz = null;
                try {
                    // dodajemy 2 sekundy na buforowanie i przesyłanie
                    this.clientSocket.setSoTimeout((int)TimeUnit.SECONDS.toMillis(czasNaOdpowiedz + 2));
                    System.out.println("Oczekuję na odpowiedź od klienta " + this.studentId);
                    odpowiedz = this.in.readLine();
                    System.out.println("Odebrałem odpowiedź od klienta " + this.studentId + ": " + odpowiedz);
                    this.clientSocket.setSoTimeout(0);
                } catch (IOException e) {
                    System.out.println("Upłynął czas na odpowiedź studenta " + this.studentId +
                            " na pytanie: " + pytanie.getTresc());
                    odpowiedz = "BRAK_ODPOWIEDZI";
                }

                // Zapis odpowiedzi i sprawdzenie poprawności
                this.serwer.zapiszOdpowiedz(
                        this.studentId,
                        pytanie.getTresc(),
                        odpowiedz,
                        pytanie.getPrawidlowaOdpowiedz()
                );

                if (odpowiedz != null && odpowiedz.trim().equalsIgnoreCase(pytanie.getPrawidlowaOdpowiedz())) {
                    ++this.poprawneOdpowiedzi;
                }
            }

            // przesłanie wyników
            this.out.println("KONIEC_TESTU");
            this.out.println("Twój wynik: " + this.poprawneOdpowiedzi + "/" + pytania.size());
            this.serwer.zapiszWynik(this.studentId, this.poprawneOdpowiedzi, pytania.size());

        } catch (IOException e) {
            System.err.println("Błąd obsługi klienta " + this.clientSocket.getInetAddress() +
                    " (ID: " + this.studentId + "): " + e.getMessage());
        } finally {
            // Zamknięcie
            try {
                if (this.in != null) {
                    this.in.close();
                }
                if (this.out != null) {
                    this.out.close();
                }
                this.clientSocket.close();
                this.serwer.usunKlienta(this);

                System.out.println("Połączenie z klientem " + this.clientSocket.getInetAddress() +
                        " (ID: " + this.studentId + ") zakończone.");
            } catch (IOException e) {
                System.err.println("Błąd zamykania zasobów klienta (ID: " + this.studentId + "): " + e.getMessage());
            }
        }
    }

    public String getStudentId() {
        return this.studentId;
    }
}