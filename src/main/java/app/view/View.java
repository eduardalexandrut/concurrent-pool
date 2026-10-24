package app.view;

import app.controller.Controller;
import app.model.entities.game.BallState;
import app.model.entities.game.Hole;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class View extends JFrame {
    private final double LOGICAL_WIDTH;
    private final double LOGICAL_HEIGHT;

    private final Controller controller;
    private final VisualiserPanel panel;
    private final Timer renderTimer;

    public View(Controller controller, int w, int h) {
        this.LOGICAL_WIDTH = w;
        this.LOGICAL_HEIGHT = h;

        this.controller = controller;
        setTitle("Poool");
        setSize(w, h + 25);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        panel = new VisualiserPanel();
        getContentPane().add(panel);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                System.exit(-1);
            }
            public void windowClosed(WindowEvent ev) {
                System.exit(-1);
            }
        });

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                String key = KeyEvent.getKeyText(e.getKeyCode()).toUpperCase();
                if ("WASD".contains(key)) {
                    controller.processInput(key);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                String key = KeyEvent.getKeyText(e.getKeyCode()).toUpperCase();
                if ("WASD".contains(key)) {
                    controller.processInput("STOP_ " + key);
                }
            }
        });

        this.setFocusable(true);
        this.requestFocusInWindow();

        this.renderTimer = new Timer(16, e -> {
            panel.repaint();
        });
        this.renderTimer.start();
    }

    public class VisualiserPanel extends JPanel {

        public VisualiserPanel() {
            setBackground(Color.DARK_GRAY);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            double targetWidth = getWidth() * 0.95;
            double targetHeight = getHeight() * 0.95;

            double scaleX = targetWidth / LOGICAL_WIDTH;
            double scaleY = targetHeight / LOGICAL_HEIGHT;
            double scale = Math.min(scaleX, scaleY);

            double offsetX = (getWidth() - (LOGICAL_WIDTH * scale)) / 2.0;
            double offsetY = (getHeight() - (LOGICAL_HEIGHT * scale)) / 2.0;

            java.awt.geom.AffineTransform originalTransform = g2.getTransform();

            g2.translate(offsetX, offsetY);
            g2.scale(scale, scale);

            g2.setColor(new Color(240, 240, 240));
            g2.fillRect(0, 0, (int) LOGICAL_WIDTH, (int) LOGICAL_HEIGHT);

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(0, 0, (int) LOGICAL_WIDTH, (int) LOGICAL_HEIGHT);

            g2.setColor(Color.LIGHT_GRAY);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine((int) (LOGICAL_WIDTH / 2), 0, (int) (LOGICAL_WIDTH / 2), (int) LOGICAL_HEIGHT);
            g2.drawLine(0, (int) (LOGICAL_HEIGHT / 2), (int) LOGICAL_WIDTH, (int) (LOGICAL_HEIGHT / 2));

            Hole leftHole = controller.getLeftHole();
            Hole rightHole = controller.getRightHole();
            if (leftHole != null) drawHole(leftHole, g2);
            if (rightHole != null) drawHole(rightHole, g2);

            for (BallState b : controller.getStateSnapshot()) {
                this.drawBall(b, g2, null);
            }

            BallState userBall = controller.getUserBallState();
            if (userBall != null) {
                this.drawBall(userBall, g2, "H");
            }

            BallState npcBall = controller.getNPCBallState();
            if (npcBall != null) {
                this.drawBall(npcBall, g2, "B");
            }

            g2.setTransform(originalTransform);

            g2.setFont(new Font("Arial", Font.BOLD, 36));
            g2.setColor(Color.BLUE);
            String scoreText = "User: " + controller.getUserScore() + "  -  NPC: " + controller.getNPCScore();
            FontMetrics fmUI = g2.getFontMetrics();
            int scoreX = (getWidth() - fmUI.stringWidth(scoreText)) / 2;
            g2.drawString(scoreText, scoreX, 45);

            g2.setFont(new Font("Monospaced", Font.BOLD, 18));
            g2.setColor(Color.GREEN);
            String fpsText = "FPS: 60";
            g2.drawString(fpsText, getWidth() - 120, 35);

            String gameState = controller.getGameState();
            if (!"RUNNING".equals(gameState)) {
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRect(0, 0, getWidth(), getHeight());

                String endText;
                Color textColor;
                switch (gameState) {
                    case "USER_WON":
                        endText = "YOU WON!";
                        textColor = Color.GREEN;
                        break;
                    case "NPC_WON":
                        endText = "NPC WON!";
                        textColor = Color.RED;
                        break;
                    default:
                        endText = "DRAW";
                        textColor = Color.YELLOW;
                        break;
                }

                g2.setFont(new Font("Arial", Font.BOLD, 100));
                g2.setColor(textColor);
                FontMetrics endFm = g2.getFontMetrics();
                int tx = (getWidth() - endFm.stringWidth(endText)) / 2;
                int ty = (getHeight() - endFm.getHeight()) / 2 + endFm.getAscent();

                g2.drawString(endText, tx, ty);
            }
        }

        private void drawBall(BallState ball, Graphics2D g2, String label) {
            int x = (int) ball.pos().x();
            int y = (int) ball.pos().y();
            int r = (int) ball.radius();

            if (label == null || label.isEmpty()) {
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(x - r, y - r, r * 2, r * 2);
                return;
            }

            g2.setColor(Color.WHITE);
            g2.fillOval(x - r, y - r, r * 2, r * 2);

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(x - r, y - r, r * 2, r * 2);

            g2.setColor(Color.BLACK);
            int fontSize = (int) (r * 1.5);
            g2.setFont(new Font("Arial", Font.BOLD, fontSize));

            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(label);
            int textHeight = fm.getAscent();

            int labelX = x - (textWidth / 2);
            int labelY = y + (textHeight / 2) - fm.getDescent() / 2;

            g2.drawString(label, labelX, labelY);
        }

        private void drawHole(Hole hole, Graphics2D g) {
            int x = (int) hole.position().x();
            int y = (int) hole.position().y();
            int r = (int) hole.radius();

            g.setColor(Color.BLACK);
            g.fillOval(x - r, y - r, r * 2, r * 2);

            g.setStroke(new BasicStroke(2));
            g.drawOval(x - r, y - r, r * 2, r * 2);
        }
    }
}