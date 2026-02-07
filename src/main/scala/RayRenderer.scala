import java.awt.image.BufferedImage
import java.awt.Color

object RayRenderer {
  def render(width: Int, height: Int, camera: Camera): BufferedImage = {
    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val fov = 90f
    
    for {
      y <- 0 until height
      x <- 0 until width
    } {
      val rayDir = camera.getRayDirection(x, y, width, height, fov)
      
      val color = RayMarcher.march(camera.position, rayDir) match {
        case Some(hit) =>
          // Bright rainbow colors
          val phase = hit.point.z * 0.2f
          val r = ((math.sin(phase).toFloat * 0.5f + 0.5f) * 255).toInt
          val g = ((math.sin(phase + 2).toFloat * 0.5f + 0.5f) * 255).toInt
          val b = ((math.sin(phase + 4).toFloat * 0.5f + 0.5f) * 255).toInt
          
          new Color(r, g, b)
        
        case None =>
          Color.BLACK
      }
      
      img.setRGB(x, y, color.getRGB)
    }
    
    img
  }
}
