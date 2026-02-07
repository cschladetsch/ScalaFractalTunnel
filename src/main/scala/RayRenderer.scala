import java.awt.image.BufferedImage
import java.awt.Color

object RayRenderer {
  def render(width: Int, height: Int, camera: Camera): BufferedImage = {
    val img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    
    val speedFactor = math.min((GameState.currentSpeed - Config.startSpeed) / (Config.maxSpeed - Config.startSpeed), 1f)
    
    for {
      y <- 0 until height
      x <- 0 until width
    } {
      val rayDir = camera.getRayDirection(x, y, width, height, Config.fov)
      
      val color = RayMarcher.march(camera.position, rayDir) match {
        case Some(hit) =>
          val phase = hit.point.z * Config.colorPhaseMultiplier
          
          hit.materialId match {
            case RayMarcher.MAT_TUNNEL =>
              val r = ((math.sin(phase).toFloat * 0.5f + 0.5f) * (150 + speedFactor * 105)).toInt
              val g = ((math.sin(phase + 2).toFloat * 0.5f + 0.5f) * (255 - speedFactor * 100)).toInt
              val b = ((math.sin(phase + 4).toFloat * 0.5f + 0.5f) * (255 - speedFactor * 150)).toInt
              new Color(r max 0 min 255, g max 50 min 255, b max 50 min 255)
              
            case RayMarcher.MAT_HEALTH_PICKUP =>
              val normal = RayMarcher.getNormal(hit.point)
              val lightDir = Vec3(0.3f, -0.5f, -0.8f).normalized
              val diffuse = math.max(0.3f, normal.dot(lightDir))
              
              val t = System.currentTimeMillis() * 0.003f
              val pulse = (math.sin(t).toFloat * 0.2f + 1f)
              val baseG = (180 * pulse).toInt min 255
              
              new Color(
                (30 * diffuse).toInt min 255,
                (baseG * diffuse).toInt min 255,
                (30 * diffuse).toInt min 255
              )
              
            case RayMarcher.MAT_SHIELD_PICKUP =>
              val normal = RayMarcher.getNormal(hit.point)
              val lightDir = Vec3(0.3f, -0.5f, -0.8f).normalized
              val diffuse = math.max(0.3f, normal.dot(lightDir))
              
              val t = System.currentTimeMillis() * 0.003f
              val pulse = (math.sin(t).toFloat * 0.2f + 1f)
              val baseB = (220 * pulse).toInt min 255
              
              new Color(
                (40 * diffuse).toInt min 255,
                (120 * diffuse).toInt min 255,
                (baseB * diffuse).toInt min 255
              )
              
            case RayMarcher.MAT_SLOWTIME_PICKUP =>
              val normal = RayMarcher.getNormal(hit.point)
              val lightDir = Vec3(0.3f, -0.5f, -0.8f).normalized
              val diffuse = math.max(0.3f, normal.dot(lightDir))
              
              val t = System.currentTimeMillis() * 0.003f
              val pulse = (math.sin(t).toFloat * 0.2f + 1f)
              val baseY = (220 * pulse).toInt min 255
              
              new Color(
                (baseY * diffuse).toInt min 255,
                (baseY * diffuse).toInt min 255,
                (40 * diffuse).toInt min 255
              )
              
            case RayMarcher.MAT_OBSTACLE =>
              val normal = RayMarcher.getNormal(hit.point)
              val lightDir = Vec3(0.3f, -0.5f, -0.8f).normalized
              val diffuse = math.max(0.4f, normal.dot(lightDir))
              
              val t = System.currentTimeMillis() * 0.005f
              val pulse = (math.sin(t).toFloat * 0.3f + 1f)
              val baseR = (200 * pulse).toInt min 255
              
              new Color(
                (baseR * diffuse).toInt min 255,
                0,
                0
              )
              
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
