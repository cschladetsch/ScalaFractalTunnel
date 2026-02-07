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
          val normal = RayMarcher.getNormal(hit.point)
          val lightDir = Vec3(0.3f, -0.5f, -0.8f).normalized
          val diffuse = math.max(0f, normal.dot(lightDir))
          val intensity = 0.2f + diffuse * 0.8f
          
          new Color(
            (intensity * 255).toInt,
            (intensity * 200).toInt,
            (intensity * 180).toInt
          )
        
        case None =>
          Color.BLACK
      }
      
      img.setRGB(x, y, color.getRGB)
    }
    
    img
  }
}
