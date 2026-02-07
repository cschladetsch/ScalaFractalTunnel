import javax.swing._
import java.awt._

object Main {
  var camera = Camera.default
  val inputHandler = new InputHandler()
  var lastTime = System.nanoTime()
  var frameCount = 0
  var lastFpsTime = System.currentTimeMillis()
  var fps = 0
  
  def main(args: Array[String]): Unit = {
    val renderW = 213
    val renderH = 160
    val displayW = 640
    val displayH = 480
    
    AudioSystem.init()
    
    val frame = new JFrame("Fractal Tunnel - Don't hit the walls!")
    val panel = new JPanel() {
      override def paintComponent(g: Graphics): Unit = {
        val now = System.nanoTime()
        val dt = math.min((now - lastTime) / 1e9f, 0.1f)
        lastTime = now
        
        if (GameState.isAlive) {
          // Game loop
          camera = inputHandler.updateCamera(camera, dt)
          ProjectileSystem.update(dt)
          GameState.update(camera.position, dt)
          AudioSystem.updatePosition(camera.position)
          
          // Render
          val img = RayRenderer.render(renderW, renderH, camera)
          g.drawImage(img, 0, 0, displayW, displayH, null)
          
          // HUD
          g.setColor(new Color(0, 0, 0, 180))
          g.fillRect(0, 0, displayW, 80)
          
          // Health bar
          val healthBarWidth = 200
          val healthPercent = GameState.health / 100f
          g.setColor(if (healthPercent > 0.5f) Color.GREEN else if (healthPercent > 0.25f) Color.YELLOW else Color.RED)
          g.fillRect(10, 10, (healthBarWidth * healthPercent).toInt, 20)
          g.setColor(Color.WHITE)
          g.drawRect(10, 10, healthBarWidth, 20)
          
          g.setFont(new Font("Monospaced", Font.BOLD, 18))
          g.drawString(f"Health: ${GameState.health}%.1f", 10, 50)
          g.drawString(f"Distance: ${GameState.distance}%.0f m", 10, 70)
          
          // FPS
          g.setFont(new Font("Monospaced", Font.PLAIN, 14))
          g.drawString(s"FPS: $fps", displayW - 80, 20)
          
          // Warning when low health
          if (healthPercent < 0.3f) {
            g.setColor(new Color(255, 0, 0, (math.sin(System.currentTimeMillis() * 0.01) * 100 + 155).toInt))
            g.setFont(new Font("Monospaced", Font.BOLD, 24))
            g.drawString("LOW HEALTH!", displayW / 2 - 80, displayH - 30)
          }
          
        } else {
          // Game Over screen
          g.setColor(new Color(20, 0, 0))
          g.fillRect(0, 0, displayW, displayH)
          
          g.setColor(Color.RED)
          g.setFont(new Font("Monospaced", Font.BOLD, 48))
          g.drawString("GAME OVER", displayW / 2 - 150, 80)
          
          g.setColor(Color.WHITE)
          g.setFont(new Font("Monospaced", Font.BOLD, 32))
          g.drawString(f"Distance: ${GameState.distance}%.0f m", displayW / 2 - 120, 140)
          
          // High scores
          g.setFont(new Font("Monospaced", Font.BOLD, 24))
          g.drawString("HIGH SCORES", displayW / 2 - 90, 200)
          
          g.setFont(new Font("Monospaced", Font.PLAIN, 18))
          val scores = GameState.getHighScores()
          for ((timestamp, dist) <- scores.zipWithIndex.take(10)) {
            val rank = dist + 1
            val (ts, d) = timestamp
            val date = new java.text.SimpleDateFormat("MMM dd").format(new java.util.Date(ts.toLong))
            g.drawString(f"$rank%2d. $d%5d m  ($date)", displayW / 2 - 100, 230 + rank * 25)
          }
          
          g.setFont(new Font("Monospaced", Font.PLAIN, 16))
          g.drawString("Press R to restart", displayW / 2 - 90, displayH - 40)
          
          // Check for restart
          if (inputHandler.keys.contains(java.awt.event.KeyEvent.VK_R)) {
            GameState.reset()
            camera = Camera.default
          }
        }
        
        // FPS counter
        frameCount += 1
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 1000) {
          fps = frameCount
          frameCount = 0
          lastFpsTime = currentTime
        }
        
        SwingUtilities.invokeLater(() => repaint())
      }
      
      override def getPreferredSize(): Dimension = new Dimension(displayW, displayH)
    }
    
    panel.setFocusable(true)
    panel.addKeyListener(inputHandler)
    
    frame.add(panel)
    frame.pack()
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
      override def windowClosing(e: java.awt.event.WindowEvent): Unit = {
        AudioSystem.stop()
      }
    })
    frame.setVisible(true)
    
    panel.requestFocusInWindow()
  }
}
