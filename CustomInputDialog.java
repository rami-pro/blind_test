import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CustomInputDialog extends JDialog {
    private JTextField textField;
    private JButton validateButton;
    private String start = "Enter your name";

    public CustomInputDialog(Frame parent, String title, boolean modal) {
        super(parent, title, modal);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Create the main panel with BorderLayout
        JPanel panel = new JPanel(new BorderLayout());

        // Create the background image as the background
        ImageIcon backgroundImageIcon = new ImageIcon("resources/stade.jpeg");
        JLabel backgroundLabel = new JLabel(backgroundImageIcon);
        backgroundLabel.setLayout(new BorderLayout());

        // Create the top panel to hold components above the image
        JPanel topPanel = new JPanel();
        topPanel.setBackground(new Color(0, 0, 0, 0));
        topPanel.setLayout(new BorderLayout());

        // Create the title label with a nice font
        JLabel titleLabel = new JLabel(start);
        Font titleFont = new Font(Font.SERIF, Font.BOLD, 24);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(Color.LIGHT_GRAY); // Set the text color to light gray
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(titleLabel, BorderLayout.CENTER);

        backgroundLabel.add(topPanel, BorderLayout.NORTH);

        // Create the bottom panel to hold components below the image
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(0, 0, 0, 0));
        bottomPanel.setLayout(new BorderLayout());

        // Create the text field for the username
        textField = new JTextField();
        textField.setPreferredSize(new Dimension(300, 30)); // Fixed size of the input field
        bottomPanel.add(textField, BorderLayout.CENTER);

        // Create the "Validate" button to confirm the input
        validateButton = new JButton("Validate");
        bottomPanel.add(validateButton, BorderLayout.EAST);
        validateButton.addActionListener(e -> dispose());

        backgroundLabel.add(bottomPanel, BorderLayout.SOUTH);

        panel.add(backgroundLabel, BorderLayout.CENTER);

        getContentPane().add(panel);
        setSize(backgroundImageIcon.getIconWidth(), backgroundImageIcon.getIconHeight());
        setLocationRelativeTo(parent);
        setResizable(false);

        // Bind the Enter key press to the "Validate" button
        ActionListener enterListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateButton.doClick();
            }
        };
        textField.addActionListener(enterListener);
    }

    /**
     * Displays the custom input dialog and returns the user's input.
     *
     * @return The user's input as a String.
     */
    public String showDialog() {
        setVisible(true);
        return textField.getText();
    }
}
