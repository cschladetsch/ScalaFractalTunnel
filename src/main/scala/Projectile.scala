case class Projectile(
  position: Vec3,
  velocity: Vec3,
  lifetime: Float
)

object ProjectileSystem {
  private var projectiles = List[Projectile]()
  private val speed = 15f        // Reduced from 30f
  private val maxLifetime = 5f
  
  def fire(position: Vec3, direction: Vec3): Unit = {
    // Spawn closer to camera (0.5f instead of 2f)
    projectiles = Projectile(position + direction * 0.5f, direction * speed, maxLifetime) :: projectiles
  }
  
  def update(dt: Float): Unit = {
    projectiles = projectiles.flatMap { proj =>
      val newPos = proj.position + proj.velocity * dt
      val newLifetime = proj.lifetime - dt
      
      val dist = RayMarcher.sceneSDF(newPos)._1
      
      if (dist < 0.2f || newLifetime <= 0) {
        None
      } else {
        Some(proj.copy(position = newPos, lifetime = newLifetime))
      }
    }
  }
  
  def getAll: List[Projectile] = projectiles
  
  def projectileSDF(p: Vec3): Float = {
    if (projectiles.isEmpty) Float.MaxValue
    else projectiles.map(proj => (p - proj.position).length - 0.3f).min
  }
}
