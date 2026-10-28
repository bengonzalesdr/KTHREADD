package dev.kthreadd.core;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

public final class ProfileStore {
    private final Path root;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Set<String> blocked = new HashSet<>();
    public String lastError;
    public ProfileStore(Path root) { this.root = root.toAbsolutePath().normalize(); }
    private Path path(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("Profile name: use 1-64 letters, digits, _ or -");
        return root.resolve(name + ".json");
    }
    public Profile load(String name) {
        Path file = path(name);
        lastError=null;
        if (!Files.exists(file)) return new Profile();
        try {
            Profile p=read(file); blocked.remove(name); return p;
        } catch (IOException | RuntimeException e) {
            blocked.add(name); lastError = "Cannot load profile " + name + "; original preserved: " + e.getMessage();
            try { Files.copy(file, root.resolve(name + "-" + System.currentTimeMillis() + ".broken")); } catch (IOException ignored) { }
            try {
                Profile recovered=read(root.resolve(name+".json.bak"));
                lastError="Recovered backup for " + name + "; damaged original preserved. Save under a new name.";
                return recovered;
            } catch(IOException | RuntimeException ignored) { }
            return new Profile();
        }
    }
    private Profile read(Path file) throws IOException {
        if(Files.size(file)>2_000_000) throw new IOException("Profile exceeds 2 MB");
        Profile p=gson.fromJson(Files.readString(file),Profile.class);
        if(p==null || p.schema!=1) throw new IOException("Unsupported profile schema");
        p.sanitize(); return p;
    }
    public void save(String name, Profile p) throws IOException {
        Path file = path(name);
        if (blocked.contains(name)) throw new IllegalStateException("Damaged profile preserved; save under a new name to recover");
        Files.createDirectories(root); p.sanitize();
        Path temp = Files.createTempFile(root, name + "-", ".tmp");
        try {
            Files.writeString(temp, gson.toJson(p));
            if(Files.exists(file)) {
                // Validate before rotating: a damaged original must never replace a good backup.
                read(file);
                Path backupTemp=Files.createTempFile(root,name+"-backup-",".tmp");
                try { Files.copy(file,backupTemp,StandardCopyOption.REPLACE_EXISTING); replace(backupTemp,root.resolve(name+".json.bak")); }
                finally { Files.deleteIfExists(backupTemp); }
            }
            replace(temp,file);
        } finally { Files.deleteIfExists(temp); }
    }
    private static void replace(Path source,Path target) throws IOException {
        try { Files.move(source,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
        catch(AtomicMoveNotSupportedException e) { Files.move(source,target,StandardCopyOption.REPLACE_EXISTING); }
    }
    public List<String> list() throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        try (var files = Files.list(root)) { return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).map(n -> n.substring(0,n.length()-5)).sorted().toList(); }
    }
    public static String serverKey(String address) {
        try { return "server-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(address.getBytes(StandardCharsets.UTF_8))).substring(0,24); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
