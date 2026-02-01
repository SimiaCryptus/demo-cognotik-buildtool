package com.simiacryptus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CyberpunkHackingGame - A terminal-based hacking simulator.
 * Features a virtual file system, ANSI aesthetics, and logic-based mini-games.
 */
public class CyberpunkHackingGame {

    // ANSI Color Constants
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String RED = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";

    private final Scanner scanner = new Scanner(System.in);
    private final Directory root = new Directory("/");
    private Directory currentDir = root;
    private boolean running = true;
    private final Set<String> inventory = new HashSet<>();

    public static void main(String[] args) {
        new CyberpunkHackingGame().run();
    }

    public CyberpunkHackingGame() {
        setupFileSystem();
    }

    private void setupFileSystem() {
        Directory bin = new Directory("bin");
        bin.addChild(new File("decrypt.exe", "System decryption utility. Use 'decrypt <filename>'"));
        bin.addChild(new File("scanner.exe", "Network scanning tool."));
        
        Directory logs = new Directory("logs");
        logs.addChild(new File("access.log", "192.168.1.1 - SUCCESS\n192.168.1.45 - DENIED\n10.0.0.5 - SUCCESS"));
        logs.addChild(new File("system.log", "Kernel panic at 0x004F32. Rebooting..."));

        Directory privateDir = new Directory("private");
        privateDir.setLocked(true);
        File secret = new File("project_omega.txt", "The secret password for the mainframe is: NEUROMANCER_1984");
        secret.setEncrypted(true);
        privateDir.addChild(secret);

        root.addChild(bin);
        root.addChild(logs);
        root.addChild(privateDir);
        root.addChild(new File("readme.txt", "Welcome to the Neural-Link Terminal v4.2.0.\nFind the secret data to complete your mission."));
    }

    public void run() {
        printHeader();
        while (running) {
            System.out.print(BRIGHT_GREEN + "user@net-node:" + CYAN + getPath(currentDir) + BRIGHT_GREEN + "$ " + RESET);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            processCommand(input);
        }
        System.out.println(GREEN + "Connection closed. Goodbye, Operator." + RESET);
    }

