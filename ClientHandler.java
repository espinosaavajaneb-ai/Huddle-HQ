import java.io.*;
import java.net.Socket;

/** Runs on its own thread on the server: talks to exactly one connected client. */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private String nick;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void send(String message) {
        if (out != null) out.println(message);
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String first = in.readLine();
            if (first == null) return;
            String[] join = Protocol.parse(first, 2);
            if (!join[0].equals(Protocol.JOIN) || join.length < 2) return;

            String wanted = Protocol.cleanNick(join[1]);
            if (wanted.isEmpty() || wanted.length() > 16 || !ChatServer.register(wanted, this)) {
                send(Protocol.build(Protocol.ERR, "Nickname is empty, too long, or already taken."));
                return;
            }
            nick = wanted;
            ChatServer.broadcast(Protocol.build(Protocol.SYS, nick + " joined the huddle."));
            ChatServer.sendState();

            String line;
            while ((line = in.readLine()) != null) {
                String[] p = Protocol.parse(line, 2);
                if (p.length < 2) continue;
                switch (p[0]) {
                    case Protocol.CHAT:
                        if (!p[1].trim().isEmpty())
                            ChatServer.broadcast(Protocol.build(Protocol.CHAT, nick, p[1]));
                        break;
                    case Protocol.VOTE:
                        ChatServer.setVote(nick, p[1]);
                        break;
                    case Protocol.READY:
                        ChatServer.setReady(nick, p[1].equals("1"));
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (nick != null) {
                ChatServer.remove(nick);
                ChatServer.broadcast(Protocol.build(Protocol.SYS, nick + " left the huddle."));
                ChatServer.sendState();
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
