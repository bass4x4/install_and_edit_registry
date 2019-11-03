package Backend;

import UI.InstallationWindow;

import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        JFrame installationWindow = new JFrame("Установка приложения.");
        installationWindow.setContentPane(new InstallationWindow().getInstallationPanel());
        installationWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        installationWindow.pack();
        installationWindow.setSize(500, 300);
        installationWindow.setMinimumSize(new Dimension(200, 200));

        installationWindow.setVisible(true);
    }
}