    private void printHeader() {
        System.out.println(BRIGHT_GREEN + "====================================================");
        System.out.println("   NEURAL-LINK TERMINAL v4.2.0 - SECURE ACCESS      ");
        System.out.println("====================================================" + RESET);
        System.out.println(GREEN + "Type 'help' for a list of commands." + RESET);
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "ls": listFiles(); break;
            case "cd": changeDirectory(arg); break;
            case "cat": readFile(arg); break;
            case "hack": hackDirectory(arg); break;
            case "decrypt": decryptFile(arg); break;
            case "help": printHelp(); break;
            case "exit": running = false; break;
            case "clear": System.out.print("\033[H\033[2J"); System.out.flush(); break;
            default:
                System.out.println(RED + "Command not found: " + cmd + RESET);
        }
    }

    private void listFiles() {
        for (Node node : currentDir.children.values()) {
            String prefix = node instanceof Directory ? "[DIR] " : "[FILE]";
            String suffix = node.isLocked() || (node instanceof File && ((File)node).isEncrypted()) ? " (LOCKED)" : "";
            System.out.println(CYAN + prefix + " " + RESET + node.name + YELLOW + suffix + RESET);
        }
    }

    private void changeDirectory(String name) {
        if (name.equals("..")) {
            if (currentDir.parent != null) currentDir = currentDir.parent;
            return;
        }
        Node node = currentDir.children.get(name);
        if (node instanceof Directory) {
            if (node.isLocked()) {
                System.out.println(RED + "Access Denied: Directory is locked. Use 'hack <name>' to bypass." + RESET);
            } else {
                currentDir = (Directory) node;
            }
        } else {
            System.out.println(RED + "Directory not found: " + name + RESET);
        }
    }

    private void readFile(String name) {
        Node node = currentDir.children.get(name);
        if (node instanceof File) {
            File file = (File) node;
            if (file.isEncrypted()) {
                System.out.println(RED + "Error: File is encrypted. Use 'decrypt <name>'." + RESET);
            } else {
                System.out.println(GREEN + "--- " + name + " ---" + RESET);
                System.out.println(file.content);
                System.out.println(GREEN + "--- EOF ---" + RESET);
                if (file.content.contains("NEUROMANCER_1984")) {
                    System.out.println(YELLOW + "\n[MISSION COMPLETE] You've recovered the secret data!" + RESET);
                }
            }
        } else {
            System.out.println(RED + "File not found: " + name + RESET);
        }
    }

    private void hackDirectory(String name) {
        Node node = currentDir.children.get(name);
        if (node instanceof Directory && node.isLocked()) {
            System.out.println(YELLOW + "Initializing bypass sequence for " + name + "..." + RESET);
            if (playSequenceGame(4)) {
                node.setLocked(false);
                System.out.println(BRIGHT_GREEN + "SUCCESS: Security bypassed." + RESET);
            } else {
                System.out.println(RED + "FAILURE: Security lockout detected." + RESET);
            }
        } else {
            System.out.println(CYAN + "Target is not a locked directory." + RESET);
        }
    }

    private void decryptFile(String name) {
        Node node = currentDir.children.get(name);
        if (node instanceof File && ((File) node).isEncrypted()) {
            System.out.println(YELLOW + "Initializing decryption for " + name + "..." + RESET);
            if (playMathGame()) {
                ((File) node).setEncrypted(false);
                System.out.println(BRIGHT_GREEN + "SUCCESS: File decrypted." + RESET);
            } else {
                System.out.println(RED + "FAILURE: Decryption key rejected." + RESET);
            }
        } else {
            System.out.println(CYAN + "Target is not an encrypted file." + RESET);
        }
    }

    private boolean playSequenceGame(int length) {
        Random rand = new Random();
        String sequence = rand.ints(length, 0, 10)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(" "));
        
        System.out.println(CYAN + "Memorize this sequence: " + YELLOW + sequence + RESET);
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        System.out.print("\033[2K\r"); // Clear line
        System.out.print(CYAN + "Enter sequence: " + RESET);
        String input = scanner.nextLine().trim();
        return input.replace(" ", "").equals(sequence.replace(" ", ""));
    }

    private boolean playMathGame() {
        Random rand = new Random();
        int a = rand.nextInt(20) + 10;
        int b = rand.nextInt(20) + 10;
        System.out.print(CYAN + "Solve for decryption key: " + a + " + " + b + " = " + RESET);
        try {
            int answer = Integer.parseInt(scanner.nextLine().trim());
            return answer == (a + b);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void printHelp() {
        System.out.println(GREEN + "Available Commands:" + RESET);
        System.out.println("  ls              - List files and directories");
        System.out.println("  cd <dir>        - Change directory (use '..' to go up)");
        System.out.println("  cat <file>      - Read file content");
        System.out.println("  hack <dir>      - Bypass directory security");
        System.out.println("  decrypt <file>  - Decrypt an encrypted file");
        System.out.println("  clear           - Clear terminal screen");
        System.out.println("  help            - Show this help message");
        System.out.println("  exit            - Terminate connection");
    }

    private String getPath(Directory dir) {
        if (dir.parent == null) return "/";
        return getPath(dir.parent) + (dir.parent.parent == null ? "" : "/") + dir.name;
    }

    // --- File System Classes ---

    static abstract class Node {
        String name;
        Directory parent;
        private boolean locked = false;

        Node(String name) { this.name = name; }
        boolean isLocked() { return locked; }
        void setLocked(boolean locked) { this.locked = locked; }
    }

    static class Directory extends Node {
        Map<String, Node> children = new HashMap<>();
        Directory(String name) { super(name); }
        void addChild(Node node) {
            node.parent = this;
            children.put(node.name, node);
        }
    }

    static class File extends Node {
        String content;
        private boolean encrypted = false;
        File(String name, String content) {
            super(name);
            this.content = content;
        }
        boolean isEncrypted() { return encrypted; }
        void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
    }
}