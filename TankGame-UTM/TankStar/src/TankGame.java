import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;


// MAIN
public class TankGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}

//  GAME FRAME
class GameFrame extends JFrame {
    private GamePanel gamePanel;

    public GameFrame() {
        setTitle("Tank Stars - UTM Project");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        gamePanel = new GamePanel();
        add(gamePanel);
    }

    public void restartGame() {
        // Clean up old panel
        gamePanel.cleanup();
        remove(gamePanel);

        // Create fresh game panel
        gamePanel = new GamePanel();
        add(gamePanel);
        revalidate();
        repaint();
        gamePanel.requestFocusInWindow();
    }
}

//  GAME PANEL
class GamePanel extends JPanel implements KeyListener, ActionListener {
    private Tank player1;
    private Tank player2;
    private Projectile projectile;
    private boolean isPlayer1Turn;
    private int timerCounter;
    private java.util.Timer turnTimer;
    private JLabel timerLabel;
    private int player1Power,player2Power; // Store power
    private int player1Angle, player2Angle; // Store angles separately for each player
    private int player1Fuel;
    private int player2Fuel;
    private Image backgroundImage;
    private int player1Lives = 2;
    private int player2Lives = 2;
    private boolean gameEnded = false;
    private javax.swing.Timer gameTimer;

    // For turret line (trajectory preview)
    private int previewLength = 100;

