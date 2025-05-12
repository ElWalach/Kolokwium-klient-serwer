import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class serwer {
    private static final int PORT= 4999;
    private static final int MAX_CLIENTS= 250;
    private static List<String> pytania= new ArrayList<>();
    static final List<KlientHandler> clients = new ArrayList<>(); // Lista aktywnych klientów
    private static ExecutorService executorService;


    public static void main(String[] args) throws IOException{
        // Wczytywanie pytań z pliku
        if (!wczytajPytania()) {
            System.out.println("Błąd wczytywania pytań z pliku.");
            return;
        }

        // Uruchomienie serwera
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serwer uruchomiony na porcie " + PORT);
            executorService = Executors.newFixedThreadPool(MAX_CLIENTS); // Używamy puli wątków

            while (clients.size() < MAX_CLIENTS) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowy klient połączony: " + clientSocket.getInetAddress());

                KlientHandler clientHandler = new KlientHandler(clientSocket);
                clients.add(clientHandler); // Dodaj klienta do listy
                executorService.submit(clientHandler); // Przypisanie obsługi klienta do wątku z puli
            }
            System.out.println("Osiągnięto maksymalną liczbę klientów (" + MAX_CLIENTS + "). Serwer nie przyjmuje nowych połączeń.");
            executorService.shutdown(); // Zamknij pule wątków po zakończeniu przyjmowania klientów.
            try {
                executorService.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS); // Czekaj na zakończenie wszystkich wątków
            } catch (InterruptedException e) {
                System.err.println("Przerwano czekanie na zakończenie wątków: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Błąd serwera: " + e.getMessage());
        } finally {
            // Wykonaj czyszczenie zasobów (np. zamknięcie puli wątków)
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow(); // Natychmiastowe zamknięcie puli
            }
        }
    }


    private static boolean wczytajPytania(){
        try (BufferedReader br = new BufferedReader(new FileReader("BazaPytan.txt"))){
            String linijka;
            while((linijka= br.readLine())!= null){
                pytania.add(linijka);
            }
            if(pytania.isEmpty()){
                System.err.println("Plik z pytaniami jest pusty!!");
                return false;
            }
            return true;
        } catch (IOException e){
            System.err.println("Błąd odczytu BazaPytan.txt"+ e.getMessage());
            return false;
        }
    }

    // Metoda do losowania pytania (prosty przykład - losowanie bez powtórzeń)
    static String wylosujPytanie(KlientHandler clientHandler) {
        if (pytania.isEmpty()) {
            return "Brak dostępnych pytań.";
        }
        // Szukamy, czy klient już dostał jakieś pytanie.
        String wybranePytanie = clientHandler.getWybranePytanie();
        if (wybranePytanie != null) {
            return wybranePytanie; // Zwróć to samo pytanie, które już dostał.
        } else {
            // Losowanie pytania dla klienta.
            int randomIndex = (int) (Math.random() * pytania.size());
            String pytanie = pytania.get(randomIndex);
            clientHandler.setWybranePytanie(pytanie); // Zapamiętaj wybrane pytanie dla klienta
            return pytanie;
        }
    }

    // Metoda do zapisywania odpowiedzi klienta do pliku
    static void zapiszOdpowiedz(String odpowiedz) {
        try (PrintWriter pw = new PrintWriter(new FileWriter("BazaOdpowiedzi.txt", true))) {
            pw.println(odpowiedz);
        } catch (IOException e) {
            System.err.println("Błąd zapisu do pliku BazaOdpowiedzi.txt: " + e.getMessage());
        }
    }

}


