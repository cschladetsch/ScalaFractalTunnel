case class Hit(point: Vec3, distance: Float, steps: Int, materialId: Int)

object RayMarcher {
  val MAT_TUNNEL = 0
  val MAT_HEALTH_PICKUP = 1
  val MAT_SHIELD_PICKUP = 2
  val MAT_SLOWTIME_PICKUP = 3
  val MAT_OBSTACLE = 4
  
  def march(origin: Vec3, direction: Vec3): Option[Hit] = {
    var t = Config.startOffset
    var steps = 0
    
    while (t < Config.maxDistance && steps < Config.maxSteps) {
      val p = origin + direction * t
      val (d, mat) = sceneSDF(p)
      
      if (d < Config.epsilon) {
        return Some(Hit(p, t, steps, mat))
      }
      
      t += d * Config.stepMultiplier
      steps += 1
    }
    
    None
  }
  
  def getTunnelCenter(z: Float): Vec3 = {
    val x = math.sin(z * Config.curveFreqX).toFloat * Config.curveAmpX
    val y = math.cos(z * Config.curveFreqY).toFloat * Config.curveAmpY
    Vec3(x, y, z)
  }
  
  def sceneSDF(p: Vec3): (Float, Int) = {
    val center = getTunnelCenter(p.z)
    val dx = p.x - center.x
    val dy = p.y - center.y
    val r = math.sqrt(dx*dx + dy*dy).toFloat
    val angle = math.atan2(dy, dx).toFloat
    
    val radiusWave1 = math.sin(p.z * Config.radiusWave1Freq).toFloat * Config.radiusWave1Amp
    val radiusWave2 = math.sin(p.z * Config.radiusWave2Freq).toFloat * Config.radiusWave2Amp
    val radiusWave3 = math.sin(p.z * Config.radiusWave3Freq).toFloat * Config.radiusWave3Amp
    
    val baseRadius = Config.baseRadius + radiusWave1 + radiusWave2 + radiusWave3
    
    // Multi-scale fractal displacement
    var fractal = 0f
    for (i <- 0 until 5) {
      val freq = Config.fractalFreqs(i)
      val amp = Config.fractalAmps(i)
      val phaseZ = Config.fractalPhasesZ(i)
      val sign = if (i % 2 == 0) 1f else -1f
      
      if (i % 2 == 0) {
        fractal += math.sin(angle * freq + p.z * phaseZ * sign).toFloat * amp
      } else {
        fractal += math.cos(angle * freq + p.z * phaseZ * sign).toFloat * amp
      }
    }
    
    val tunnel = baseRadius + fractal - r
    
    // Pickups
    val pickups = GameState.getActivePickups()
    var healthDist = Float.MaxValue
    var shieldDist = Float.MaxValue
    var slowTimeDist = Float.MaxValue
    
    for (pickup <- pickups) {
      val d = (p - pickup.position).length - 0.8f
      pickup.pickupType match {
        case HealthPickup => healthDist = math.min(healthDist, d)
        case ShieldPickup => shieldDist = math.min(shieldDist, d)
        case SlowTimePickup => slowTimeDist = math.min(slowTimeDist, d)
      }
    }
    
    // Obstacles
    val obstacles = GameState.getActiveObstacles()
    val obstacleDist = if (obstacles.isEmpty) Float.MaxValue
    else obstacles.map(o => (p - o.position).length - 1.0f).min
    
    val dists = List(
      (tunnel, MAT_TUNNEL),
      (healthDist, MAT_HEALTH_PICKUP),
      (shieldDist, MAT_SHIELD_PICKUP),
      (slowTimeDist, MAT_SLOWTIME_PICKUP),
      (obstacleDist, MAT_OBSTACLE)
    )
    
    dists.minBy(_._1)
  }
  
  def getNormal(p: Vec3): Vec3 = {
    val e = Config.epsilon
    Vec3(
      sceneSDF(p + Vec3(e, 0, 0))._1 - sceneSDF(p - Vec3(e, 0, 0))._1,
      sceneSDF(p + Vec3(0, e, 0))._1 - sceneSDF(p - Vec3(0, e, 0))._1,
      sceneSDF(p + Vec3(0, 0, e))._1 - sceneSDF(p - Vec3(0, 0, e))._1
    ).normalized
  }
}
