import javax.swing._
import java.awt._

object Main {
  def main(args: Array[String]): Unit = {
    val w = 640
    val h = 480
    
    val frame = new JFrame("Descent Clone")
    val panel = new JPanel() {
      override def paintComponent(g: Graphics): Unit = {
        val img = RayRenderer.render(w, h)
        g.drawImage(img, 0, 0, null)
      }
      
      override def getPreferredSize(): Dimension = new Dimension(w, h)
    }
    
    frame.add(panel)
    frame.pack()
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.setVisible(true)
  }
}
