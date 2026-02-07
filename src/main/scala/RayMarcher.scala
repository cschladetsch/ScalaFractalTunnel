case class Hit(point: Vec3, distance: Float, steps: Int, materialId: Int)

object RayMarcher {
  val EPSILON = 0.001f
  val MAX_DIST = 80f
  val MAX_STEPS = 80
  
  val MAT_TUNNEL = 0
  val MAT_CRYSTAL = 1
  val MAT_FRACTAL = 2
  val MAT_ENERGY = 3
  val MAT_PROJECTILE = 4
  
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
    // Main tunnel that twists and deforms
    val zPhase = p.z * 0.05f
    val twist = math.sin(zPhase).toFloat * 1.2f
    val wobble = math.cos(zPhase * 2.3f).toFloat * 0.8f
    
    val pTwisted = Vec3(
      p.x * math.cos(twist).toFloat - p.y * math.sin(twist).toFloat + wobble,
      p.x * math.sin(twist).toFloat + p.y * math.cos(twist).toFloat,
      p.z
    )
    
    // Organic pulsating radius
    val pulse = math.sin(p.z * 0.2f + System.currentTimeMillis() * 0.001f).toFloat
    val radiusVariation = 3f + pulse * 2f
    val tunnelRadius = 8f + radiusVariation
    val radiusXY = math.sqrt(pTwisted.x*pTwisted.x + pTwisted.y*pTwisted.y).toFloat
    val tunnel = tunnelRadius - radiusXY
    
    // Mandelbrot-inspired bulges on walls
    val mandelPos = Vec3(
      (pTwisted.x % 8f) - 4f,
      (pTwisted.y % 8f) - 4f,
      pTwisted.z
    )
    
    var z = Vec3(mandelPos.x, mandelPos.y, 0)
    var dr = 1f
    var r = 0f
    var iterations = 0
    val maxIter = 8
    
    while (iterations < maxIter && r < 2f) {
      val r2 = z.dot(z)
      if (r2 > 4f) {
        iterations = maxIter
      } else {
        r = math.sqrt(r2).toFloat
        
        // Mandelbulb-like formula (simplified)
        val theta = math.atan2(z.y, z.x).toFloat
        val phi = math.asin(z.z / r).toFloat
        
        dr = math.pow(r, 7f).toFloat * 8f * dr + 1f
        
        val zr = math.pow(r, 8f).toFloat
        val newTheta = theta * 8f
        val newPhi = phi * 8f
        
        z = Vec3(
          zr * math.sin(newPhi).toFloat * math.cos(newTheta).toFloat,
          zr * math.sin(newPhi).toFloat * math.sin(newTheta).toFloat,
          zr * math.cos(newPhi).toFloat
        ) + mandelPos
        
        iterations += 1
      }
    }
    
    val fractal = 0.25f * math.log(r).toFloat * r / dr
    
    // Impossible recursive boxes
    val boxMod = 4f
    val qBox = Vec3(
      (p.x % boxMod) - boxMod/2f,
      (p.y % boxMod) - boxMod/2f,
      p.z
    )
    
    val boxSize = 1.2f + math.sin(p.z * 0.15f).toFloat * 0.6f
    val box = boxSDF(qBox, Vec3(0, 0, 0), Vec3(boxSize, boxSize, boxSize))
    
    // Crystal formations growing from walls
    val crystalSpacing = 12f
    val crystalZ = (p.z / crystalSpacing).floor * crystalSpacing
    val seed = crystalZ.toInt * 9973
    val rng = new scala.util.Random(seed)
    
    val crystalAngle = rng.nextFloat() * math.Pi.toFloat * 2f
    val crystalDist = 6f + rng.nextFloat() * 2f
    val crystalX = crystalDist * math.cos(crystalAngle).toFloat
    val crystalY = crystalDist * math.sin(crystalAngle).toFloat
    
    val toCrystal = p - Vec3(crystalX, crystalY, crystalZ + crystalSpacing/2f)
    val crystalBase = sphereSDF(p, Vec3(crystalX, crystalY, crystalZ + crystalSpacing/2f), 1.5f)
    
    // Add spiky protrusions
    val spikes = toCrystal.length - (2f + math.abs(math.sin(math.atan2(toCrystal.y, toCrystal.x).toFloat * 5f) * 0.8f).toFloat)
    val crystal = math.min(crystalBase, spikes)
    
    // Floating energy spheres with morphing
    val energySpacing = 18f
    val energyZ = (p.z / energySpacing).floor * energySpacing
    val eSeed = energyZ.toInt * 7717
    val eRng = new scala.util.Random(eSeed)
    
    val eX = (eRng.nextFloat() - 0.5f) * 8f
    val eY = (eRng.nextFloat() - 0.5f) * 8f
    val ePos = Vec3(eX, eY, energyZ + energySpacing/2f)
    
    val t = System.currentTimeMillis() * 0.002f
    val morph = math.sin(t + ePos.z * 0.1f).toFloat
    val energyRadius = 1f + morph * 0.5f
    val energy = sphereSDF(p, ePos, energyRadius)
    
    // Projectiles
    val projectile = ProjectileSystem.projectileSDF(p)
    
    // Smooth blend tunnel with fractal
    val tunnelFractal = smoothMin(tunnel, fractal * 3f, 1.5f)
    
    // Find minimum with material
    val dists = List(
      (tunnelFractal, MAT_TUNNEL),
      (box - 0.1f, MAT_FRACTAL),  // Hollow boxes
      (crystal, MAT_CRYSTAL),
      (energy, MAT_ENERGY),
      (projectile, MAT_PROJECTILE)
    )
    
    dists.minBy(_._1)
  }
  
  def sphereSDF(p: Vec3, center: Vec3, radius: Float): Float = {
    (p - center).length - radius
  }
  
  def boxSDF(p: Vec3, center: Vec3, size: Vec3): Float = {
    val q = Vec3(
      math.abs(p.x - center.x) - size.x,
      math.abs(p.y - center.y) - size.y,
      math.abs(p.z - center.z) - size.z
    )
    
    val outside = Vec3(
      math.max(q.x, 0),
      math.max(q.y, 0),
      math.max(q.z, 0)
    ).length
    
    val inside = math.min(math.max(q.x, math.max(q.y, q.z)), 0f)
    
    outside + inside
  }
  
  def smoothMin(a: Float, b: Float, k: Float): Float = {
    val h = math.max(k - math.abs(a - b), 0f) / k
    math.min(a, b).toFloat - h * h * k * 0.25f
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
