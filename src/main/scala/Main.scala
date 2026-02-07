import javax.swing._
import java.awt._
import java.awt.event.KeyEvent

object Main {
  var camera = Camera.default
  val inputHandler = new InputHandler()
  var lastTime = System.nanoTime()
  
  def main(args: Array[String]): Unit = {
    val w = 640
    val h = 480
    
    val frame = new JFrame("Descent Clone - WASD/Space/Shift to move, Arrows/Q/E to rotate")
    val panel = new JPanel() {
      override def paintComponent(g: Graphics): Unit = {
        val now = System.nanoTime()
        val dt = (now - lastTime) / 1e9f
        lastTime = now
        
        camera = inputHandler.updateCamera(camera, dt)
        
        val img = RayRenderer.render(w, h, camera)
        g.drawImage(img, 0, 0, null)
        
        // Continuous repaint
        SwingUtilities.invokeLater(() => repaint())
      }
      
      override def getPreferredSize(): Dimension = new Dimension(w, h)
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
