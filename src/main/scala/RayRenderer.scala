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
          
          val fogAmount = math.min(1f, hit.distance / 50f)
          val depthFade = 1f - fogAmount * 0.5f
          
          // Color cycle based on position
          val t = System.currentTimeMillis() * 0.0003f
          val hue1 = math.sin(hit.point.z * 0.1f + t).toFloat * 0.5f + 0.5f
          val hue2 = math.sin(hit.point.z * 0.15f + t + 2).toFloat * 0.5f + 0.5f
          val hue3 = math.sin(hit.point.z * 0.12f + t + 4).toFloat * 0.5f + 0.5f
          
          val baseColor = hit.materialId match {
            case RayMarcher.MAT_TUNNEL =>
              // Rainbow tunnel with fractal edges
              val r = (150 * hue1 + 100).toInt
              val g = (120 * hue2 + 80).toInt
              val b = (200 * hue3 + 100).toInt
              (r, g, b)
              
            case RayMarcher.MAT_DETAIL =>
              // Glowing orbs
              val glow = 1.5f - fogAmount
              val r = (255 * glow * hue3).toInt min 255
              val g = (200 * glow * hue1).toInt min 255
              val b = (255 * glow * hue2).toInt min 255
              (r, g, b)
              
            case RayMarcher.MAT_PROJECTILE =>
              (255, 255, 100)
              
            case _ => (180, 180, 180)
          }
          
          val intensity = (0.3f + diffuse * 0.7f) * depthFade
          
          new Color(
            (intensity * baseColor._1).toInt min 255,
            (intensity * baseColor._2).toInt min 255,
            (intensity * baseColor._3).toInt min 255
          )
        
        case None =>
          // Dark space
          val r = (rayDir.x * 30 + 20).toInt max 0 min 255
          val g = (rayDir.y * 30 + 30).toInt max 0 min 255
          val b = (rayDir.z * 50 + 40).toInt max 0 min 255
          new Color(r, g, b)
      }
      
      img.setRGB(x, y, color.getRGB)
    }
    
    img
  }
}
