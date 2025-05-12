import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class KlientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String wybranePytanie; // Przechowuje wylosowane pytanie dla danego klienta.

    public KlientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public String getWybranePytanie() {
        return wybranePytanie;
    }

    public void setWybranePytanie(String wybranePytanie) {
        this.wybranePytanie = wybranePytanie;
    }

    @Override
    public void run() {
        try {
            // Inicjalizacja strumieni wejścia/wyjścia
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Wysyłanie powitania do klienta
            out.println("Witaj! Połączono z serwerem. Oczekiwanie na pytanie...");

            // Pętla obsługująca komunikację z klientem
            String pytanie = serwer.wylosujPytanie(this); // Losowanie pytania dla klienta
            out.println(pytanie); // Wysyłanie pytania do klienta

            String odpowiedz = in.readLine(); // Oczekiwanie na odpowiedź od klienta
            System.out.println("Odpowiedź od klienta " + clientSocket.getInetAddress() + ": " + odpowiedz);
            serwer.zapiszOdpowiedz(odpowiedz); // Zapisanie odpowiedzi do pliku

            out.println("Dziękujemy za odpowiedź.  Zamykamy połączenie.");

        } catch (IOException e) {
            System.err.println("Błąd obsługi klienta " + clientSocket.getInetAddress() + ": " + e.getMessage());
        } finally {
            try {
                // Zamknięcie zasobów (strumieni, gniazda)
                if (in != null) in.close();
                if (out != null) out.close();
                clientSocket.close();
                serwer.clients.remove(this); // Usuń klienta z listy
                System.out.println("Połączenie z klientem " + clientSocket.getInetAddress() + " zakończone.");
            } catch (IOException e) {
                System.err.println("Błąd zamykania zasobów klienta: " + e.getMessage());
            }
        }
    }
}