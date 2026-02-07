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
          val phase = hit.point.z * 0.2f
          
          hit.materialId match {
            case RayMarcher.MAT_TUNNEL =>
              // Rainbow tunnel
              val r = ((math.sin(phase).toFloat * 0.5f + 0.5f) * 255).toInt
              val g = ((math.sin(phase + 2).toFloat * 0.5f + 0.5f) * 255).toInt
              val b = ((math.sin(phase + 4).toFloat * 0.5f + 0.5f) * 255).toInt
              new Color(r, g, b)
              
            case RayMarcher.MAT_PICKUP =>
              // Pulsing green health pickups
              val t = System.currentTimeMillis() * 0.003f
              val pulse = (math.sin(t).toFloat * 0.3f + 1f)
              val brightness = (200 * pulse).toInt min 255
              new Color(50, brightness, 50)
              
            case RayMarcher.MAT_PROJECTILE =>
              new Color(255, 255, 100)
              
            case _ =>
              new Color(180, 180, 180)
          }
        
        case None =>
          Color.BLACK
      }
      
      img.setRGB(x, y, color.getRGB)
    }
    
    img
  }
}