    public GamePanel() {
        // Setup panel
        setBackground(new Color(135, 206, 235));
        setLayout(null);

        // Initialize tanks
        player1 = new Tank(100, 490, Color.BLUE);
        player2 = new Tank(600, 490, Color.GRAY);

        // Initialize game state
        projectile = null;
        isPlayer1Turn = true;
        player1Fuel = 2;
        player2Fuel = 2;
        player1Power = 0;
        player2Power = 0;
        player1Angle = 45;  // Initial angles
        player2Angle = 180;

        // Load background image
        try {
            backgroundImage = ImageIO.read(new File("background_01.png"));
        } catch (IOException e) {
            backgroundImage = null;
            System.out.println("Background image not found, using default graphics");
        }

        // Setup timer label
        timerLabel = new JLabel("30", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        timerLabel.setForeground(Color.RED);
        timerLabel.setBounds(350, 10, 100, 30);
        add(timerLabel);

        // Start turn timer
        turnTimer = new java.util.Timer();
        startTurnTimer();

        // Setup key listener
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();

        // Game loop timer - using javax.swing.Timer
        gameTimer = new javax.swing.Timer(16, this); // Use instance variable
        gameTimer.start();
    }

    public void cleanup() {
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        if (turnTimer != null) {
            gameTimer.stop();
        }
        gameEnded = false;
        projectile = null;
        isPlayer1Turn = true;
        timerCounter = 30;

    }

    private void startTurnTimer() {
        timerCounter = 30;
        timerLabel.setText(String.valueOf(timerCounter));
        turnTimer.cancel();
        turnTimer = new java.util.Timer();

        turnTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                timerCounter--;
                SwingUtilities.invokeLater(() -> {
                    timerLabel.setText(String.valueOf(timerCounter));
                    if (timerCounter <= 0) {
                        switchTurn();
                    }
                });
            }
        }, 1000, 1000);
    }

    private void switchTurn() {
        isPlayer1Turn = !isPlayer1Turn;
        player1Fuel = 2;
        player2Fuel = 2;
        projectile = null;
        startTurnTimer();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ADD background
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            drawBackgroundHills(g2d);
            drawGround(g2d);
        }

        // Druw wall
        drawWall(g2d);

        // Draw tanks with turrets
        int currentAngle = isPlayer1Turn ? player1Angle : player2Angle;
        player1.draw(g2d, player1Angle);
        player2.draw(g2d, player2Angle);

        // Draw turret line
        if (!gameEnded && projectile == null) {
            drawTurretLine(g2d);
        }

        // Druw projectile
        if (projectile != null) {
            projectile.draw(g2d);
        }

        // Druw UI text (NOT ADDED)
        drawTEXT(g2d);
    }

    private void drawBackgroundHills(Graphics g) {
        g.setColor(new Color(85, 100, 47));
        for (int x = 0; x < getWidth(); x++) {
            int y = (int) (490 + 50 * Math.sin(x * 0.02));
            g.drawLine(x, y, x, getHeight());
        }
    }

    private void drawGround(Graphics g) {
        g.setColor(new Color(139, 69, 19));
        g.fillRect(0, 490, getWidth(), getHeight() - 350);

        // Add some texture to the ground
        g.setColor(new Color(160, 82, 45));
        for (int i = 0; i < getWidth(); i += 20) {
            g.drawLine(i, 490, i, getHeight());
        }
    }

    private void drawWall(Graphics g) {
        g.setColor(Color.BLACK);
        int wallX = getWidth() / 2 - 10;
        int wallY = 290;
        int wallWidth = 20;
        int wallHeight = 225;
        g.fillRect(wallX, wallY, wallWidth, wallHeight);

        // Add wall texture
        g.setColor(Color.DARK_GRAY);
        for (int i = wallY; i < wallY + wallHeight; i += 10) {
            g.drawLine(wallX, i, wallX + wallWidth, i);
        }
    }

    private void drawTurretLine(Graphics g) {
        Tank currentTank = isPlayer1Turn ? player1 : player2;
        int currentAngle = isPlayer1Turn ? player1Angle : player2Angle;
        int currentPower = isPlayer1Turn ? player1Power : player2Power;

        int turretLength = 35;
        int muzzleSize = 12;
        int startX = currentTank.getX() + 20;
        int startY = currentTank.getY() + 10 ;
// - turretLength - muzzleSize/2 + 5

        double actualPower = currentPower * 0.8;
        double radians;
        if (isPlayer1Turn) {
            radians = Math.toRadians(currentAngle);
        } else {
            radians = Math.toRadians(180 - currentAngle);
        }

        // Calculate end point for turret line
        int endX = startX + (int)(previewLength * Math.cos(radians) * (isPlayer1Turn ? 1 : -1));
        int endY = startY - (int)(previewLength * Math.sin(radians));

        // Draw turret line with gradient
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Create gradient based on power
        float powerRatio = currentPower / 40.0f;
        Color startColor = new Color(255, (int)(255 * (1 - powerRatio)), 0, 150);
        Color endColor = new Color(255, 0, 0, 50);

        GradientPaint gradient = new GradientPaint(startX, startY, startColor, endX, endY, endColor);
        g2d.setPaint(gradient);
        g2d.drawLine(startX, startY, endX, endY);

        // Reset stroke
        g2d.setStroke(new BasicStroke(1));
    }

    private void drawTEXT(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));

        String turnText = isPlayer1Turn ? "Player 1's Turn (Left)" : "Player 2's Turn (Right)";
        g.drawString(turnText, 10, 20);

        int currentPower = isPlayer1Turn ? player1Power : player2Power;
        int displayedPower = (int) ((currentPower / 40.0) * 100);
        g.drawString("Power: " + displayedPower + "%", 10, 40);

        int currentAngle = isPlayer1Turn ? player1Angle : player2Angle;
        g.drawString("Angle: " + currentAngle + "°", 10, 60);

        String fuelText = "Fuel: " + (isPlayer1Turn ? player1Fuel : player2Fuel);
        g.drawString(fuelText, 10, 80);

        // Draw lives
        g.drawString("P1 Lives: " + player1Lives, 10, 100);
        g.drawString("P2 Lives: " + player2Lives, getWidth() - 120, 100);

