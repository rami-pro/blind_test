import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;

public class Server {
    private static final int PORT = 8889;
    private static final int MAX_PLAYERS = 2;

    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private GameState gameState;
    private int currentPlayerIndex;

    public Server() {
        try {
            serverSocket = new ServerSocket(PORT);
            clients = new ArrayList<>();
            gameState = new GameState();
            currentPlayerIndex = 0;

            System.out.println("Server started on port " + PORT);
        } catch (IOException e) {
            System.out.println("Error starting the server: " + e.getMessage());
        }
    }

    public void start() {
        try {
            while (clients.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
                System.out.println("Player connected: " + clientHandler.getPlayerName());
            }

            System.out.println("Game started!");
            sendImagesToClients();
            startGame();

        } catch (IOException e) {
            System.out.println("Error accepting client connection: " + e.getMessage());
        }
    }

    private void sendImagesToClients() {
        // int nextImageIndex = (gameState.getCurrentImageIndex() + 1) %
        // gameState.getImagePaths().size();
        String imagePath = gameState.getCurrentImage();
        for (ClientHandler client : clients) {
            client.sendMessage(new Message(MessageType.IMAGE, imagePath));
        }
    }

    private void startGame() {
        gameState.shuffleQuestions(); // Mélanger les questions
        while (!gameState.isGameOver()) {
            ClientHandler currentPlayer = clients.get(currentPlayerIndex);
            sendImagesToClients();
            currentPlayer.sendMessage(new Message(MessageType.QUESTION, "Who is this player?"));
            currentPlayer.startTimer();

            // Wait for the current player's answer
            String answer = currentPlayer.waitForAnswer();

            if (gameState.isCorrectAnswer(answer)) {
                currentPlayer.incrementScore();
                currentPlayer.sendMessage(new Message(MessageType.RESULT, "Correct!"));
            } else {
                currentPlayer.sendMessage(new Message(MessageType.RESULT,
                        "Wrong! The correct answer is: " + gameState.getCurrentAnswer()));
            }

            currentPlayer.stopTimer();
            if (currentPlayerIndex == 1) {
                gameState.setCurrentImageIndex(gameState.getCurrentImageIndex() + 1);
            }

            currentPlayerIndex = (currentPlayerIndex + 1) % clients.size();
        }

        // Game over
        sendGameOverMessages();
        closeConnections();
    }

    private void sendGameOverMessages() {
        List<Integer> scores = new ArrayList<>();
        for (ClientHandler client : clients) {
            scores.add(client.getScore());
        }
        for (ClientHandler client : clients) {
            client.sendMessage(new Message(MessageType.GAME_OVER, "Game over! Final scores:"));
            client.sendMessage(new Message(MessageType.SCORE, gameState.getScores(scores.get(0), scores.get(1))));
        }
    }

    private void closeConnections() {
        try {
            for (ClientHandler client : clients) {
                client.close();
            }
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing connections: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    private static class GameState {
        private List<String> imagePaths;
        private List<String> answers;
        private int currentImageIndex;

        public int getCurrentImageIndex() {
            return currentImageIndex;
        }

        public void setCurrentImageIndex(int index) {
            currentImageIndex = index;
        }

        public GameState() {
            imagePaths = new ArrayList<>();
            answers = new ArrayList<>();

            imagePaths.add("resources/pogba.jpg");
            answers.add("pogba");

            imagePaths.add("resources/benzema.jpg");
            answers.add("benzema");

            imagePaths.add("resources/mbappe.jpg");
            answers.add("mbappe");

            imagePaths.add("resources/modric.jpg");
            answers.add("modric");

            imagePaths.add("resources/ronaldo.jpg");
            answers.add("ronaldo");

            imagePaths.add("resources/neymar.jpg");
            answers.add("neymar");

            currentImageIndex = 0;
        }

        public void shuffleQuestions() {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < imagePaths.size(); i++) {
                indices.add(i);
            }
            Collections.shuffle(indices);

            List<String> shuffledImagePaths = new ArrayList<>();
            List<String> shuffledAnswers = new ArrayList<>();

            for (int index : indices) {
                shuffledImagePaths.add(imagePaths.get(index));
                shuffledAnswers.add(answers.get(index));
            }

            imagePaths = shuffledImagePaths;
            answers = shuffledAnswers;

            currentImageIndex = 0;
        }

        public String getCurrentImage() {
            return imagePaths.get(currentImageIndex);
        }

        public boolean isCorrectAnswer(String answer) {
            String correctAnswer = answers.get(currentImageIndex);
            return answer.equalsIgnoreCase(correctAnswer);
        }

        public String getCurrentAnswer() {
            return answers.get(currentImageIndex);
        }

        public boolean isGameOver() {
            return currentImageIndex >= imagePaths.size();
        }

        public List<String> getScores(int score1, int score2) {
            List<String> scores = new ArrayList<>();
            scores.add("* Player 1: " + score1);
            scores.add("* Player 2: " + score2);
            return scores;
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split("\\|");
        MessageType type = MessageType.valueOf(parts[0]);
        String content = parts[1];
        ClientHandler currentPlayer = clients.get(currentPlayerIndex);

        switch (type) {
            case ANSWER:
                if (gameState.isCorrectAnswer(content)) {
                    currentPlayer.incrementScore();
                    currentPlayer.sendMessage(new Message(MessageType.RESULT, "Correct!"));
                } else {
                    currentPlayer.sendMessage(new Message(MessageType.RESULT,
                            "Wrong! The correct answer is: " + gameState.getCurrentAnswer()));
                }

                currentPlayer.stopTimer();
                if (currentPlayerIndex == 1) {
                    gameState.setCurrentImageIndex(gameState.getCurrentImageIndex() + 1);
                }

                currentPlayerIndex = (currentPlayerIndex + 1) % clients.size();
                break;
            case TIME_OUT:
                break;
            default:
                return;
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String playerName;
        private int score;
        private boolean isTimerRunning;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            score = 0;
            isTimerRunning = false;

            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            } catch (IOException e) {
                System.out.println("Error creating I/O streams for client: " + e.getMessage());
            }
        }

        public String getPlayerName() {
            return playerName;
        }

        public void sendMessage(Message message) {
            writer.println(message.toString());
        }

        public void incrementScore() {
            score++;
        }

        public int getScore() {
            return score;
        }

        public void startTimer() {
            isTimerRunning = true;
        }

        public void stopTimer() {
            isTimerRunning = false;
        }

        public String waitForAnswer() {
            try {
                while (true) {
                    String answer = reader.readLine();
                    if (answer != null) {
                        String[] parts = answer.split("\\|");
                        return parts[1];
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading client input: " + e.getMessage());
            }

            return null;
        }

        public void close() {
            try {
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing client connection: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                playerName = reader.readLine();
            } catch (IOException e) {
                System.out.println("Error reading player name: " + e.getMessage());
            }
        }
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
