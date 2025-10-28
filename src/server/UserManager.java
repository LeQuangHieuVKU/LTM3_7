package server;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private static final String BASE_DIR = "server_users";
    private final Map<String, String> users = new ConcurrentHashMap<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public UserManager() {
        loadUsers();
    }

    private void loadUsers() {
        File dir = new File(BASE_DIR);
        if (!dir.exists()) dir.mkdirs();
        for (File userDir : dir.listFiles(File::isDirectory)) {
            File info = new File(userDir, "info.txt");
            if (info.exists()) {
                try (var br = new BufferedReader(new FileReader(info))) {
                    String line = br.readLine();
                    if (line != null) {
                        String[] parts = line.split(",", 3);
                        if (parts.length >= 2) {
                            users.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public synchronized boolean register(String username, String password) {
        if (username.isBlank() || password.isBlank() || users.containsKey(username)) {
            return false;
        }
        Path userPath = Path.of(BASE_DIR, username);
        try {
            Files.createDirectories(userPath.resolve("inbox"));
            String timestamp = sdf.format(new Date());
            String info = username + "," + password + "," + timestamp;
            Files.writeString(userPath.resolve("info.txt"), info);
            users.put(username, password);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean login(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }

    public Path getMailbox(String username) {
        return Path.of(BASE_DIR, username);
    }
}