//        g.setFont(new Font("Arial", Font.PLAIN, 12));
//        g.drawString("P1: A/D=Move, W/S=Angle, Q/E=Power, SPACE=Fire", 10, getHeight() - 60);
//        g.drawString("P2: ←/→=Move, ↑/↓=Angle, L/M=Power, SPACE=Fire", 10, getHeight() - 40);
//        g.drawString("Turret line shows shooting direction", 10, getHeight() - 20);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (projectile != null) {
            projectile.move();

            // Wall collision
            int wallX = getWidth() / 2 - 10;
            int wallY = 265;
            int wallWidth = 20;
            int wallHeight = 225;

            if (projectile.hasHitWall(wallX, wallY, wallWidth, wallHeight)) {
                projectile = null;
                switchTurn();
                return;
            }

            // Tank collision
            if (projectile.hasHit(player1)) {
                player1Lives--;
                if (player1Lives <= 0) {
                    gameEnded = true;
                    showGameOver("Player 1 is hit! Player 2 wins!");
                } else {
                    JOptionPane.showMessageDialog(this, "Player 1 is hit! Lives remaining: " + player1Lives);
                    projectile = null;
                    switchTurn();
                    return;
                }
            } else if (projectile.hasHit(player2)) {
                player2Lives--;
                if (player2Lives <= 0) {
                    gameEnded = true;
                    showGameOver("Player 2 is hit! Player 1 wins!");
                } else {
                    JOptionPane.showMessageDialog(this, "Player 2 is hit! Lives remaining: " + player2Lives);
                    projectile = null;
                    switchTurn();
                    return;
                }
            }

            // Out of bounds
            if (projectile.isOutOfBounds(getWidth(), getHeight())) {
                projectile = null;
                switchTurn();
            }
        }
        repaint();
    }

    private void showGameOver(String message) {
        turnTimer.cancel();
        gameTimer.stop();

        int option = JOptionPane.showConfirmDialog(
                this,
                message + "\n\nDo you want to play again?",
                "Game Over",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {
//            System.exit(0);
            // Restart game
//            Container parent = getParent();
//            if (parent instanceof GameFrame) {
//              //  System.exit(0);
//            ((GameFrame) parent).restartGame();
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof GameFrame) {
                ((GameFrame) window).restartGame();
            }
        } else {
            System.exit(0);
        }
    }

    public void fireProjectile() {
        if (gameEnded) return;
        Tank shooter = isPlayer1Turn ? player1 : player2;
        int currentPower = isPlayer1Turn ? player1Power : player2Power;
        double actualPower = currentPower * 0.8;
        double radians;
        int currentAngle = isPlayer1Turn ? player1Angle : player2Angle;

        if (isPlayer1Turn) {
            radians = Math.toRadians(currentAngle);
        } else {
            radians = Math.toRadians(180 - currentAngle);
        }

        int speedX = (int) (actualPower * Math.cos(radians)) * (isPlayer1Turn ? 1 : -1);
        int speedY = (int) (-actualPower * Math.sin(radians));
        int turretLength = 35;
        int muzzleSize = 12;

        int muzzleX = shooter.getX() + 20;
        int muzzleY = shooter.getY() + 10 - turretLength;
        //- muzzleSize/2 + 5
        projectile = new Projectile(muzzleX, muzzleY, speedX, speedY);
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameEnded) return;

        int key = e.getKeyCode();

        if (isPlayer1Turn) {
            // Player 1 controls
            switch (key) {
                case KeyEvent.VK_A:
                    if (player1Fuel > 0) {
                        player1.move(-20);
                        player1Fuel--;
                    }
                    break;
                case KeyEvent.VK_D:
                    if (player1Fuel > 0) {
                        player1.move(20);
                        player1Fuel--;
                    }
                    break;
                case KeyEvent.VK_W:
                    if (player1Angle < 90) player1Angle++;
                    break;
                case KeyEvent.VK_S:
                    if (player1Angle > 0) player1Angle--;
                    break;
                case KeyEvent.VK_E:
                    if (player1Power < 40) player1Power += 5;
                    break;
                case KeyEvent.VK_Q:
                    if (player1Power > 0) player1Power -= 5;
                    break;
                case KeyEvent.VK_SPACE:
                    fireProjectile();
                    break;
            }
        } else {
            // Player 2 controls
            switch (key) {
                case KeyEvent.VK_LEFT:
                    if (player2Fuel > 0) {
                        player2.move(-20);
                        player2Fuel--;
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (player2Fuel > 0) {
                        player2.move(20);
                        player2Fuel--;
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (player2Angle > 90) player2Angle--;
                    break;
                case KeyEvent.VK_DOWN:
                    if (player2Angle < 180) player2Angle++;
                    break;
                case KeyEvent.VK_P:
                    if (player2Power < 40) player2Power += 5;
                    break;
                case KeyEvent.VK_L:
                    if (player2Power > 0) player2Power -= 5;
                    break;
                case KeyEvent.VK_SPACE:
                    fireProjectile();
                    break;
            }
        }
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}




// ==================== "TANK CLASS" ====================
class Tank {
    private int x, y;
    private final Color color;

    public Tank(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    public void draw(Graphics g, int angle) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Tank body
        g2d.setColor(color);
        g2d.fillRoundRect(x, y, 40, 20, 5, 5);

        // Tank turret
        int centerX = x + 20;
        int centerY = y + 10;

        // Save current transform
       AffineTransform oldTransform = g2d.getTransform();

        if (color == Color.BLUE) {
            g2d.rotate(Math.toRadians((90 - angle)), centerX, centerY);

        } else {
            g2d.rotate(Math.toRadians((90 - angle)), centerX, centerY);
        }


//        g2d.setColor(color.darker());
//        g2d.fillRect(centerX - 5, centerY - 20, 10, 25);
//        g2d.setColor(color.brighter());
//        g2d.fillRect(centerX - 3, centerY - 18, 6, 20);

        int turretLength = 35;  // Longer barrel
        int turretWidth = 8;
        int muzzleSize = 12;    // Square muzzle at end

// Draw main turret barrel
        g2d.setColor(color.darker());
        g2d.fillRect(centerX - turretWidth/2, centerY - turretLength, turretWidth, turretLength);

// Draw inner barrel
        g2d.setColor(color.brighter());
        g2d.fillRect(centerX - turretWidth/2 + 1, centerY - turretLength + 2, turretWidth - 2, turretLength - 4);

// Draw muzzle
//        g2d.setColor(color.darker().darker());
//        g2d.fillRect(centerX - muzzleSize/2, centerY - turretLength - muzzleSize/2, muzzleSize, muzzleSize);

        // Restore transform
        g2d.setTransform(oldTransform);

        // Tank tracks
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x - 5, y + 20, 50, 5);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x - 5, y + 20, 50, 5);

        // Tank wheels
        g2d.setColor(Color.BLACK);
        for (int i = 0; i < 3; i++) {
            g2d.fillOval(x + i * 15 - 2, y + 18, 8, 8);
        }
    }

    public void move(int dx) {
        x += dx;
        // Boundary checking
        if (x < 0) x = 0;
        if (x > 760) x = 760;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}

