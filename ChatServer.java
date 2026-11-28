import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final Map<String, String> votes = new ConcurrentHashMap<>();  
    private static final Set<String> ready = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Huddle HQ server listening on port " + port);
            while (true) {
                Socket s = server.accept();
                new Thread(new ClientHandler(s)).start();
            }
        }
    }

   
    static boolean register(String nick, ClientHandler handler) {
        return clients.putIfAbsent(nick, handler) == null;
    }

    static void remove(String nick) {
        clients.remove(nick);
        votes.remove(nick);
        ready.remove(nick);
    }

    static void setVote(String nick, String map) {
        votes.put(nick, map);
        sendState();
    }

    static void setReady(String nick, boolean isReady) {
        if (isReady) ready.add(nick); else ready.remove(nick);
        sendState();
    }

    static void broadcast(String message) {
        for (ClientHandler h : clients.values()) h.send(message);
    }

    static void sendState() {
        Set<String> names = new TreeSet<>(clients.keySet());

        broadcast(Protocol.build(Protocol.USERS, String.join(",", names)));

        List<String> counts = new ArrayList<>();
        for (String map : Protocol.MAPS) {
            int c = 0;
            for (String v : votes.values()) if (v.equals(map)) c++;
            counts.add(map + ":" + c);
        }
        broadcast(Protocol.build(Protocol.VOTES, String.join(",", counts)));

        List<String> states = new ArrayList<>();
        for (String n : names) states.add(n + ":" + (ready.contains(n) ? 1 : 0));
        broadcast(Protocol.build(Protocol.READY_LIST, String.join(",", states)));
    }
}
