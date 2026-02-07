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
          
          // Ambient occlusion approximation from ray steps
          val ao = 1f - (hit.steps.toFloat / 80f) * 0.6f
          
          // Distance fog
          val fogAmount = math.min(1f, hit.distance / 60f)
          val depthFade = 1f - fogAmount * 0.7f
          
          // Position-based color cycling
          val cycle = (
            math.sin(hit.point.z * 0.08f + System.currentTimeMillis() * 0.0005f).toFloat * 0.4f + 0.6f,
            math.sin(hit.point.z * 0.11f + System.currentTimeMillis() * 0.0007f + 2).toFloat * 0.4f + 0.6f,
            math.sin(hit.point.z * 0.09f + System.currentTimeMillis() * 0.0006f + 4).toFloat * 0.4f + 0.6f
          )
          
          val baseColor = hit.materialId match {
            case RayMarcher.MAT_TUNNEL =>
              // Iridescent tunnel walls
              val r = (120 * cycle._1 * depthFade).toInt
              val g = (80 * cycle._2 * depthFade).toInt  
              val b = (200 * cycle._3 * depthFade).toInt
              (r, g, b)
              
            case RayMarcher.MAT_FRACTAL =>
              // Impossible geometry - shifting colors
              val r = (200 * cycle._2 * depthFade).toInt
              val g = (150 * cycle._1 * depthFade).toInt
              val b = (100 * cycle._3 * depthFade).toInt
              (r, g, b)
              
            case RayMarcher.MAT_CRYSTAL =>
              // Glowing crystals
              val glow = (1.3f - fogAmount) * ao
              val r = (255 * glow * cycle._3).toInt min 255
              val g = (180 * glow * cycle._1).toInt min 255
              val b = (255 * glow * cycle._2).toInt min 255
              (r, g, b)
              
            case RayMarcher.MAT_ENERGY =>
              // Pulsating energy orbs
              val pulse = math.sin(System.currentTimeMillis() * 0.003f).toFloat * 0.3f + 1f
              val glow = pulse * (1.5f - fogAmount)
              val r = (200 * glow * cycle._1).toInt min 255
              val g = (255 * glow).toInt min 255
              val b = (150 * glow * cycle._3).toInt min 255
              (r, g, b)
              
            case RayMarcher.MAT_PROJECTILE =>
              (255, 255, 100)
              
            case _ => (180, 180, 180)
          }
          
          val intensity = (0.4f + diffuse * 0.6f) * ao
          
          new Color(
            (intensity * baseColor._1).toInt min 255,
            (intensity * baseColor._2).toInt min 255,
            (intensity * baseColor._3).toInt min 255
          )
        
        case None =>
          // Nebula background
          val nebula = (
            math.sin(rayDir.x * 5f + rayDir.z * 3f).toFloat * 0.5f + 0.5f,
            math.sin(rayDir.y * 4f + rayDir.z * 2f).toFloat * 0.5f + 0.5f,
            math.sin(rayDir.z * 6f + rayDir.x * 2f).toFloat * 0.5f + 0.5f
          )
          val stars = if (math.random() < 0.002) 255 else 0
          
          new Color(
            (nebula._1 * 60 + stars).toInt min 255,
            (nebula._2 * 40 + stars).toInt min 255,
            (nebula._3 * 80 + stars).toInt min 255
          )
      }
      
      img.setRGB(x, y, color.getRGB)
    }
    
    img
  }
}
