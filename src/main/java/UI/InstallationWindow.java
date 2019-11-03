package UI;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class InstallationWindow {
    private JButton chooseFolderButton;
    private JTextField registryKeyField;
    private JPanel installationPanel;
    private JLabel installationPathLabel;
    private JButton installButton;

    private String installationPath;

    public InstallationWindow() {
        chooseFolderButton.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("."));
            fileChooser.setDialogTitle("Выберите папку установки:");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);

            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                String installationPath = fileChooser.getSelectedFile().getPath();
                installationPathLabel.setText(installationPath);
                this.installationPath = installationPath;
            }
        });
        installButton.addActionListener(actionEvent -> {
            installProgram();
        });
    }

    private void installProgram() {
        HashMap<String, String> jarMap = new HashMap<>();
        jarMap.put("lab-one-ankushev-1.0-SNAPSHOT", "\\lab-one-ankushev-1.0-SNAPSHOT.jar");
        jarMap.put("forms_rt-7.0.3", "\\libs\\forms_rt-7.0.3.jar");
        jarMap.put("log4j-1.2.17", "\\libs\\log4j-1.2.17.jar");
        jarMap.put("slf4j-log4j12-1.7.26", "\\libs\\slf4j-log4j12-1.7.26.jar");
        jarMap.put("slf4j-api-1.7.26", "\\libs\\slf4j-api-1.7.26.jar");
        jarMap.put("jna-3.5.1", "\\libs\\jna-3.5.1.jar");
        jarMap.put("platform-3.5.1", "\\libs\\platform-3.5.1.jar");

        if (insertDigitalSignatureIntoRegistry()) {
            checkAllJarsExist(jarMap);

            File installationFolder = null;
            try {
                installationFolder = new File(installationPath);
            } catch (NullPointerException e) {
                JOptionPane.showMessageDialog(null, "Неправильный путь!", "Ошибка!", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
            if (!installationFolder.exists()) {
                JOptionPane.showMessageDialog(null, "Неправильный путь!", "Ошибка!", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }

            copyFiles(jarMap);
            createExecScript(jarMap);

            JOptionPane.showMessageDialog(null, "Установка завершена!");
        }
    }

    private boolean insertDigitalSignatureIntoRegistry() {
        String cypheredInfo = getCypheredInfo();
        if (cypheredInfo.isEmpty()) {
            System.exit(-1);
        }
        String text = registryKeyField.getText();

        if (!text.isEmpty()) {
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, "Software", text, cypheredInfo);
            return true;
        } else {
            JOptionPane.showMessageDialog(null, "Выберите название раздела реестра!", "Ошибка!", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private String getCypheredInfo() {
        String info = getInfo();

        MessageDigest mdHashFunction = null;
        try {
            mdHashFunction = MessageDigest.getInstance("MD2");
            String hashedInfo = new String(mdHashFunction.digest(info.getBytes()));

            Cipher cipher = Cipher.getInstance("RSA");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair pair = keyGen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] cypheredInfo = cipher.doFinal(hashedInfo.getBytes());

            writePublicKeyToFile(new String(publicKey.getEncoded()));
            return new String(cypheredInfo);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
            System.exit(1);
        } catch (BadPaddingException e) {
            JOptionPane.showMessageDialog(null, "Wrong passphrase!");
            System.exit(1);
        }
//        cipher.init(Cipher.DECRYPT_MODE, );
//        byte[] y = cipher.doFinal( x );
        return "";
    }

    private String getInfo() {
        String username = System.getProperty("user.name");
        String hostname = getHostname();
        String winDirectory = System.getenv("WINDIR");
        String system32 = winDirectory + "\\system32";
        int numberMouseOfButtons = MouseInfo.getNumberOfButtons();
        double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        long diskSpace = getDiskSpace();


        return new StringBuilder().append(username)
                .append(hostname)
                .append(winDirectory)
                .append(system32)
                .append(numberMouseOfButtons)
                .append(screenHeight)
                .append(diskSpace)
                .toString();
    }


    private void writePublicKeyToFile(String publicKey) {
        File publicKeyFile = new File(installationPath + "\\publicKey.txt");
        try {
            Files.write(publicKey, publicKeyFile, Charsets.UTF_8);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Ошибка при создании файла с открытым ключом!", "Ошибка!", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    private static long getDiskSpace() {
        File file = new File("/");
        return file.getTotalSpace();
    }

    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "";
        }
    }

/*
    private static String getConfirmedPassphrase() {
        String newPassphare = "";
        while (newPassphare.isEmpty()) {
            newPassphare = showInputDialog("Парольная фраза:");
            if (newPassphare == null) {
                return "";
            }
        }

        String confirmedPassphrase = "";
        while (!confirmedPassphrase.equals(newPassphare)) {
            confirmedPassphrase = showInputDialog("Подтвердите парольную фразу:");
            if (confirmedPassphrase == null) {
                return "";
            }
        }

        return newPassphare;
    }
*/

    private static String showInputDialog(String message) {
        JPasswordField passwordField = new JPasswordField();
        int okCxl = JOptionPane.showConfirmDialog(null, passwordField, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (okCxl == JOptionPane.OK_OPTION) {
            return new String(passwordField.getPassword());
        } else {
            return null;
        }
    }

    private void checkAllJarsExist(HashMap<String, String> jarMap) {
        String currentDirectory = System.getProperty("user.dir");
        jarMap.forEach((jarName, jarPath) -> {
            File jarFile = new File(currentDirectory + jarPath);
            if (!jarFile.exists()) {
                JOptionPane.showMessageDialog(null, String.format("Файл %s не существует!", jarName), "Ошибка!", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
        });
    }

    private void copyFiles(HashMap<String, String> jarMap) {
        File libsDir = new File(installationPath + "\\libs");
        if (!libsDir.exists()) {
            libsDir.mkdir();
        }

        jarMap.forEach((jarName, jarPath) -> {
            String currentDirectory = System.getProperty("user.dir");
            File to = new File(installationPath + jarPath);
            if (!to.exists()) {
                File from = new File(currentDirectory + jarPath);
                try {
                    Files.copy(from, to);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, String.format("Ошибка при копировании файла %s!", jarName), "Ошибка!", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void createExecScript(HashMap<String, String> jarMap) {
        List<String> jarPathsList = jarMap.values()
                .stream()
                .filter(jarPath -> !jarPath.equals("lab-one-ankushev-1.0-SNAPSHOT"))
                .map(jarPath -> installationPath + jarPath)
                .collect(Collectors.toList());

        String jarPaths = installationPath + "\\lab-one-ankushev-1.0-SNAPSHOT;" + String.join(";", jarPathsList);

        String scriptText = String.format("java -cp %s Backend.App", jarPaths);

        File scriptFile = new File(installationPath + "\\exec.bat");
        if (!scriptFile.exists()) {
            try {
                Files.write(scriptText, scriptFile, Charsets.UTF_8);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Ошибка при создании исполнимого файла exec.bat!", "Ошибка!", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
        }
    }

    public String getInstallationPath() {
        return installationPath;
    }

    public JPanel getInstallationPanel() {
        return installationPanel;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        installationPanel = new JPanel();
        installationPanel.setLayout(new GridLayoutManager(7, 5, new Insets(0, 0, 0, 0), -1, -1));
        chooseFolderButton = new JButton();
        chooseFolderButton.setText("Выбрать папку");
        installationPanel.add(chooseFolderButton, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        installationPanel.add(spacer1, new GridConstraints(6, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        installationPanel.add(spacer2, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Имя раздела реестра:");
        installationPanel.add(label1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        installationPanel.add(spacer3, new GridConstraints(1, 0, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        registryKeyField = new JTextField();
        installationPanel.add(registryKeyField, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer4 = new Spacer();
        installationPanel.add(spacer4, new GridConstraints(1, 4, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        installButton = new JButton();
        installButton.setText("Установить");
        installationPanel.add(installButton, new GridConstraints(5, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        installationPanel.add(spacer5, new GridConstraints(4, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        installationPanel.add(spacer6, new GridConstraints(2, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        installationPathLabel = new JLabel();
        installationPathLabel.setText("");
        installationPanel.add(installationPathLabel, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return installationPanel;
    }

}