//  PROJECTILE CLASS
class Projectile {
    private int x, y;
    private int speedX, speedY;
    private int size = 10;
    private Color trailColor = Color.ORANGE;

    public Projectile(int x, int y, int speedX, int speedY) {
        this.x = x;
        this.y = y;
        this.speedX = speedX;
        this.speedY = speedY;
    }

    public void move() {
        x += speedX;
        y += speedY;
        speedY += 1; // Gravity effect
    }

    public boolean hasHitWall(int wallX, int wallY, int wallWidth, int wallHeight) {
        return x >= wallX && x <= wallX + wallWidth &&
                y >= wallY && y <= wallY + wallHeight;

    }

    public boolean hasHit(Tank tank) {
        int tankX = tank.getX();
        int tankY = tank.getY();
        return x >= tankX && x <= tankX + 40 &&
                y >= tankY && y <= tankY + 30;
    }

    public boolean isOutOfBounds(int width, int height) {
        return x < -10 || x > width + 10 || y > height + 10;
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw trail effect
        g2d.setColor(new Color(trailColor.getRed(), trailColor.getGreen(), trailColor.getBlue(), 100));
        g2d.fillOval(x - 2, y - 2, size + 4, size + 4);

        // Draw main projectile with gradient
        GradientPaint gradient = new GradientPaint(
                x, y, Color.RED,
                x + size, y + size, Color.YELLOW
        );
        g2d.setPaint(gradient);
        g2d.fillOval(x, y, size, size);

        // Draw highlight
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x + 2, y + 2, size / 3, size / 3);

        // Draw outline
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x, y, size, size);
    }
}