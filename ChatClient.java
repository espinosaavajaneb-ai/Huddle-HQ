import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

/** Huddle HQ - LAN squad hub: chat, map vote, ready check. */
public class ChatClient extends JFrame {

    // ---- palette ----
    static final Color BG       = new Color(0x15, 0x16, 0x1B);
    static final Color PANEL    = new Color(0x1E, 0x20, 0x27);
    static final Color PANEL_2  = new Color(0x27, 0x2A, 0x33);
    static final Color ACCENT   = new Color(0xFF, 0xB0, 0x1F);
    static final Color GOOD     = new Color(0x3D, 0xDC, 0x97);
    static final Color BAD      = new Color(0xFF, 0x5C, 0x6C);
    static final Color TEXT     = new Color(0xEC, 0xEE, 0xF3);
    static final Color MUTED    = new Color(0x8B, 0x90, 0x9E);
    static final Font  UI       = new Font("Segoe UI", Font.PLAIN, 14);
    static final Font  UI_BOLD  = new Font("Segoe UI", Font.BOLD, 14);

    static final String[] MAPS = Protocol.MAPS;

    // ---- connection ----
    private Socket socket;
    private PrintWriter out;
    private boolean connected = false;
    private String myNick = "";
    private String myVote = null;

    // ---- widgets ----
    private final JTextField ipField = field("localhost", 11);
    private final JTextField portField = field(String.valueOf(Protocol.DEFAULT_PORT), 5);
    private final JTextField nickField = field("", 10);
    private final JButton connectBtn = button("Connect", ACCENT, BG);
    private final JLabel statusLabel = new JLabel("OFFLINE");
    private final JLabel statusDot = new JLabel("\u25CF");

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final List<JButton> navButtons = new ArrayList<>();

    private final JTextArea chatArea = new JTextArea();
    private final JTextField chatInput = field("", 30);
    private final JButton sendBtn = button("Send", ACCENT, BG);

    private final JLabel squadTitle = new JLabel("SQUAD (0)");
    private final DefaultListModel<String> squadModel = new DefaultListModel<>();

    private final Map<String, JButton> mapButtons = new LinkedHashMap<>();
    private final JPanel readyList = new JPanel();
    private final JLabel readySummary = new JLabel("0 / 0 ready");
    private final JButton readyBtn = button("I'M READY", GOOD, BG);
    private boolean iAmReady = false;

    public ChatClient() {
        super("Huddle HQ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(980, 660);
        setMinimumSize(new Dimension(820, 540));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildCards(), BorderLayout.CENTER);
        setContentPane(root);

        connectBtn.addActionListener(e -> { if (connected) disconnect(); else connect(); });
        nickField.addActionListener(e -> connectBtn.doClick());
        sendBtn.addActionListener(e -> sendChat());
        chatInput.addActionListener(e -> sendChat());
        readyBtn.addActionListener(e -> toggleReady());

        setConnectedUi(false);
        showCard("Chat");
    }

