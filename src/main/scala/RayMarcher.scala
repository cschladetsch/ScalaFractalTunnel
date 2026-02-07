case class Hit(point: Vec3, distance: Float, steps: Int, materialId: Int)

object RayMarcher {
  val EPSILON = 0.01f
  val MAX_DIST = 60f
  val MAX_STEPS = 55
  
  val MAT_TUNNEL = 0
  val MAT_PROJECTILE = 1
  
  def march(origin: Vec3, direction: Vec3): Option[Hit] = {
    var t = 0.1f
    var steps = 0
    
    while (t < MAX_DIST && steps < MAX_STEPS) {
      val p = origin + direction * t
      val (d, mat) = sceneSDF(p)
      
      if (d < EPSILON) {
        return Some(Hit(p, t, steps, mat))
      }
      
      t += d
      steps += 1
    }
    
    None
  }
  
  def getTunnelCenter(z: Float): Vec3 = {
    val x = math.sin(z * 0.15f).toFloat * 6f
    val y = math.cos(z * 0.12f).toFloat * 5f
    Vec3(x, y, z)
  }
  
  def sceneSDF(p: Vec3): (Float, Int) = {
    val t = System.currentTimeMillis() * 0.001f
    
    // Curved tunnel
    val center = getTunnelCenter(p.z)
    val dx = p.x - center.x
    val dy = p.y - center.y
    val r = math.sqrt(dx*dx + dy*dy).toFloat
    val angle = math.atan2(dy, dx).toFloat
    
    // Base radius varies
    val baseRadius = 7f + math.sin(p.z * 0.18f).toFloat * 1.5f
    
    // MULTI-SCALE FRACTAL DISPLACEMENT
    val freq1 = math.sin(angle * 8f + p.z * 0.6f + t * 0.3f).toFloat * 1.0f
    val freq2 = math.sin(angle * 16f - p.z * 1.2f).toFloat * 0.5f
    val freq3 = math.cos(angle * 32f + p.z * 2.4f).toFloat * 0.25f
    val freq4 = math.sin(angle * 64f + p.z * 4.8f).toFloat * 0.125f
    val freq5 = math.cos(angle * 128f - p.z * 9.6f).toFloat * 0.0625f
    
    val fractalDisplacement = freq1 + freq2 + freq3 + freq4 + freq5
    
    val tunnel = baseRadius + fractalDisplacement - r
    val proj = ProjectileSystem.projectileSDF(p)
    
    if (proj < tunnel) (proj, MAT_PROJECTILE)
    else (tunnel, MAT_TUNNEL)
  }
  
  def getNormal(p: Vec3): Vec3 = {
    val e = 0.01f
    Vec3(
      sceneSDF(p + Vec3(e, 0, 0))._1 - sceneSDF(p - Vec3(e, 0, 0))._1,
      sceneSDF(p + Vec3(0, e, 0))._1 - sceneSDF(p - Vec3(0, e, 0))._1,
      sceneSDF(p + Vec3(0, 0, e))._1 - sceneSDF(p - Vec3(0, 0, e))._1
    ).normalized
  }
}
