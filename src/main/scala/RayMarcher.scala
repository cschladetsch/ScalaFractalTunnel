case class Hit(point: Vec3, distance: Float, steps: Int, materialId: Int)

object RayMarcher {
  val EPSILON = 0.001f
  val MAX_DIST = 100f
  val MAX_STEPS = 100
  
  val MAT_TUNNEL = 0
  val MAT_WALL = 1
  val MAT_OBSTACLE = 2
  
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
    // Tunnel walls
    val tunnelRadius = 8f
    val radiusXY = math.sqrt(p.x*p.x + p.y*p.y).toFloat
    val tunnel = tunnelRadius - radiusXY
    
    // Walls with doorways
    val wallSpacing = 20f
    val wallThickness = 0.5f
    val segmentZ = (p.z / wallSpacing).floor * wallSpacing
    val wallCenter = segmentZ + wallSpacing / 2
    
    val wall = boxSDF(p, Vec3(0, 0, wallCenter), Vec3(7, 7, wallThickness))
    val doorway = boxSDF(p, Vec3(0, 0, wallCenter), Vec3(2, 2, wallThickness + 0.1f))
    val wallWithDoor = subtraction(wall, doorway)
    
    // Obstacles
    val segmentId = (p.z / 10f).floor.toInt
    val rng = new scala.util.Random(segmentId * 12345)
    val obsX = (rng.nextFloat() - 0.5f) * 6f
    val obsY = (rng.nextFloat() - 0.5f) * 6f
    val obsZ = segmentId * 10f + 5f
    val obstacle = boxSDF(p, Vec3(obsX, obsY, obsZ), Vec3(1, 1, 1))
    
    // Find closest and return material
    val dists = List((tunnel, MAT_TUNNEL), (wallWithDoor, MAT_WALL), (obstacle, MAT_OBSTACLE))
    dists.minBy(_._1)
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
  
  def union(d1: Float, d2: Float): Float = math.min(d1, d2)
  def subtraction(d1: Float, d2: Float): Float = math.max(d1, -d2)
  
  def getNormal(p: Vec3): Vec3 = {
    val e = 0.001f
    Vec3(
      sceneSDF(p)._1 + e - sceneSDF(p - Vec3(e, 0, 0))._1,
      sceneSDF(p + Vec3(0, e, 0))._1 - sceneSDF(p - Vec3(0, e, 0))._1,
      sceneSDF(p + Vec3(0, 0, e))._1 - sceneSDF(p - Vec3(0, 0, e))._1
    ).normalized
  }
}
