case class Hit(point: Vec3, distance: Float, steps: Int, materialId: Int)

object RayMarcher {
  val EPSILON = 0.001f
  val MAX_DIST = 80f
  val MAX_STEPS = 64
  
  val MAT_TUNNEL = 0
  val MAT_DETAIL = 1
  val MAT_PROJECTILE = 2
  
  def march(origin: Vec3, direction: Vec3): Option[Hit] = {
    var t = 0f
    var steps = 0
    
    while (t < MAX_DIST && steps < MAX_STEPS) {
      val p = origin + direction * t
      val (d, mat) = sceneSDF(p)
      
      if (d < EPSILON) {
        return Some(Hit(p, t, steps, mat))
      }
      
      t += d * 0.95f
      steps += 1
    }
    
    None
  }
  
  def sceneSDF(p: Vec3): (Float, Int) = {
    // Basic twisting tunnel
    val twist = math.sin(p.z * 0.1).toFloat * 0.5f
    val pTwisted = Vec3(
      p.x * math.cos(twist).toFloat - p.y * math.sin(twist).toFloat,
      p.x * math.sin(twist).toFloat + p.y * math.cos(twist).toFloat,
      p.z
    )
    
    // Main tunnel radius
    val baseRadius = 8f + math.sin(p.z * 0.2f).toFloat * 2f
    val radiusXY = math.sqrt(pTwisted.x*pTwisted.x + pTwisted.y*pTwisted.y).toFloat
    
    // Fractal noise on the walls - simple multi-octave noise
    val angle = math.atan2(pTwisted.y, pTwisted.x).toFloat
    
    val detail1 = math.sin(angle * 6f + p.z * 0.5f).toFloat * 0.6f
    val detail2 = math.sin(angle * 12f + p.z * 0.8f).toFloat * 0.3f
    val detail3 = math.sin(angle * 24f + p.z * 1.2f).toFloat * 0.15f
    
    val fractalDetail = detail1 + detail2 + detail3
    
    val tunnel = baseRadius + fractalDetail - radiusXY
    
    // Simple floating orbs for interest
    val orbSpacing = 15f
    val orbZ = (p.z / orbSpacing).floor * orbSpacing
    val seed = orbZ.toInt * 7919
    val rng = new scala.util.Random(seed)
    
    val orbX = (rng.nextFloat() - 0.5f) * 10f
    val orbY = (rng.nextFloat() - 0.5f) * 10f
    val orbRadius = 0.8f + rng.nextFloat() * 0.4f
    val orb = (p - Vec3(orbX, orbY, orbZ + orbSpacing/2f)).length - orbRadius
    
    // Projectiles
    val projectile = ProjectileSystem.projectileSDF(p)
    
    val dists = List(
      (tunnel, MAT_TUNNEL),
      (orb, MAT_DETAIL),
      (projectile, MAT_PROJECTILE)
    )
    
    dists.minBy(_._1)
  }
  
  def getNormal(p: Vec3): Vec3 = {
    val e = 0.001f
    Vec3(
      sceneSDF(p + Vec3(e, 0, 0))._1 - sceneSDF(p - Vec3(e, 0, 0))._1,
      sceneSDF(p + Vec3(0, e, 0))._1 - sceneSDF(p - Vec3(0, e, 0))._1,
      sceneSDF(p + Vec3(0, 0, e))._1 - sceneSDF(p - Vec3(0, 0, e))._1
    ).normalized
  }
}
