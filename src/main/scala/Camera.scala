case class Camera(
  position: Vec3,
  pitch: Float,
  yaw: Float,
  roll: Float
) {
  def forward: Vec3 = {
    val cp = math.cos(pitch).toFloat
    val sp = math.sin(pitch).toFloat
    val cy = math.cos(yaw).toFloat
    val sy = math.sin(yaw).toFloat
    
    Vec3(sy * cp, sp, cy * cp).normalized
  }
  
  def right: Vec3 = {
    val cy = math.cos(yaw).toFloat
    val sy = math.sin(yaw).toFloat
    val cr = math.cos(roll).toFloat
    val sr = math.sin(roll).toFloat
    
    Vec3(cy * cr, sr, -sy * cr).normalized
  }
  
  def up: Vec3 = right.cross(forward).normalized
  
  def getRayDirection(screenX: Int, screenY: Int, width: Int, height: Int, fov: Float): Vec3 = {
    val aspect = width.toFloat / height
    val fovRad = math.toRadians(fov).toFloat
    
    val ndcX = (2f * screenX / width - 1f) * aspect * math.tan(fovRad / 2).toFloat
    val ndcY = (1f - 2f * screenY / height) * math.tan(fovRad / 2).toFloat
    
    val rayLocal = Vec3(ndcX, ndcY, 1f).normalized
    
    val r = right
    val u = up
    val f = forward
    
    (r * rayLocal.x + u * rayLocal.y + f * rayLocal.z).normalized
  }
}

object Camera {
  def default: Camera = Camera(Vec3(0, 0, 0), 0f, 0f, 0f)  // Start in tunnel
}
