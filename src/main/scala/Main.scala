import javax.swing._
import java.awt._

object Main {
  var camera = Camera.default
  val inputHandler = new InputHandler()
  var lastTime = System.nanoTime()
  
  def main(args: Array[String]): Unit = {
    val renderW = 213
    val renderH = 160
    val displayW = 640
    val displayH = 480
    
    AudioSystem.init()
    
    val frame = new JFrame("Descent Clone - WASD/R/F move, Arrows/Q/E rotate, SPACE fire")
    val panel = new JPanel() {
      override def paintComponent(g: Graphics): Unit = {
        val now = System.nanoTime()
        val dt = (now - lastTime) / 1e9f
        lastTime = now
        
        camera = inputHandler.updateCamera(camera, dt)
        ProjectileSystem.update(dt)
        AudioSystem.updatePosition(camera.position)
        
        val img = RayRenderer.render(renderW, renderH, camera)
        g.drawImage(img, 0, 0, displayW, displayH, null)
        
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
