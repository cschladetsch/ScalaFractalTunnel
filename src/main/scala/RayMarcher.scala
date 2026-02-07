case class Hit(point: Vec3, distance: Float, steps: Int)

object RayMarcher {
  val EPSILON = 0.001f
  val MAX_DIST = 100f
  val MAX_STEPS = 100
  
  def march(origin: Vec3, direction: Vec3): Option[Hit] = {
    var t = 0f
    var steps = 0
    
    while (t < MAX_DIST && steps < MAX_STEPS) {
      val p = origin + direction * t
      val d = sceneSDF(p)
      
      if (d < EPSILON) {
        return Some(Hit(p, t, steps))
      }
      
      t += d * 0.9f  // slight dampening
      steps += 1
    }
    
    None
  }
  
  // Simple test scene - sphere at origin
  def sceneSDF(p: Vec3): Float = {
    sphereSDF(p, Vec3(0, 0, 10), 3f)
  }
  
  def sphereSDF(p: Vec3, center: Vec3, radius: Float): Float = {
    (p - center).length - radius
  }
  
  def getNormal(p: Vec3): Vec3 = {
    val e = 0.001f
    Vec3(
      sceneSDF(p + Vec3(e, 0, 0)) - sceneSDF(p - Vec3(e, 0, 0)),
      sceneSDF(p + Vec3(0, e, 0)) - sceneSDF(p - Vec3(0, e, 0)),
      sceneSDF(p + Vec3(0, 0, e)) - sceneSDF(p - Vec3(0, 0, e))
    ).normalized
  }
}
