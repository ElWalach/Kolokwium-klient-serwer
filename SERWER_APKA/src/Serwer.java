import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Serwer {
    private int port;
    private String ipAddress;
    private int maxClients;
    private int czasNaOdpowiedz;
    private List<Pytanie> listaPytan = new ArrayList<>();
    private List<KlientHandler> klienci = new ArrayList<>();
    private List<String> zajeteId = new ArrayList<>();
    private ExecutorService executorService;

    public Serwer() {
        this.loadConfig();
        this.wczytajPytania();
        this.executorService = Executors.newFixedThreadPool(this.maxClients);
    }

    private void loadConfig() {
        Properties properties = new Properties();

        try (FileInputStream configFile = new FileInputStream("C:/SERWER_APKA/config.txt")) {
            properties.load(configFile);
            this.port = Integer.parseInt(properties.getProperty("PORT", "4999"));
            this.ipAddress = properties.getProperty("SERVER_ADDRESS", "localhost");
            this.maxClients = Integer.parseInt(properties.getProperty("MAX_CLIENTS", "250"));
            this.czasNaOdpowiedz = Integer.parseInt(properties.getProperty("CZAS_NA_ODPOWIEDZ", "30"));
            System.out.println("Konfiguracja serwera: IP=" + this.ipAddress + ", Port=" + this.port +
                    ", Max Clients=" + this.maxClients + ", Czas na odpowiedź=" + this.czasNaOdpowiedz);
        } catch (IOException e) {
            System.err.println("Błąd odczytu pliku konfiguracyjnego: " + e.getMessage());
            this.port = 4999;
            this.ipAddress = "localhost";
            this.maxClients = 250;
            this.czasNaOdpowiedz = 30;
            System.err.println("Używam domyślnych ustawień serwera: IP=" + this.ipAddress + ", Port=" + this.port +
                    ", Max Clients=" + this.maxClients + ", Czas na odpowiedź=" + this.czasNaOdpowiedz);
        }
    }

    private void wczytajPytania() {
        try (BufferedReader reader = new BufferedReader(new FileReader("C:/SERWER_APKA/BazaPytan.txt"))) {
            String linia;
            while ((linia = reader.readLine()) != null) {
                String[] czesci = linia.split(";");
                if (czesci.length == 6) {
                    String tresc = czesci[0];
                    String odpA = czesci[1];
                    String odpB = czesci[2];
                    String odpC = czesci[3];
                    String odpD = czesci[4];
                    String poprawnaOdp = czesci[5].trim().toUpperCase();

                    if (poprawnaOdp.matches("[A-D]")) {
                        this.listaPytan.add(new Pytanie(tresc, odpA, odpB, odpC, odpD, poprawnaOdp));
                    } else {
                        System.err.println("Niepoprawny format poprawnej odpowiedzi w linii: " + linia);
                    }
                } else {
                    System.err.println("Niepoprawny format linii w pliku z pytaniami: " + linia);
                }
            }

            if (this.listaPytan.isEmpty()) {
                System.err.println("Plik z pytaniami jest pusty!");
            }

            System.out.println("Wczytano " + this.listaPytan.size() + " pytań.");
        } catch (IOException e) {
            System.err.println("Błąd odczytu pliku bazaPytan.txt: " + e.getMessage());
        }
    }

    public List<Pytanie> getListaPytan() {
        return this.listaPytan;
    }

    public int getCzasNaOdpowiedz() {
        return this.czasNaOdpowiedz;
    }

    public void zarejestrujStudenta(String studentId) {
        synchronized (this.zajeteId) {
            if (this.zajeteId.contains(studentId)) {
                System.err.println("Próba ponownej rejestracji studenta o ID: " + studentId);
            } else {
                this.zajeteId.add(studentId);
                System.out.println("Zarejestrowano studenta o ID: " + studentId);
            }
        }
    }

    public void usunKlienta(KlientHandler klient) {
        synchronized (this.klienci) {
            this.klienci.remove(klient);
        }

        synchronized (this.zajeteId) {
            if (klient.getStudentId() != null) {
                this.zajeteId.remove(klient.getStudentId());
                System.out.println("Wyrejestrowano studenta o ID: " + klient.getStudentId());
            }
        }
    }

    public void zapiszOdpowiedz(String studentId, String pytanie, String odpowiedz, String poprawnaOdpowiedz) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("C:/SERWER_APKA/BazaOdpowiedzi.txt", true))) {
            writer.println(studentId + ";" + pytanie + ";" + odpowiedz + ";" + poprawnaOdpowiedz);
        } catch (IOException e) {
            System.err.println("Błąd zapisu do pliku bazaOdpowiedzi.txt: " + e.getMessage());
        }
    }

    public void zapiszWynik(String studentId, int poprawneOdpowiedzi, int wszystkiePytania) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("C:/SERWER_APKA/wyniki.txt", true))) {
            writer.println(studentId + ";" + poprawneOdpowiedzi + ";" + wszystkiePytania);
        } catch (IOException e) {
            System.err.println("Błąd zapisu do pliku wyniki.txt: " + e.getMessage());
        }
    }

    public void uruchom() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            System.out.println("Serwer uruchomiony na adresie " + this.ipAddress + " i porcie " + this.port);

            while (this.klienci.size() < this.maxClients) {
                Socket clientSocket = serverSocket.accept();        //accept
                System.out.println("Nowe połączenie od: " + clientSocket.getInetAddress());

                KlientHandler klientHandler = new KlientHandler(clientSocket, this);
                synchronized (this.klienci) {
                    this.klienci.add(klientHandler);
                }
                this.executorService.submit(klientHandler);
            }

            System.out.println("Osiągnięto maksymalną liczbę klientów (" + this.maxClients +
                    "). Serwer nie przyjmuje nowych połączeń.");
            this.executorService.shutdown();

            try {
                this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                System.err.println("Przerwano oczekiwanie na zakończenie wątków: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Błąd serwera: " + e.getMessage());
        } finally {
            if (this.executorService != null && !this.executorService.isShutdown()) {
                this.executorService.shutdownNow();
            }
        }
    }

    public static void main(String[] args) {
        Serwer serwer = new Serwer();
        serwer.uruchom();
    }
}