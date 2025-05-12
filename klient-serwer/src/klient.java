import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class klient {
    public static void main(String[] args) {
        final String SERVER_ADDRESS = "localhost"; // Adres serwera
        final int PORT = 4999; // Port serwera

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT)) {
            // Inicjalizacja strumieni wejścia/wyjścia
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);

            // Odbiór powitania od serwera
            String powitanie = in.readLine();
            System.out.println(powitanie);

            // Odbiór pytania od serwera
            String pytanie = in.readLine();
            System.out.println("Pytanie od serwera: " + pytanie);

            // Wysyłanie odpowiedzi na pytanie
            System.out.print("Twoja odpowiedź: ");
            String odpowiedz = scanner.nextLine();
            out.println(odpowiedz);

            // Odbiór podziękowania od serwera
            String podziekowanie = in.readLine();
            System.out.println(podziekowanie);

        } catch (IOException e) {
            System.err.println("Błąd klienta: " + e.getMessage());
        }
    }
}
