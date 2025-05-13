import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class Klient {
    public static void main(String[] args) {
        String serverAddress = null;
        int port = 0;


        try (BufferedReader configFile = new BufferedReader(new FileReader("C:/KLIENT_APKA/config.txt"))) {
            String line;
            while ((line = configFile.readLine()) != null) {
                if (line.startsWith("SERVER_ADDRESS=")) {
                    serverAddress = line.substring("SERVER_ADDRESS=".length()).trim();
                } else if (line.startsWith("PORT=")) {
                    port = Integer.parseInt(line.substring("PORT=".length()).trim());
                } else {
                    System.err.println("Nieznana konfiguracja: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd odczytu pliku konfiguracyjnego: " + e.getMessage());
            serverAddress = "localhost";
            port = 4999;
            System.err.println("Używam domyślnych ustawień serwera: " + serverAddress + ":" + port);
        }

        if (serverAddress == null || port == 0) {
            System.err.println("Niepoprawna konfiguracja serwera. Program zakończy działanie.");
            return;
        }

        // Połączenie z serwerem
        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            //  ID studenta
            String studentIdRequest = in.readLine();
            System.out.println(studentIdRequest);
            System.out.print("Twoje ID: ");

            //  System.in dla większej kontroli, w innym wypadku pojawialy sie bledy
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String studentId = consoleReader.readLine();
            out.println(studentId);

            // czyszczenie bufora wejściowego
            Runnable flushInputBuffer = () -> {
                try {
                    while (System.in.available() > 0) {
                        System.in.read();
                    }
                } catch (IOException e) {
                    System.err.println("Błąd przy czyszczeniu bufora wejścia: " + e.getMessage());
                }
            };


            StringBuilder pelnePytanie = new StringBuilder();
            String linia;
            boolean odczytujePytanie = true;

            // odbieranie pytan i wysywlanie odpowiedzi
            while ((linia = in.readLine()) != null) {
                // Sprawdzenie czy test się zakończył
                if (linia.equals("KONIEC_TESTU")) {
                    String wynik = in.readLine();
                    System.out.println("Twój wynik: " + wynik);
                    break;
                }


                if (odczytujePytanie) {
                    if (linia.matches("\\d+")) {
                        int czasNaOdpowiedz = Integer.parseInt(linia);
                        odczytujePytanie = false;

                        System.out.println("Pytanie: " + pelnePytanie.toString());
                        System.out.println("Czas na odpowiedź: " + czasNaOdpowiedz + " sekund");
                        System.out.print("Twoja odpowiedź (A/B/C/D): ");


                        AtomicBoolean odpowiedzWyslana = new AtomicBoolean(false);
                        Timer timer = new Timer(true);

                        // timeout
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (!odpowiedzWyslana.get()) {
                                    System.out.println("\nCzas na odpowiedź minął!");
                                    System.out.println("Wysyłam odpowiedź: BRAK_ODPOWIEDZI");
                                    out.println("BRAK_ODPOWIEDZI");
                                    odpowiedzWyslana.set(true);
                                }
                            }
                        }, czasNaOdpowiedz * 1000); // konwersja na milisekundy


                        InputStreamReader isr = new InputStreamReader(System.in);


                        String odpowiedz = null;
                        long endTime = System.currentTimeMillis() + czasNaOdpowiedz * 1000;
                        StringBuilder inputBuffer = new StringBuilder();


                        try {
                            while (System.currentTimeMillis() < endTime && !odpowiedzWyslana.get()) {

                                if (System.in.available() > 0) {
                                    int znak = System.in.read();
                                    if (znak == '\n' || znak == '\r') {

                                        odpowiedz = inputBuffer.toString().trim().toUpperCase();
                                        inputBuffer.setLength(0);
                                        if (!odpowiedzWyslana.get()) {
                                            odpowiedzWyslana.set(true);
                                            timer.cancel(); //wylaczenie timera po wyslaniu odpowiedzi

                                            // czy dobry format odpowiedzi [a-d]
                                            if (!odpowiedz.matches("[A-D]")) {
                                                System.out.println("Niepoprawny format odpowiedzi. Akceptowane są tylko A, B, C lub D.");
                                                odpowiedz = "BŁĄD";
                                            }

                                            System.out.println("Wysyłam odpowiedź: " + odpowiedz);
                                            out.println(odpowiedz);
                                            break;
                                        }
                                    } else {

                                        inputBuffer.append((char)znak);

                                        System.out.print((char)znak);
                                    }
                                }

                                try {

                                    Thread.sleep(50);
                                } catch (InterruptedException e) {

                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Błąd odczytu z konsoli: " + e.getMessage());
                        }

                        try {

                            while (System.in.available() > 0) {
                                System.in.read();
                            }
                        } catch (IOException e) {
                            System.err.println("Błąd przy czyszczeniu bufora wejścia: " + e.getMessage());
                        }


                        timer.cancel();


                        flushInputBuffer.run(); //czyszczenie buforu wejscia przed pytaniem

                        // Resetujemy na kolejne pytanie
                        pelnePytanie = new StringBuilder();
                        odczytujePytanie = true;
                    } else {
                        // Dodajemy linię do pytania
                        if (pelnePytanie.length() > 0) {
                            pelnePytanie.append("\n");
                        }
                        pelnePytanie.append(linia);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd klienta: " + e.getMessage());
        }
    }
}