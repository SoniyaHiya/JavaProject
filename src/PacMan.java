import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;
import java.io.*;

public class PacMan extends JPanel implements ActionListener, KeyListener {

    enum GameState { START, PLAYING, PAUSED, GAME_OVER }
    GameState gameState = GameState.START;

    int scaredTimer = 0;
    int score = 0;
    int lives = 3;
    int level = 1;
    int highScore = 0;
    boolean gameOver = false;

    class Block {
        int x, y, width, height;
        Image image, normalImage;
        int startX, startY;
        char direction = 'U';
        int velocityX = 0, velocityY = 0;
        boolean scared = false;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.normalImage = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char dir) {
            this.direction = dir;
            updateVelocity(); // শুধু velocity update করবে, move এখানে হবে না
        }

        void updateVelocity() {
            int speed = tileSize / 4 + level / 2;
            switch (direction) {
                case 'U': velocityX = 0; velocityY = -speed; break;
                case 'D': velocityX = 0; velocityY = speed; break;
                case 'L': velocityX = -speed; velocityY = 0; break;
                case 'R': velocityX = speed; velocityY = 0; break;
            }
        }

        void reset() {
            this.x = startX;
            this.y = startY;
            this.velocityX = 0;
            this.velocityY = 0;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 24;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage, blueGhostImage, orangeGhostImage, pinkGhostImage, redGhostImage, scaredGhostImage;
    private Image pacmanUpImage, pacmanDownImage, pacmanLeftImage, pacmanRightImage;
    private Image cherryImage, cherry2Image, powerFoodImage;

    private Block pacman;
    private Block cherry = null;

    private String[] tileMap = {
            "XXXXXXXXXXXXXXXXXXX",
            "X*       X       *X",
            "X XX XXX X XXX XX X",
            "X                 X",
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "OOOX X       X XOOO",
            "XXXX X XXrXX X XXXX",
            "O       bpo       O",
            "XXXX X XXXXX X XXXX",
            "OOOX X       X XOOO",
            "XXXX X XXXXX X XXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X  X     P     X  X",
            "XX X X XXXXX X X XX",
            "X    X   X   X    X",
            "X XXXXXX X XXXXXX X",
            "X*               *X",
            "XXXXXXXXXXXXXXXXXXX"
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'};
    Random random = new Random();

    public PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Load images from assets folder
        wallImage = new ImageIcon("assets/wall.png").getImage();
        blueGhostImage = new ImageIcon("assets/blueGhost.png").getImage();
        orangeGhostImage = new ImageIcon("assets/orangeGhost.png").getImage();
        pinkGhostImage = new ImageIcon("assets/pinkGhost.png").getImage();
        redGhostImage = new ImageIcon("assets/redGhost.png").getImage();
        scaredGhostImage = new ImageIcon("assets/scaredGhost.png").getImage();

        pacmanUpImage = new ImageIcon("assets/pacmanUp.png").getImage();
        pacmanDownImage = new ImageIcon("assets/pacmanDown.png").getImage();
        pacmanLeftImage = new ImageIcon("assets/pacmanLeft.png").getImage();
        pacmanRightImage = new ImageIcon("assets/pacmanRight.png").getImage();

        cherryImage = new ImageIcon("assets/cherry.png").getImage();
        cherry2Image = new ImageIcon("assets/cherry2.png").getImage();
        powerFoodImage = new ImageIcon("assets/powerFood.png").getImage();

        loadHighScore();
        loadMap();

        for (Block ghost : ghosts) {
            char dir = directions[random.nextInt(4)];
            ghost.updateDirection(dir);
        }

        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    public void loadMap() {
        walls = new HashSet<>();
        foods = new HashSet<>();
        ghosts = new HashSet<>();
        pacman = null;

        for (int r = 0; r < rowCount; r++) {
            String row = tileMap[r];
            for (int c = 0; c < columnCount; c++) {
                char ch = row.charAt(c);
                int x = c * tileSize;
                int y = r * tileSize;

                if (ch == 'X') walls.add(new Block(wallImage, x, y, tileSize, tileSize));
                else if (ch == 'b') ghosts.add(new Block(blueGhostImage, x, y, tileSize, tileSize));
                else if (ch == 'o') ghosts.add(new Block(orangeGhostImage, x, y, tileSize, tileSize));
                else if (ch == 'p') ghosts.add(new Block(pinkGhostImage, x, y, tileSize, tileSize));
                else if (ch == 'r') ghosts.add(new Block(redGhostImage, x, y, tileSize, tileSize));
                else if (ch == 'P') pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                else if (ch == ' ') foods.add(new Block(null, x + tileSize / 2 - 2, y + tileSize / 2 - 2, 4, 4));
                else if (ch == '*') foods.add(new Block(powerFoodImage, x + tileSize / 2 - 4, y + tileSize / 2 - 4, 8, 8));
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // Game state screens
        if (gameState == GameState.START) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("PAC-MAN", boardWidth / 2 - 70, boardHeight / 2 - 50);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Press S to Start", boardWidth / 2 - 70, boardHeight / 2);
            return;
        }

        if (gameState == GameState.PAUSED) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("PAUSED", boardWidth / 2 - 60, boardHeight / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Press P to Resume", boardWidth / 2 - 70, boardHeight / 2 + 30);
            return;
        }

        if (gameState == GameState.GAME_OVER) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("Game Over", boardWidth / 2 - 70, boardHeight / 2 - 50);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Score: " + score, boardWidth / 2 - 40, boardHeight / 2 - 10);
            g.drawString("High: " + highScore, boardWidth / 2 - 40, boardHeight / 2 + 10);
            g.drawString("Press R to Restart", boardWidth / 2 - 70, boardHeight / 2 + 40);
            return;
        }

        // Draw food
        for (Block food : foods) {
            if (food.image != null) g.drawImage(food.image, food.x, food.y, food.width, food.height, null);
            else {
                g.setColor(Color.WHITE);
                g.fillRect(food.x, food.y, food.width, food.height);
            }
        }

        if (cherry != null) g.drawImage(cherry.image, cherry.x, cherry.y, cherry.width, cherry.height, null);
        for (Block wall : walls) g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        for (Block ghost : ghosts) g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        // Draw score/lives
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        g.drawString("Lives: " + lives, 5, 16);
        g.drawString("Score: " + score, 5, 36);
        g.drawString("High Score: " + highScore, boardWidth - 130, 16);
        g.drawString("Level: " + level, boardWidth - 80, 36);
    }

    public void move() {
        // Pacman move
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        // Screen wrap
        if (pacman.x < 0) pacman.x = boardWidth - tileSize;
        if (pacman.x + tileSize > boardWidth) pacman.x = 0;

        // Wall collision
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
            }
        }

        // Ghost move & collision
        for (Block ghost : ghosts) {
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;

            for (Block wall : walls) {
                if (collision(ghost, wall) || ghost.x <= 0 || ghost.x + ghost.width >= boardWidth) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    ghost.updateDirection(directions[random.nextInt(4)]);
                }
            }

            if (collision(ghost, pacman)) {
                if (ghost.scared) {
                    score += 200;
                    ghost.reset();
                    ghost.image = ghost.normalImage;
                    ghost.scared = false;
                } else {
                    lives--;
                    if (lives == 0) {
                        gameOver = true;
                        return;
                    }
                    resetPositions();
                }
            }
        }

