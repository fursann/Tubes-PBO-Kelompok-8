package com.mycompany.tubestest.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/** Membebaskan port agar NetBeans Build & Run tidak bentrok dengan proses lama. */
public final class PortHelper {

    private PortHelper() {
    }

    public static void tryFreePort(int port) {
        if (port < 1 || port > 65535) {
            return;
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            freePortWindows(port);
        }
    }

    private static void freePortWindows(int port) {
        Set<String> pids = new HashSet<>();
        try {
            Process netstat = new ProcessBuilder("netstat", "-ano").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(netstat.getInputStream()))) {
                String line;
                String portToken = ":" + port;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains(portToken) || !line.contains("LISTENING")) {
                        continue;
                    }
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 0) {
                        String pid = parts[parts.length - 1];
                        if (pid.matches("\\d+")) {
                            pids.add(pid);
                        }
                    }
                }
            }
            netstat.waitFor();
        } catch (Exception e) {
            System.err.println("[WARN] Cek port " + port + " gagal: " + e.getMessage());
            return;
        }

        for (String pid : pids) {
            try {
                new ProcessBuilder("taskkill", "/F", "/PID", pid).start().waitFor();
                System.out.println("[Server] Port " + port + " dibebaskan (PID " + pid + ").");
            } catch (Exception ignored) {
            }
        }
    }
}
