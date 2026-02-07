case class Hit(point: Vec3, distance: Float, steps: Int, materialId: Int)

object RayMarcher {
  val EPSILON = 0.001f
  val MAX_DIST = 100f
  val MAX_STEPS = 100
  
  val MAT_TUNNEL = 0
  val MAT_CRYSTAL = 1
  val MAT_GLOW = 2
  val MAT_PROJECTILE = 3
  
  def march(origin: Vec3, direction: Vec3): Option[Hit] = {
    var t = 0f
    var steps = 0
    
    while (t < MAX_DIST && steps < MAX_STEPS) {
      val p = origin + direction * t
      val (d, mat) = sceneSDF(p)
      
      if (d < EPSILON) {
        return Some(Hit(p, t, steps, mat))
      }
      
      t += d * 0.9f
      steps += 1
    }
    
    None
  }
  
  def sceneSDF(p: Vec3): (Float, Int) = {
    // Twisted organic tunnel
    val twist = math.sin(p.z * 0.1).toFloat * 0.3f
    val pTwisted = Vec3(
      p.x * math.cos(twist).toFloat - p.y * math.sin(twist).toFloat,
      p.x * math.sin(twist).toFloat + p.y * math.cos(twist).toFloat,
      p.z
    )
    
    // Pulsating radius based on Z
    val radiusVariation = 2f + math.sin(p.z * 0.3).toFloat * 1.5f
    val tunnelRadius = 8f + radiusVariation
    val radiusXY = math.sqrt(pTwisted.x*pTwisted.x + pTwisted.y*pTwisted.y).toFloat
    val tunnel = tunnelRadius - radiusXY
    
    // Fractal crystal structures on walls
    val crystalPattern = (
      math.sin(p.x * 2f + p.z * 0.5f).toFloat * 
      math.cos(p.y * 2f + p.z * 0.3f).toFloat * 
      math.sin(p.z * 1.5f).toFloat
    ) * 1.5f
    
    val crystalPos = Vec3(
      (p.x % 10f) - 5f,
      (p.y % 10f) - 5f,
      p.z
    )
    val crystal = sphereSDF(crystalPos, Vec3(0, 0, 0), 1.5f) + crystalPattern
    
    // Glowing orbs floating in space
    val orbZ = (p.z / 15f).floor * 15f
    val seed = orbZ.toInt * 7919
    val rng = new scala.util.Random(seed)
    val orbX = (rng.nextFloat() - 0.5f) * 10f
    val orbY = (rng.nextFloat() - 0.5f) * 10f
    val orbRadius = 0.5f + rng.nextFloat() * 0.5f
    val orb = sphereSDF(p, Vec3(orbX, orbY, orbZ + 7.5f), orbRadius)
    
    // Projectiles
    val projectile = ProjectileSystem.projectileSDF(p)
    
    // Combine with smooth min for organic blending
    val tunnelCrystal = smoothMin(tunnel, crystal, 0.8f)
    
    val dists = List(
      (tunnelCrystal, MAT_TUNNEL),
      (orb, MAT_GLOW),
      (projectile, MAT_PROJECTILE)
    )
    dists.minBy(_._1)
  }
  
  def sphereSDF(p: Vec3, center: Vec3, radius: Float): Float = {
    (p - center).length - radius
  }
  
  def smoothMin(a: Float, b: Float, k: Float): Float = {
    val h = math.max(k - math.abs(a - b), 0f) / k
    math.min(a, b).toFloat - h * h * k * 0.25f
  }
  
  def union(d1: Float, d2: Float): Float = math.min(d1, d2)
  
  def getNormal(p: Vec3): Vec3 = {
    val e = 0.001f
    Vec3(
      sceneSDF(p + Vec3(e, 0, 0))._1 - sceneSDF(p - Vec3(e, 0, 0))._1,
      sceneSDF(p + Vec3(0, e, 0))._1 - sceneSDF(p - Vec3(0, e, 0))._1,
      sceneSDF(p + Vec3(0, 0, e))._1 - sceneSDF(p - Vec3(0, 0, e))._1
    ).normalized
  }
}
