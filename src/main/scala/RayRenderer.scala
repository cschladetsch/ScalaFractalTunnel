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
          
          // Distance-based fog and color variation
          val fogAmount = math.min(1f, hit.distance / 50f)
          val depthColor = 1f - fogAmount * 0.5f
          
          // Position-based color variation
          val colorShift = (
            math.sin(hit.point.z * 0.1).toFloat * 0.3f + 0.7f,
            math.sin(hit.point.z * 0.15 + 2).toFloat * 0.3f + 0.7f,
            math.sin(hit.point.z * 0.12 + 4).toFloat * 0.3f + 0.7f
          )
          
          val baseColor = hit.materialId match {
            case RayMarcher.MAT_TUNNEL =>
              // Rainbow tunnel walls
              val r = (150 * colorShift._1 * depthColor).toInt
              val g = (100 * colorShift._2 * depthColor).toInt
              val b = (200 * colorShift._3 * depthColor).toInt
              (r, g, b)
              
            case RayMarcher.MAT_GLOW =>
              // Bright glowing orbs
              val glow = 1.5f - fogAmount
              val r = (255 * glow).toInt min 255
              val g = (150 * glow * colorShift._2).toInt min 255
              val b = (200 * glow * colorShift._3).toInt min 255
              (r, g, b)
              
            case RayMarcher.MAT_PROJECTILE =>
              (255, 255, 100)
              
            case _ => (180, 180, 180)
          }
          
          val intensity = 0.3f + diffuse * 0.7f
          
          new Color(
            (intensity * baseColor._1).toInt min 255,
            (intensity * baseColor._2).toInt min 255,
            (intensity * baseColor._3).toInt min 255
          )
        
        case None =>
          // Colorful space background based on ray direction
          val starField = if (math.random() < 0.001) 200 else 0
          val r = (rayDir.x * 30 + 20 + starField).toInt max 0 min 255
          val g = (rayDir.y * 30 + 30 + starField).toInt max 0 min 255
          val b = (rayDir.z * 50 + 40 + starField).toInt max 0 min 255
          new Color(r, g, b)
      }
      
      img.setRGB(x, y, color.getRGB)
    }
    
    img
  }
}
