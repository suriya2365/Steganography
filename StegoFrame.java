import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class StegoFrame extends JFrame {

    private JTextField inputPathField, outputPathField, messageField, nField;
    private JTextArea resultArea;
    private JButton encodeButton, decodeButton;

    public StegoFrame() {
        setTitle("Image Steganography (Swing)");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Input Image:"));
        inputPathField = new JTextField(20);
        panel.add(inputPathField);

        panel.add(new JLabel("Output Image:"));
        outputPathField = new JTextField("encoded.png", 20);
        panel.add(outputPathField);

        panel.add(new JLabel("Message:"));
        messageField = new JTextField(20);
        panel.add(messageField);

        panel.add(new JLabel("n (1-8):"));
        nField = new JTextField("2", 5);
        panel.add(nField);

        encodeButton = new JButton("Encode");
        decodeButton = new JButton("Decode");
        panel.add(encodeButton);
        panel.add(decodeButton);

        resultArea = new JTextArea(10, 50);
        resultArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(resultArea);

        add(panel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // --- Button Actions ---
        encodeButton.addActionListener(e -> encodeAction());
        decodeButton.addActionListener(e -> decodeAction());
    }

    // --- Conversion Helpers ---
    public static String letterTo5Bit(char letter) {
        int position = Character.toUpperCase(letter) - 'A' + 1;
        return String.format("%5s", Integer.toBinaryString(position)).replace(' ', '0');
    }

    public static Character fiveBitToLetter(String binary) {
        int pos = Integer.parseInt(binary, 2);
        if (pos < 1 || pos > 26) return null;
        return (char) ('A' + pos - 1);
    }

    public static String messageToBinary(String message) {
        StringBuilder bin = new StringBuilder();
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                bin.append(letterTo5Bit(c));
            } else if (c == ' ') {
                bin.append("00000");
            }
        }
        bin.append("11111");
        return bin.toString();
    }

    public static int[] getChannels(BufferedImage img) {
        int width = img.getWidth(), height = img.getHeight();
        int[] channels = new int[width * height * 3];
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                channels[idx++] = (rgb >> 16) & 0xFF;
                channels[idx++] = (rgb >> 8) & 0xFF;
                channels[idx++] = rgb & 0xFF;
            }
        }
        return channels;
    }

    public static BufferedImage setChannels(BufferedImage original, int[] channels) {
        int width = original.getWidth(), height = original.getHeight();
        BufferedImage newImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int idx = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = channels[idx++];
                int g = channels[idx++];
                int b = channels[idx++];
                int rgb = (r << 16) | (g << 8) | b;
                newImg.setRGB(x, y, rgb);
            }
        }
        return newImg;
    }

    public static int modifyChannelForBit(int channel, char bit, int n) {
        int mask = (1 << n) - 1;
        int lastN = channel & mask;
        int target = bit - '0';
        int parity = lastN % 2;
        if (parity == target) return channel;
        if (channel > 0 && (channel & 1) != target) return channel ^ 1;
        else if (channel < 255) return channel + 1;
        else return channel - 1;
    }

    public static char extractBitFromChannel(int channel, int n) {
        int mask = (1 << n) - 1;
        return ((channel & mask) % 2 == 1) ? '1' : '0';
    }

    // --- Encode/Decode ---
    public static boolean encodeMessage(String inputPath, String message, int n, String outputPath) {
        try {
            BufferedImage img = ImageIO.read(new File(inputPath));
            String binary = messageToBinary(message);
            int[] channels = getChannels(img);

            if (binary.length() > channels.length) {
                return false;
            }
            for (int i = 0; i < binary.length(); i++) {
                channels[i] = modifyChannelForBit(channels[i], binary.charAt(i), n);
            }
            BufferedImage out = setChannels(img, channels);
            ImageIO.write(out, "png", new File(outputPath));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String decodeMessage(String inputPath, int n, int maxLength) {
        try {
            BufferedImage img = ImageIO.read(new File(inputPath));
            int[] channels = getChannels(img);
            StringBuilder binary = new StringBuilder();
            for (int i = 0; i < Math.min(channels.length, maxLength * 5); i++) {
                binary.append(extractBitFromChannel(channels[i], n));
            }
            StringBuilder decoded = new StringBuilder();
            for (int i = 0; i + 4 < binary.length(); i += 5) {
                String five = binary.substring(i, i + 5);
                if (five.equals("11111")) break;
                if (five.equals("00000")) decoded.append(" ");
                else {
                    Character letter = fiveBitToLetter(five);
                    if (letter != null) decoded.append(letter);
                }
            }
            return decoded.toString();
        } catch (IOException e) {
            return null;
        }
    }

    // --- Button Logic ---
    private void encodeAction() {
        String in = inputPathField.getText().trim();
        String out = outputPathField.getText().trim();
        String msg = messageField.getText().trim();
        int n = Integer.parseInt(nField.getText().trim());
        boolean success = encodeMessage(in, msg, n, out);
        if (success) resultArea.setText("Message encoded into " + out);
        else resultArea.setText("Encoding failed!");
    }

    private void decodeAction() {
        String in = inputPathField.getText().trim();
        int n = Integer.parseInt(nField.getText().trim());
        String decoded = decodeMessage(in, n, 1000);
        if (decoded != null) resultArea.setText("Decoded message: " + decoded);
        else resultArea.setText("Decoding failed!");
    }

    // --- Main ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StegoFrame frame = new StegoFrame();
            frame.setVisible(true);
        });
    }
}