    // =====================================================  UI BUILDING
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PANEL);
        header.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel logo = new JLabel("HUDDLE");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logo.setForeground(ACCENT);
        JLabel hq = new JLabel(" HQ");
        hq.setFont(new Font("Segoe UI", Font.BOLD, 28));
        hq.setForeground(TEXT);
        JLabel sub = new JLabel("   squad hub  \u2022  LAN");
        sub.setFont(UI);
        sub.setForeground(MUTED);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(logo); left.add(hq); left.add(sub);

        statusDot.setForeground(BAD);
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(UI_BOLD);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        right.setOpaque(false);
        right.add(statusDot); right.add(statusLabel);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        JPanel conn = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        conn.setBackground(PANEL_2);
        conn.setBorder(new EmptyBorder(0, 8, 0, 8));
        conn.add(label("IP")); conn.add(ipField);
        conn.add(label("Port")); conn.add(portField);
        conn.add(label("Nickname")); conn.add(nickField);
        conn.add(connectBtn);

        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);
        wrap.add(top, BorderLayout.NORTH);
        wrap.add(conn, BorderLayout.CENTER);
        header.add(wrap);
        return header;
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(BG);
        side.setBorder(new EmptyBorder(16, 14, 14, 14));
        side.setPreferredSize(new Dimension(210, 0));

        for (String name : new String[]{"Chat", "Map Vote", "Ready Check"}) {
            JButton b = button(name, PANEL, TEXT);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.addActionListener(e -> showCard(name));
            navButtons.add(b);
            side.add(b);
            side.add(Box.createVerticalStrut(8));
        }

        side.add(Box.createVerticalStrut(14));
        squadTitle.setFont(UI_BOLD);
        squadTitle.setForeground(MUTED);
        squadTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(squadTitle);
        side.add(Box.createVerticalStrut(6));

        JList<String> list = new JList<>(squadModel);
        list.setBackground(PANEL);
        list.setForeground(TEXT);
        list.setFont(UI);
        list.setSelectionBackground(PANEL_2);
        list.setFocusable(false);
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(new LineBorder(PANEL_2));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(sp);
        return side;
    }

    private JComponent buildCards() {
        cardPanel.setBackground(BG);
        cardPanel.setBorder(new EmptyBorder(16, 4, 14, 16));
        cardPanel.add(buildChatCard(), "Chat");
        cardPanel.add(buildMapCard(), "Map Vote");
        cardPanel.add(buildReadyCard(), "Ready Check");
        return cardPanel;
    }

    private JComponent buildChatCard() {
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(PANEL);
        chatArea.setForeground(TEXT);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        chatArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        JScrollPane sp = new JScrollPane(chatArea);
        sp.setBorder(new LineBorder(PANEL_2));

        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.add(chatInput, BorderLayout.CENTER);
        bar.add(sendBtn, BorderLayout.EAST);

        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        p.add(sp, BorderLayout.CENTER);
        p.add(bar, BorderLayout.SOUTH);
        return p;
    }

    private JComponent buildMapCard() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);
        JLabel t = new JLabel("Vote for the next map");
        t.setFont(new Font("Segoe UI", Font.BOLD, 20));
        t.setForeground(TEXT);
        p.add(t, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        for (String m : MAPS) {
            JButton b = button(m + "   \u2022   0 votes", PANEL, TEXT);
            b.setFont(new Font("Segoe UI", Font.BOLD, 16));
            b.addActionListener(e -> vote(m));
            mapButtons.put(m, b);
            grid.add(b);
        }
        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildReadyCard() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setOpaque(false);
        JLabel t = new JLabel("Ready check");
        t.setFont(new Font("Segoe UI", Font.BOLD, 20));
        t.setForeground(TEXT);
        readySummary.setForeground(MUTED);
        readySummary.setFont(UI_BOLD);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(t, BorderLayout.WEST);
        head.add(readySummary, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);

        readyList.setLayout(new BoxLayout(readyList, BoxLayout.Y_AXIS));
        readyList.setBackground(PANEL);
        readyList.setBorder(new EmptyBorder(8, 12, 8, 12));
        JScrollPane sp = new JScrollPane(readyList);
        sp.setBorder(new LineBorder(PANEL_2));
        p.add(sp, BorderLayout.CENTER);

        readyBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        readyBtn.setPreferredSize(new Dimension(0, 46));
        p.add(readyBtn, BorderLayout.SOUTH);
        return p;
    }

    // =====================================================  HELPERS
    private static JTextField field(String text, int cols) {
        JTextField f = new JTextField(text, cols);
        f.setBackground(PANEL);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setFont(UI);
        f.setBorder(new CompoundBorder(new LineBorder(new Color(0x3A, 0x3E, 0x4A)), new EmptyBorder(6, 8, 6, 8)));
        return f;
    }

    private static JButton button(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(UI_BOLD);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(9, 16, 9, 16));
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setForeground(MUTED);
        l.setFont(UI_BOLD);
        return l;
    }

    private void showCard(String name) {
        cards.show(cardPanel, name);
        for (JButton b : navButtons) {
            boolean on = b.getText().equals(name);
            b.setBackground(on ? ACCENT : PANEL);
            b.setForeground(on ? BG : TEXT);
        }
    }

    private void setConnectedUi(boolean on) {
        connected = on;
        connectBtn.setText(on ? "Disconnect" : "Connect");
        connectBtn.setBackground(on ? BAD : ACCENT);
        connectBtn.setForeground(on ? Color.WHITE : BG);
        statusDot.setForeground(on ? GOOD : BAD);
        statusLabel.setText(on ? "ONLINE" : "OFFLINE");
        statusLabel.setForeground(on ? GOOD : MUTED);
        ipField.setEnabled(!on); portField.setEnabled(!on); nickField.setEnabled(!on);
        chatInput.setEnabled(on); sendBtn.setEnabled(on); readyBtn.setEnabled(on);
        for (JButton b : mapButtons.values()) b.setEnabled(on);
        if (!on) {
            squadModel.clear();
            squadTitle.setText("SQUAD (0)");
            readyList.removeAll();
            readyList.repaint();
            readySummary.setText("0 / 0 ready");
            iAmReady = false; myVote = null;
            readyBtn.setText("I'M READY");
            readyBtn.setBackground(GOOD);
            for (String m : MAPS) updateMapButton(m, 0);
        }
    }

    private void addLine(String s) {
        chatArea.append(s + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    // =====================================================  NETWORK
    private void connect() {
        String ip = ipField.getText().trim();
        String nick = Protocol.cleanNick(nickField.getText());
        int port;
        try { port = Integer.parseInt(portField.getText().trim()); }
        catch (NumberFormatException e) { addLine("[!] Port must be a number."); return; }
        if (nick.isEmpty()) { addLine("[!] Enter a nickname first."); return; }
        myNick = nick;
        connectBtn.setEnabled(false);

        new Thread(() -> {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(ip, port), 4000);
                socket = s;
                out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
                out.println(Protocol.build(Protocol.JOIN, nick));
                SwingUtilities.invokeLater(() -> { connectBtn.setEnabled(true); setConnectedUi(true); addLine("* Connected as " + nick); });
                String line;
                while ((line = in.readLine()) != null) {
                    final String l = line;
                    SwingUtilities.invokeLater(() -> handle(l));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> addLine("[!] Could not connect: " + ex.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> { connectBtn.setEnabled(true); setConnectedUi(false); });
            }
        }).start();
    }

    private void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void send(String msg) {
        if (out != null) out.println(msg);
    }

    private void handle(String line) {
        String[] p = Protocol.parse(line, 3);
        switch (p[0]) {
            case Protocol.CHAT:
                if (p.length >= 3) addLine(p[1] + ": " + p[2]);
                break;
            case Protocol.SYS:
                if (p.length >= 2) addLine("* " + p[1]);
                break;
            case Protocol.ERR:
                if (p.length >= 2) addLine("[!] " + p[1]);
                break;
            case Protocol.USERS: {
                squadModel.clear();
                String[] names = p.length > 1 && !p[1].isEmpty() ? p[1].split(",") : new String[0];
                for (String n : names) squadModel.addElement(n.equals(myNick) ? n + " (you)" : n);
                squadTitle.setText("SQUAD (" + names.length + ")");
                break;
            }
            case Protocol.VOTES:
                if (p.length > 1) for (String pair : p[1].split(",")) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) updateMapButton(kv[0], Integer.parseInt(kv[1]));
                }
                break;
            case Protocol.READY_LIST:
                updateReady(p.length > 1 ? p[1] : "");
                break;
        }
    }

    // =====================================================  ACTIONS
    private void sendChat() {
        String t = chatInput.getText().trim();
        if (t.isEmpty() || !connected) return;
        send(Protocol.build(Protocol.CHAT, t));
        chatInput.setText("");
    }

    private void vote(String map) {
        myVote = map;
        send(Protocol.build(Protocol.VOTE, map));
    }

    private void toggleReady() {
        iAmReady = !iAmReady;
        send(Protocol.build(Protocol.READY, iAmReady ? "1" : "0"));
        readyBtn.setText(iAmReady ? "CANCEL READY" : "I'M READY");
        readyBtn.setBackground(iAmReady ? BAD : GOOD);
        readyBtn.setForeground(iAmReady ? Color.WHITE : BG);
    }

    private void updateMapButton(String map, int count) {
        JButton b = mapButtons.get(map);
        if (b == null) return;
        b.setText(map + "   \u2022   " + count + (count == 1 ? " vote" : " votes"));
        boolean mine = map.equals(myVote);
        b.setBackground(mine ? ACCENT : PANEL);
        b.setForeground(mine ? BG : TEXT);
    }

    private void updateReady(String data) {
        readyList.removeAll();
        int total = 0, ok = 0;
        if (!data.isEmpty()) for (String pair : data.split(",")) {
            String[] kv = pair.split(":");
            if (kv.length != 2) continue;
            total++;
            boolean r = kv[1].equals("1");
            if (r) ok++;
            JLabel l = new JLabel((r ? "\u2714  " : "\u2022  ") + kv[0] + (r ? "  - ready" : "  - waiting"));
            l.setFont(new Font("Segoe UI", Font.BOLD, 15));
            l.setForeground(r ? GOOD : MUTED);
            l.setBorder(new EmptyBorder(6, 0, 6, 0));
            readyList.add(l);
        }
        readySummary.setText(ok + " / " + total + " ready");
        readySummary.setForeground(total > 0 && ok == total ? GOOD : MUTED);
        readyList.revalidate();
        readyList.repaint();
    }

    // =====================================================  MAIN
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ChatClient().setVisible(true));
    }
}
