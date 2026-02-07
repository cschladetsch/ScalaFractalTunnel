import javax.swing._
import java.awt._

object Main {
  var camera = Camera.default
  val inputHandler = new InputHandler()
  var lastTime = System.nanoTime()
  var frameCount = 0
  var lastFpsTime = System.currentTimeMillis()
  var fps = 0
  var messageTime = 0L
  var message = ""
  
  def main(args: Array[String]): Unit = {
    AudioSystem.init()
    GameState.reset()
    
    val frame = new JFrame("Fractal Tunnel - ULTIMATE")
    val panel = new JPanel() {
      override def paintComponent(g: Graphics): Unit = {
        val now = System.nanoTime()
        val dt = math.min((now - lastTime) / 1e9f, 0.1f)
        lastTime = now
        
        if (GameState.isAlive) {
          if (!GameState.gameStarted) {
            g.setColor(Color.BLACK)
            g.fillRect(0, 0, Config.displayWidth, Config.displayHeight)
            g.setColor(Color.WHITE)
            g.setFont(new Font("Monospaced", Font.BOLD, 72))
            val countText = if (GameState.countdownTime > 0) GameState.countdownTime.toInt.toString else "GO!"
            g.drawString(countText, Config.displayWidth / 2 - 50, Config.displayHeight / 2)
            
            GameState.update(camera.position, dt, camera)
            SwingUtilities.invokeLater(() => repaint())
            return
          }
          
          val (newCamera, checkpointReached, checkpointMessage) = GameState.update(camera.position, dt, camera)
          camera = inputHandler.updateCamera(newCamera, dt)
          
          if (checkpointReached) {
            messageTime = System.currentTimeMillis()
            message = checkpointMessage
          }
          
          AudioSystem.updatePosition(camera.position)
          
          val img = RayRenderer.render(Config.renderWidth, Config.renderHeight, camera)
          g.drawImage(img, 0, 0, Config.displayWidth, Config.displayHeight, null)
          
          // Speed lines
          if (GameState.currentSpeed > 8f) {
            val speedIntensity = ((GameState.currentSpeed - 8f) / 10f * 255).toInt min 200
            g.setColor(new Color(255, 255, 255, speedIntensity))
            val rng = new scala.util.Random((System.currentTimeMillis() / 50).toInt)
            for (_ <- 0 until 30) {
              val x1 = rng.nextInt(Config.displayWidth)
              val y1 = rng.nextInt(Config.displayHeight)
              g.drawLine(Config.displayWidth/2, Config.displayHeight/2, x1, y1)
            }
          }
          
          // Particles
          for (p <- ParticleSystem.getAll) {
            val screenPos = projectToScreen(p.position, camera, Config.displayWidth, Config.displayHeight)
            if (screenPos._3 > 0) {
              val alpha = (p.lifetime / p.maxLifetime * 255).toInt
              g.setColor(new Color(p.color._1, p.color._2, p.color._3, alpha))
              g.fillOval(screenPos._1 - 2, screenPos._2 - 2, 4, 4)
            }
          }
          
          // Low health vignette
          if (GameState.health < 30f) {
            val vignetteAlpha = ((30f - GameState.health) / 30f * 150).toInt
            g.setColor(new Color(255, 0, 0, vignetteAlpha))
            for (i <- 0 until 50) {
              g.drawRect(i * 2, i * 2, Config.displayWidth - i * 4, Config.displayHeight - i * 4)
            }
          }
          
          // Shield effect
          if (GameState.shieldActive) {
            g.setColor(new Color(0, 150, 255, 100))
            g.fillRect(0, 0, Config.displayWidth, 5)
            g.fillRect(0, Config.displayHeight - 5, Config.displayWidth, 5)
            g.fillRect(0, 0, 5, Config.displayHeight)
            g.fillRect(Config.displayWidth - 5, 0, 5, Config.displayHeight)
          }
          
          // Slow time effect
          if (GameState.slowTimeActive) {
            g.setColor(new Color(255, 255, 0, 50))
            g.fillRect(0, 0, Config.displayWidth, Config.displayHeight)
          }
          
          // HUD
          g.setColor(new Color(0, 0, 0, 180))
          g.fillRect(0, 0, Config.displayWidth, 140)
          
          val healthPercent = GameState.health / Config.startHealth
          g.setColor(if (healthPercent > 0.5f) Color.GREEN else if (healthPercent > 0.25f) Color.YELLOW else Color.RED)
          g.fillRect(10, 10, (200 * healthPercent).toInt, 20)
          g.setColor(Color.WHITE)
          g.drawRect(10, 10, 200, 20)
          
          g.setFont(new Font("Monospaced", Font.BOLD, 16))
          g.drawString(f"Health: ${GameState.health}%.0f", 10, 50)
          g.drawString(f"Distance: ${GameState.distance * GameState.comboMultiplier}%.0f m", 10, 70)
          g.drawString(f"Speed: ${GameState.currentSpeed}%.1f", 10, 90)
          g.drawString(f"Multiplier: ${GameState.comboMultiplier}%.1fx", 10, 110)
          g.drawString(f"Rank: #${GameState.getCurrentRank()}", 10, 130)
          
          g.setFont(new Font("Monospaced", Font.PLAIN, 14))
          g.drawString(f"Next CP: ${GameState.getDistanceToNextCheckpoint()}m", Config.displayWidth - 150, 20)
          g.drawString(f"Best: ${GameState.bestDistance}%.0f", Config.displayWidth - 150, 40)
          g.drawString(s"FPS: $fps", Config.displayWidth - 80, 60)
          
          val timeSinceMessage = System.currentTimeMillis() - messageTime
          if (timeSinceMessage < 2000 && message.nonEmpty) {
            val alpha = if (timeSinceMessage < 1000) 255 else (255 * (2000 - timeSinceMessage) / 1000).toInt
            g.setColor(new Color(0, 255, 0, alpha))
            g.setFont(new Font("Monospaced", Font.BOLD, 24))
            g.drawString(message, Config.displayWidth / 2 - message.length * 6, Config.displayHeight / 2)
          }
          
        } else {
          g.setColor(new Color(20, 0, 0))
          g.fillRect(0, 0, Config.displayWidth, Config.displayHeight)
          
          g.setColor(Color.RED)
          g.setFont(new Font("Monospaced", Font.BOLD, 48))
          g.drawString("GAME OVER", Config.displayWidth / 2 - 150, 80)
          
          if (GameState.newRecord) {
            g.setColor(Color.YELLOW)
            g.setFont(new Font("Monospaced", Font.BOLD, 32))
            g.drawString("NEW RECORD!", Config.displayWidth / 2 - 120, 120)
          }
          
          g.setColor(Color.WHITE)
          g.setFont(new Font("Monospaced", Font.BOLD, 28))
          g.drawString(f"Score: ${GameState.distance * GameState.comboMultiplier}%.0f m", Config.displayWidth / 2 - 100, 160)
          
          g.setFont(new Font("Monospaced", Font.BOLD, 20))
          g.drawString("HIGH SCORES", Config.displayWidth / 2 - 80, 200)
          
          g.setFont(new Font("Monospaced", Font.PLAIN, 16))
          val scores = GameState.getHighScores()
          for ((timestamp, dist) <- scores.zipWithIndex.take(10)) {
            val rank = dist + 1
            val (ts, d) = timestamp
            val date = new java.text.SimpleDateFormat("MMM dd").format(new java.util.Date(ts.toLong))
            g.drawString(f"$rank%2d. $d%5d m  ($date)", Config.displayWidth / 2 - 90, 220 + rank * 20)
          }
          
          g.setFont(new Font("Monospaced", Font.BOLD, 18))
          g.drawString("Press R to restart", Config.displayWidth / 2 - 100, Config.displayHeight - 30)
          
          if (inputHandler.keys.contains(java.awt.event.KeyEvent.VK_R)) {
            GameState.reset()
            camera = Camera.default
            messageTime = 0L
            message = ""
          }
        }
        
        frameCount += 1
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 1000) {
          fps = frameCount
          frameCount = 0
          lastFpsTime = currentTime
        }
        
        SwingUtilities.invokeLater(() => repaint())
      }
      
      def projectToScreen(worldPos: Vec3, cam: Camera, w: Int, h: Int): (Int, Int, Float) = {
        val rel = worldPos - cam.position
        val depth = rel.dot(cam.forward)
        if (depth < 0.1f) return (0, 0, -1f)
        
        val right = rel.dot(cam.right)
        val up = rel.dot(cam.up)
        
        val screenX = (w / 2 + (right / depth) * w).toInt
        val screenY = (h / 2 - (up / depth) * h).toInt
        
        (screenX, screenY, depth)
      }
      
      override def getPreferredSize(): Dimension = new Dimension(Config.displayWidth, Config.displayHeight)
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
