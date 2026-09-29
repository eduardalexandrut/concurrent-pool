package app.view;

import app.controller.Controller;
import app.model.entities.game.BallState;
import app.model.entities.game.Hole;
import app.model.Physics;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class View extends JFrame {
    private final Physics model;
    private final Controller controller;
    private final VisualiserPanel panel;


    public View(Physics model, Controller controller, int w, int h){
        this.model = model;
        this.controller = controller;
        setTitle("Poool");
        setSize(w,h + 25);
        setResizable(false);
        panel = new VisualiserPanel(w,h);
        getContentPane().add(panel);

        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent ev){
                System.exit(-1);
            }
            public void windowClosed(WindowEvent ev){
                System.exit(-1);
            }
        });

        // Add the KeyListener
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

    }

    public void render(){

    }

    public class VisualiserPanel extends JPanel {
        private int ox;
        private int oy;
        private int delta;

        public VisualiserPanel(int w, int h){
            setSize(w,h + 25);
            ox = w/2;
            oy = h/2;
            delta = Math.min(ox, oy);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // This handles the clearRect and background
            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF
            );

            // --- 1. DRAW THE CROSSHAIR (from your old paint method) ---
            g2.setColor(Color.LIGHT_GRAY);
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(ox, 0, ox, oy * 2);
            g2.drawLine(0, oy, ox * 2, oy);

            // Draw the scores
            g2.setFont(new Font("Arial", Font.BOLD, 48));
            g2.setColor(Color.BLUE); // Semi-transparent white

            // Position them like a scoreboard on the background
            String scoreText = "User: " + controller.getUserScore() + "  -  " + "NPC: " + controller.getNPCScore();

            // Center the scoreboard at the top
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(scoreText)) / 2;
            g2.drawString(scoreText, x, getHeight() * 2/3);


            // Draw balls
            for (BallState b : controller.getStateSnapshot()) {
                this.drawBall(b, g2, null);
            }

            // Draw User's ball
//            BallState userBall = controller.getUserBallState();
//            // Overdraw the user ball with its specific label
//            if (userBall != null) {
//                this.drawBall(userBall, g2, "H");
//            }
//
//            // Draw NPC's ball
//            BallState npcBall = controller.getNPCBallState();
//            if (npcBall != null) {
//                this.drawBall(npcBall, g2, "B");
//            }

            // Draw left and right holes
            Hole leftHole = controller.getLeftHole();
            Hole rightHole = controller.getRightHole();
            if (leftHole != null) {
                drawHole(leftHole, g2);
            }
            if (rightHole != null) {
                drawHole(rightHole, g2);
            }

            // Draw FPS counter
            g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
            g2.setColor(Color.GREEN);

            String fpsText = "FPS: " + controller.getCurrentFPS();
            g2.drawString(fpsText, getWidth() - 80, 20);

        }

        /**
         * Draws a ball with a white fill, black border, and an optional label.
         */
        private void drawBall(BallState ball, Graphics2D g2, String label) {
            int x = (int) ball.pos().x();
            int y = (int) ball.pos().y();
            int r = (int) ball.radius();

            // --- A. Draw the Fill (White) ---
            g2.setColor(Color.WHITE);
//            g2.fillOval(x - r, y - r, r * 2, r * 2);

            // --- B. Draw the Border (Black) ---
            g2.setColor(Color.BLACK);
            // Use a slightly thicker stroke for visibility
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(x - r, y - r, r * 2, r * 2);

            // --- C. Draw the Label (If provided) ---
            if (label != null && !label.isEmpty()) {
                g2.setColor(Color.BLACK); // Label color

                // Set a bold, readable font. Adjust size based on ball radius.
                int fontSize = (int) (r * 1.5); // Example scaling
                g2.setFont(new Font("Arial", Font.BOLD, fontSize));

                // Center the text perfectly
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(label);
                int textHeight = fm.getAscent(); // Height from baseline up

                // We center the text relative to the ball's center (x, y)
                int labelX = x - (textWidth / 2);
                // Descent is the part below the baseline (like in 'g' or 'y')
                // We add half the ascent to center the bulk of the letter
                int labelY = y + (textHeight / 2) - fm.getDescent() / 2;

                g2.drawString(label, labelX, labelY);
            }
        }

        private void drawHole(Hole hole, Graphics2D g) {
            int x = (int) hole.position().x();
            int y = (int) hole.position().y();
            int r = (int) hole.radius();

            g.setColor(Color.BLACK);
           // g.fillOval(x - r, y - r, r * 2, r * 2);

            g.setStroke(new BasicStroke(2));
            g.drawOval(x - r, y - r, r * 2, r * 2);
        }

    }
}
