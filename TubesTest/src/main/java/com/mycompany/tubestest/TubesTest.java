package com.mycompany.tubestest;

import com.mycompany.tubestest.web.CleanHubWebServer;
import com.mycompany.tubestest.web.PortHelper;

import java.awt.Desktop;
import java.net.URI;

/**
 * Main class — menjalankan web (default) dan/atau mode konsol.
 *
 * <ul>
 *   <li>Tanpa argumen: web di port 8080</li>
 *   <li>--console: mode konsol saja</li>
 *   <li>--web: web saja (eksplisit)</li>
 *   <li>--port 9090: ganti port web</li>
 * </ul>
 */
public class TubesTest {

    public static void main(String[] args) throws Exception {
        boolean runWeb = true;
        boolean runConsole = false;
        int port = 8080;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--console" -> runConsole = true;
                case "--web" -> runWeb = true;
                case "--no-web" -> runWeb = false;
                case "--port" -> {
                    if (i + 1 < args.length) {
                        port = Integer.parseInt(args[++i]);
                    }
                }
                default -> {
                    if (args[i].startsWith("--")) {
                        System.out.println("Argumen tidak dikenal: " + args[i]);
                    }
                }
            }
        }

        if (args.length > 0 && !contains(args, "--web") && !contains(args, "--no-web")) {
            if (contains(args, "--console")) {
                runWeb = false;
            }
        }

        if (runWeb) {
            PortHelper.tryFreePort(port);
            try {
                CleanHubWebServer.start(port);
                String url = "http://localhost:" + port + "/";
                System.out.println();
                System.out.println("=== CleanHub siap ===");
                System.out.println("Buka: " + url);
                System.out.println("(NetBeans: biarkan proses ini jalan, jangan tutup Output)");
                openBrowser(url);
            } catch (Exception e) {
                System.err.println("[ERROR] Gagal menjalankan web server: " + e.getMessage());
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    System.err.println("        " + e.getCause().getMessage());
                }
                System.err.println("        → Pastikan XAMPP MySQL ON");
                System.err.println("        → Stop Tomcat di XAMPP jika port " + port + " bentrok");
                System.err.println("        → Atau jalankan dengan argumen: --port 9090");
                return;
            }
        }

        if (runConsole) {
            if (runWeb) {
                new Thread(() -> new LaundrySystem().start(), "console").start();
            } else {
                new LaundrySystem().start();
                return;
            }
        }

        if (runWeb) {
            CleanHubWebServer.join();
        }
    }

    private static boolean contains(String[] args, String value) {
        for (String arg : args) {
            if (value.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            System.out.println("Buka browser manual: " + url);
        }
    }
}
