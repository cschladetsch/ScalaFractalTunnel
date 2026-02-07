import java.awt.image.BufferedImage
import java.awt.Color

object RayRenderer {
  def render(width: Int, height: Int): BufferedImage = {
    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val fov = 90f
    val aspect = width.toFloat / height
    val fovRad = math.toRadians(fov).toFloat
    
    val camPos = Vec3(0, 0, 0)
    
    for {
      y <- 0 until height
      x <- 0 until width
    } {
      // NDC coordinates [-1, 1]
      val ndcX = (2f * x / width - 1f) * aspect * math.tan(fovRad / 2).toFloat
      val ndcY = (1f - 2f * y / height) * math.tan(fovRad / 2).toFloat
      
      val rayDir = Vec3(ndcX, ndcY, 1f).normalized
      
      val color = RayMarcher.march(camPos, rayDir) match {
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
