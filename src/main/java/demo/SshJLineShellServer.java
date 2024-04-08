package demo;

import java.io.IOException;
import java.util.function.Consumer;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.jline.builtins.ssh.ShellFactoryImpl;
import org.jline.builtins.ssh.Ssh;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.terminal.Terminal;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class SshJLineShellServer {
    static {
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }


    public static void main(String[] args) throws IOException {
        SshServer sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(2222);
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshServer.setPasswordAuthenticator(new MyPasswordAuthenticator());
        final Consumer<Ssh.ShellParams> shell = SshJLineShellServer::shell;
        sshServer.setShellFactory(new ShellFactoryImpl(shell));
        sshServer.start();
        System.out.println("SSH Server started on port 2222. Use username 'admin' and password 'admin' to login.");

        // Block the main thread and wait indefinitely
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Server interrupted, shutting down.");
            Thread.currentThread().interrupt();
        } finally {
            sshServer.stop();
        }
    }

    private static class MyPasswordAuthenticator implements PasswordAuthenticator {
        @Override
        public boolean authenticate(String username, String password, ServerSession session) {
            return "admin".equals(username) && "admin".equals(password);
        }
    }

    private static void shell(Ssh.ShellParams shell) {
        Terminal terminal = shell.getTerminal();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        KeyMap<Binding> map = reader.getKeyMaps().get(LineReader.MAIN);
        map.bind(new Reference(LineReader.MENU_COMPLETE), "?");
        boolean run = true;
        while (run) {
            try {
                terminal.writer().println("""
                        Choose an option (type 1, 2, or 3):
                        1. Hello
                        2. World
                        3. Demo
                        Type 'exit' to quit.""");
                terminal.flush();

                String string = reader.readLine("> "); // Added prompt symbol
                if ("exit".equalsIgnoreCase(string.trim())) {
                    // Exit command to break the loop
                    shell.getCloser().run();
                    run = false;
                    continue;
                }

                switch (string.trim()) {
                    case "1" -> terminal.writer().println("You chose 'Hello'");
                    case "2" -> terminal.writer().println("You chose 'World'");
                    case "3" -> terminal.writer().println("You chose 'Demo'");
                    default -> terminal.writer().println("Invalid option, please choose 1, 2, or 3.");
                }
                terminal.flush();
            } catch (EndOfFileException ex) {
                shell.getCloser().run();
                run = false;
            } catch (Exception ignored) {
            }
        }
    }

}
