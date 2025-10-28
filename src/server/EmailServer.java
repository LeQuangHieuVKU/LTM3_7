package server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class EmailServer {
    private static final int SMTP_PORT = 2525;
    private static final int POP3_PORT = 8110;
    private static final UserManager userManager = new UserManager();
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        start(SMTP_PORT, EmailServer::handleSMTP);
        start(POP3_PORT, EmailServer::handlePOP3);
        System.out.println("Server: SMTP 2525 | POP3 8110");
    }

    private static void start(int port, ClientHandler handler) {
        pool.submit(() -> {
            try (var server = new ServerSocket(port)) {
                while (true) {
                    Socket client = server.accept();
                    pool.submit(() -> handler.handle(client));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // ============================= SMTP =============================
    private static void handleSMTP(Socket client) {
        try (client;
             var in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             var out = new PrintWriter(client.getOutputStream(), true)) {

            out.println("220 localhost SMTP");
            String from = null, to = null;
            StringBuilder rawData = new StringBuilder();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("HELO")) out.println("250 OK");
                else if (line.startsWith("MAIL FROM:")) {
                    from = extractUser(line.substring(10).trim());
                    out.println("250 OK");
                }
                else if (line.startsWith("RCPT TO:")) {
                    to = extractUser(line.substring(8).trim());
                    out.println("250 OK");
                }
                else if (line.equals("DATA")) {
                    out.println("354 End data with .");
                    while (!(line = in.readLine()).equals(".")) {
                        rawData.append(line).append("\n");
                    }

                    String rawContent = rawData.toString().trim();
                    String finalFrom = from;
                    String finalTo = to;
                    String finalSubject = extractSubjectFromData(rawContent);
                    String body = extractBodyFromData(rawContent);

                    saveEmail(finalFrom, finalTo, finalSubject, body);
                    out.println("250 OK");
                }
                else if (line.equals("QUIT")) {
                    out.println("221 Bye");
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private static String extractUser(String addr) {
        return addr.replaceAll("[<@>]", "").trim();
    }

    private static String extractSubjectFromData(String data) {
        for (String line : data.split("\n")) {
            if (line.startsWith("Subject: ")) return line.substring(9).trim();
        }
        return "No Subject";
    }

    private static String extractBodyFromData(String data) {
        boolean inBody = false;
        StringBuilder body = new StringBuilder();
        for (String line : data.split("\n")) {
            if (line.trim().isEmpty()) {
                inBody = true;
                continue;
            }
            if (inBody) body.append(line).append("\n");
        }
        return body.toString().trim();
    }

    // LƯU VÀO INBOX + SENT
    private static void saveEmail(String from, String to, String subject, String content) {
        saveToFolder(userManager.getMailbox(to).resolve("inbox"), from, to, subject, content);
        saveToFolder(userManager.getMailbox(from).resolve("sent"), from, to, subject, content);
    }

    private static void saveToFolder(Path folder, String from, String to, String subject, String content) {
        try {
            Files.createDirectories(folder);
            Path file = folder.resolve(System.currentTimeMillis() + ".eml");
            String email =
                    "From: " + from + "\n" +
                            "To: " + to + "\n" +
                            "Subject: " + subject + "\n" +
                            "\n" +
                            content;
            Files.writeString(file, email);
        } catch (Exception ignored) {}
    }

    // ============================= POP3 =============================
    private static void handlePOP3(Socket client) {
        try (client;
             var in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             var out = new PrintWriter(client.getOutputStream(), true)) {

            out.println("+OK POP3 ready");
            String user = null;
            boolean auth = false;

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("USER ")) { user = line.substring(5).trim(); out.println("+OK"); }
                else if (line.startsWith("PASS ")) {
                    auth = userManager.login(user, line.substring(5).trim());
                    out.println(auth ? "+OK" : "-ERR");
                }
                else if (line.equals("STAT") && auth) {
                    int[] stats = getStats(user);
                    out.println("+OK " + stats[0] + " " + stats[1]);
                }
                else if (line.equals("LIST") && auth) { listEmails(user, out); }
                else if (line.startsWith("RETR ") && auth) { retrieveEmail(user, Integer.parseInt(line.substring(5).trim()), out); }
                else if (line.startsWith("TOP ") && auth) { topEmail(user, Integer.parseInt(line.split(" ")[1]), out); }
                else if (line.equals("QUIT")) { out.println("+OK"); break; }
                else out.println("-ERR");
            }
        } catch (Exception ignored) {}
    }

    private static int[] getStats(String user) {
        Path inbox = userManager.getMailbox(user).resolve("inbox");
        if (!Files.exists(inbox)) return new int[]{0, 0};
        try (var stream = Files.list(inbox)) {
            var files = stream.filter(f -> f.toString().endsWith(".eml")).toList();
            long size = files.stream().mapToLong(f -> {
                try { return Files.size(f); } catch (Exception e) { return 0; }
            }).sum();
            return new int[]{files.size(), (int) size};
        } catch (Exception e) { return new int[]{0, 0}; }
    }

    private static void listEmails(String user, PrintWriter out) throws IOException {
        Path inbox = userManager.getMailbox(user).resolve("inbox");
        out.println("+OK");
        if (Files.exists(inbox)) {
            var files = Files.list(inbox)
                    .filter(f -> f.toString().endsWith(".eml"))
                    .sorted(Comparator.comparingLong(f -> {
                        try { return Files.getLastModifiedTime(f).toMillis(); }
                        catch (Exception e) { return 0; }
                    })).toList();
            for (int i = 0; i < files.size(); i++) {
                out.println((i+1) + " " + Files.size(files.get(i)));
            }
        }
        out.println(".");
    }

    private static void retrieveEmail(String user, int num, PrintWriter out) throws IOException {
        Path inbox = userManager.getMailbox(user).resolve("inbox");
        var files = Files.list(inbox)
                .filter(f -> f.toString().endsWith(".eml"))
                .sorted(Comparator.comparingLong(f -> {
                    try { return Files.getLastModifiedTime(f).toMillis(); }
                    catch (Exception e) { return 0; }
                })).toList();
        if (num < 1 || num > files.size()) { out.println("-ERR"); return; }
        out.println("+OK");
        out.println(Files.readString(files.get(num - 1)));
        out.println(".");
    }

    private static void topEmail(String user, int num, PrintWriter out) throws IOException {
        retrieveEmail(user, num, out);
    }

    @FunctionalInterface interface ClientHandler { void handle(Socket client); }
}