        // Food collision
        Block eaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                eaten = food;
                if (food.width == 4) score += 10;
                else if (food.width == 8) {
                    score += 50;
                    scareGhosts();
                }
                break;
            }
        }
        foods.remove(eaten);

        // Cherry spawn & eat
        if (score > 0 && score % 200 == 0 && cherry == null) {
            int x = random.nextInt(columnCount) * tileSize;
            int y = random.nextInt(rowCount) * tileSize;
            cherry = new Block(cherryImage, x, y, tileSize, tileSize);
        }

        if (cherry != null && collision(pacman, cherry)) {
            score += 100;
            cherry = null;
        }

        // Level up
        if (foods.isEmpty()) {
            level++;
            increaseDifficulty();
            loadMap();
            resetPositions();
        }
    }

    public void scareGhosts() {
        scaredTimer = 60;
        for (Block ghost : ghosts) {
            ghost.scared = true;
            ghost.image = scaredGhostImage;
        }
    }

    public void resetPositions() {
        pacman.reset();
        for (Block ghost : ghosts) {
            ghost.reset();
            ghost.updateDirection(directions[random.nextInt(4)]);
        }
    }

    public boolean collision(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x &&
               a.y < b.y + b.height && a.y + a.height > b.y;
    }

    public void increaseDifficulty() {
        for (Block ghost : ghosts) {
            if (ghost.velocityX > 0) ghost.velocityX += 1;
            else if (ghost.velocityX < 0) ghost.velocityX -= 1;
            if (ghost.velocityY > 0) ghost.velocityY += 1;
            else if (ghost.velocityY < 0) ghost.velocityY -= 1;
        }
    }

    public void loadHighScore() {
        try {
            File file = new File("highscore.txt");
            if (file.exists()) {
                java.util.Scanner sc = new java.util.Scanner(file);
                if (sc.hasNextInt()) highScore = sc.nextInt();
                sc.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void saveHighScore() {
        try {
            PrintWriter writer = new PrintWriter("highscore.txt");
            writer.println(highScore);
            writer.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState != GameState.PLAYING) return;
        move();
        repaint();

        if (scaredTimer > 0) {
            scaredTimer--;
            if (scaredTimer == 0) {
                for (Block ghost : ghosts) {
                    ghost.scared = false;
                    if (ghost.image == scaredGhostImage) ghost.image = ghost.normalImage;
                }
            }
        }

        if (gameOver) {
            if (score > highScore) {
                highScore = score;
                saveHighScore();
            }
            gameState = GameState.GAME_OVER;
            gameLoop.stop();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}
    @Override
    public void keyPressed(KeyEvent e) {}
    
    @Override
    public void keyReleased(KeyEvent e) {
        if (gameState == GameState.START && e.getKeyCode() == KeyEvent.VK_S) {
            gameState = GameState.PLAYING;
            gameLoop.start();
            repaint();
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_R) {
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            level = 1;
            gameOver = false;
            gameState = GameState.PLAYING;
            gameLoop.start();
            repaint();
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_P) {
            gameState = (gameState == GameState.PLAYING) ? GameState.PAUSED : GameState.PLAYING;
            repaint();
            return;
        }

        if (gameState != GameState.PLAYING) return;

        if (e.getKeyCode() == KeyEvent.VK_UP) pacman.updateDirection('U');
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) pacman.updateDirection('D');
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) pacman.updateDirection('L');
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) pacman.updateDirection('R');

        switch (pacman.direction) {
            case 'U': pacman.image = pacmanUpImage; break;
            case 'D': pacman.image = pacmanDownImage; break;
            case 'L': pacman.image = pacmanLeftImage; break;
            case 'R': pacman.image = pacmanRightImage; break;
        }
    }
}