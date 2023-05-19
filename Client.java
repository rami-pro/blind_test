import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class Client extends JFrame {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String playerName;
    private JLabel timerLabel;
    private JLabel questionLabel;
    private JTextField answerField;
    private JButton submitButton;
    private JLabel imageLabel;
    private Timer timer;
    private int remainingTime = 120;

    public Client(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error connecting to the server: " + e.getMessage());
            System.exit(1);
        }

        initializeUI();

        String playerName = "";
        CustomInputDialog dialog = new CustomInputDialog(null, "Bienvenue au BlindTest", true);
        playerName = dialog.showDialog();

        writer.println(playerName);

        new ServerListener().start();

        setVisible(true);
    }

    private void initializeUI() {

        setTitle("Blind Test Client");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        ImageIcon backgroundImage = new ImageIcon("resources/parcDesPrinces.jpeg");
        Image scaledImage = backgroundImage.getImage().getScaledInstance(800, 600, Image.SCALE_SMOOTH);
        backgroundImage = new ImageIcon(scaledImage);
        JLabel backgroundLabel = new JLabel(backgroundImage);
        backgroundLabel.setLayout(new BorderLayout());

        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new GridBagLayout());
        gamePanel.setOpaque(false);

        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);

        timerLabel = new JLabel("Time Remaining: -");
        timerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        timerLabel.setForeground(Color.LIGHT_GRAY); // Set the text color to light gray
        topPanel.add(timerLabel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 0.5;
        gbc.insets = new Insets(10, 0, 10, 0);
        gamePanel.add(topPanel, gbc);

        imageLabel = new JLabel();
        gbc.gridy = 1;
        gamePanel.add(imageLabel, gbc);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(175, 216, 248));

        questionLabel = new JLabel("Waiting for the game to start...");
        questionLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        bottomPanel.add(questionLabel);

        answerField = new JTextField(20);
        bottomPanel.add(answerField);

        submitButton = new JButton("Submit");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitAnswer();
            }
        });
        bottomPanel.add(submitButton);

        gbc.gridy = 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        gamePanel.add(bottomPanel, gbc);

        backgroundLabel.add(gamePanel, BorderLayout.CENTER);

        mainPanel.add(backgroundLabel, BorderLayout.CENTER);

        add(mainPanel);
        startTimer();
    }

    private void startTimer() {
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remainingTime--;
                timerLabel.setText("Time Remaining: " + remainingTime + "s");
                if (remainingTime == 0) {
                    timer.stop();
                    submitAnswer();
                }
            }
        });
    }

    private void submitAnswer() {
        String answer = answerField.getText().toLowerCase();
        if (!answer.isEmpty()) {
            sendMessage(new Message(MessageType.ANSWER, answer));
            answerField.setText("");
            submitButton.setEnabled(false);
            timer.stop();
        }
    }

    public void sendMessage(Message message) {
        System.out.println((message.toString()));
        writer.println(message.toString());
    }

    private void processMessage(String message) {
        String[] parts = message.split("\\|");
        MessageType type = MessageType.valueOf(parts[0]);
        String content = parts[1];

        switch (type) {
            case IMAGE:
                showImage(content);
                break;
            case QUESTION:
                showQuestion(content);
                break;
            case RESULT:
                showResult(content);
                break;
            case GAME_OVER:
                showGameOver();
                break;
            case SCORE:
                showScores(content);
                break;
        }
    }

    private void showImage(String imagePath) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ImageIcon imageIcon = new ImageIcon(imagePath);
                Image image = imageIcon.getImage().getScaledInstance(400, 400, Image.SCALE_DEFAULT);
                imageLabel.setIcon(new ImageIcon(image));
                imageLabel.revalidate();
                imageLabel.repaint();
            }
        });
    }

    private void showQuestion(String question) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                questionLabel.setText(question);
                submitButton.setEnabled(true);
                timer.start();
            }
        });
    }

    private void showResult(String result) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(Client.this, result);
            }
        });
    }

    private void showGameOver() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(Client.this, "Game Over");
                System.exit(0);
            }
        });
    }

    private void showScores(String scores) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(Client.this, "Scores:\n" + scores);
            }
        });
    }

    private class ServerListener extends Thread {
        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println(message);
                    processMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Error receiving message from the server: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1"; // Change this to the actual server address
        int serverPort = 8889; // Change this to the actual server port

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Client(serverAddress, serverPort);
            }
        });
    }

    private static class Message {
        private MessageType type;
        private Object content;

        public Message(MessageType type, String content) {
            this.type = type;
            this.content = content;
        }

        public Message(MessageType type, List<String> content) {
            this.type = type;
            this.content = content;
        }

        @Override
        public String toString() {
            return type.name() + "|" + content;
        }
    }

    private enum MessageType {
        IMAGE,
        QUESTION,
        RESULT,
        GAME_OVER,
        SCORE,
        TIME_OUT,
        ANSWER
    }
}
