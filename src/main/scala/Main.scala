import javax.swing._
import java.awt._
import java.awt.event.KeyEvent

object Main {
  var camera = Camera.default
  val inputHandler = new InputHandler()
  var lastTime = System.nanoTime()
  
  def main(args: Array[String]): Unit = {
    val renderW = 320
    val renderH = 240
    val displayW = 640
    val displayH = 480
    
    val frame = new JFrame("Descent Clone - WASD/Space/Shift to move, Arrows/Q/E to rotate")
    val panel = new JPanel() {
      override def paintComponent(g: Graphics): Unit = {
        val now = System.nanoTime()
        val dt = (now - lastTime) / 1e9f
        lastTime = now
        
        camera = inputHandler.updateCamera(camera, dt)
        
        val img = RayRenderer.render(renderW, renderH, camera)
        
        // Scale 2x for display - nearest neighbor for chunky pixels
        g.drawImage(img, 0, 0, displayW, displayH, null)
        
        // Continuous repaint
        SwingUtilities.invokeLater(() => repaint())
      }
      
      override def getPreferredSize(): Dimension = new Dimension(displayW, displayH)
    }
    
    panel.setFocusable(true)
    panel.addKeyListener(inputHandler)
    
    frame.add(panel)
    frame.pack()
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.setVisible(true)
    
    panel.requestFocusInWindow()
  }
}
