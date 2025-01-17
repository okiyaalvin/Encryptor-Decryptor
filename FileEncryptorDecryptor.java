
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class FileEncryptorDecryptor extends JFrame implements ActionListener {
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 32;
    private static final String ENCRYPT = "Encrypt";
    private static final String DECRYPT = "Decrypt";
    private static final String CLEAR_LOGS = "Clear Logs";

    private JButton browseButton;
    private JButton encryptButton;
    private JButton decryptButton;
    private JButton clearLogsButton;
    private JTextArea logTextArea;
    private JFileChooser fileChooser;
    private File selectedFile;

    public FileEncryptorDecryptor() {
        setTitle("File Encryptor & Decryptor");
        setSize(500, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create a colorful panel for the buttons
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.setBackground(new Color(51, 153, 255)); // Light blue color
        browseButton = new JButton("Browse");
        browseButton.setBackground(new Color(255, 153, 51)); // Orange color
        encryptButton = new JButton("Encrypt");
        encryptButton.setBackground(new Color(0, 153, 0)); // Green color
        decryptButton = new JButton("Decrypt");
        decryptButton.setBackground(new Color(255, 51, 51)); // Red color
        clearLogsButton = new JButton("Clear Logs");
        clearLogsButton.setBackground(new Color(255, 255, 0)); // Yellow color

        browseButton.addActionListener(this);
        encryptButton.addActionListener(this);
        decryptButton.addActionListener(this);
        clearLogsButton.addActionListener(this);

        topPanel.add(browseButton);
        topPanel.add(encryptButton);
        topPanel.add(decryptButton);
        topPanel.add(clearLogsButton);

        add(topPanel, BorderLayout.NORTH);

        // Create a JTextArea for logging
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Add padding
        add(scrollPane, BorderLayout.CENTER);

        // Initialize file chooser with filters
        // Initialize file chooser with filter for text files
        fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Text files", "txt");
        fileChooser.setFileFilter(filter);

        // Set background color for the main panel
        getContentPane().setBackground(new Color(240, 240, 240)); // Light gray color
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == browseButton) {
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                log("Selected file: " + selectedFile.getAbsolutePath());
            }
        } else if (e.getSource() == encryptButton) {
            if (selectedFile != null) {
                try {
                    String password = JOptionPane.showInputDialog("Enter password for encryption:");
                    if (password != null && password.length() >= 8) {
                        encryptFile(selectedFile, password);
                        log("File encrypted successfully.");
                        deleteFile(selectedFile);  // Delete original file after encryption
                        log("Original file deleted.");
                    } else {
                        log("Password must be at least 8 characters long.");
                    }
                } catch (Exception ex) {
                    log("Error occurred while encrypting file: " + ex.getMessage());
                }
            } else {
                log("Please select a file to encrypt.");
            }
        } else if (e.getSource() == decryptButton) {
            if (selectedFile != null) {
                try {
                    String password = JOptionPane.showInputDialog("Enter password for decryption:");
                    if (password != null && password.length() >= 8) {
                        decryptFile(selectedFile, password);
                        log("File decrypted successfully.");
                        deleteFile(selectedFile);  // Delete encrypted file after decryption
                        log("Encrypted file deleted.");
                    } else {
                        log("Password must be at least 8 characters long.");
                    }
                } catch (Exception ex) {
                    log("Error occurred while decrypting file: " + ex.getMessage());
                }
            } else {
                log("Please select a file to decrypt.");
            }
        } else if (e.getSource() == clearLogsButton) {
            logTextArea.setText("");
        }
    }

    private void encryptFile(File file, String password) throws Exception {
        byte[] salt = generateSalt();
        SecretKey secretKey = generateKey(password.toCharArray(), salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(new byte[16])); // Use an IV of all zeros

        byte[] fileContent = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        byte[] encryptedContent = cipher.doFinal(fileContent);

        // Prepend the salt to the encrypted content
        byte[] finalContent = new byte[salt.length + encryptedContent.length];
        System.arraycopy(salt, 0, finalContent, 0, salt.length);
        System.arraycopy(encryptedContent, 0, finalContent, salt.length, encryptedContent.length);

        String encryptedFileName = file.getAbsolutePath() + ".encrypted";
        Files.write(Paths.get(encryptedFileName), finalContent);

        log("File encrypted and saved as: " + encryptedFileName);
    }

    private void decryptFile(File file, String password) throws Exception {
        byte[] fileContent = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        if (fileContent.length <= SALT_LENGTH) {
            log("Invalid encrypted file");
            return;
        }

        byte[] salt = Arrays.copyOfRange(fileContent, 0, SALT_LENGTH);
        byte[] encryptedContent = Arrays.copyOfRange(fileContent, SALT_LENGTH, fileContent.length);
        SecretKey secretKey = generateKey(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[16])); // Use an IV of all zeros

        byte[] decryptedContent = cipher.doFinal(encryptedContent);

        String originalFileName = file.getAbsolutePath().replace(".encrypted", "");
        Files.write(Paths.get(originalFileName), decryptedContent);

        log("File decrypted and saved as: " + originalFileName);
    }

    private void deleteFile(File file) {
        if (!file.delete()) {
            log("Failed to delete the file: " + file.getAbsolutePath());
        }
    }

    private void log(String message) {
        logTextArea.append(message + "\n");
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new java.security.SecureRandom().nextBytes(salt);
        return salt;
    }

    private SecretKey generateKey(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new FileEncryptorDecryptor().setVisible(true);
            }
        });
    }
}
