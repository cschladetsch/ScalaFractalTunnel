case class Particle(
  position: Vec3,
  velocity: Vec3,
  lifetime: Float,
  maxLifetime: Float,
  color: (Int, Int, Int)
)

object ParticleSystem {
  private var particles = List[Particle]()
  
  def spawn(pos: Vec3, count: Int, color: (Int, Int, Int)): Unit = {
    val rng = new scala.util.Random()
    for (_ <- 0 until count) {
      val angle = rng.nextFloat() * math.Pi.toFloat * 2f
      val speed = rng.nextFloat() * (Config.particleMaxSpeed - Config.particleMinSpeed) + Config.particleMinSpeed
      val vx = math.cos(angle).toFloat * speed
      val vy = math.sin(angle).toFloat * speed
      val vz = (rng.nextFloat() - 0.5f) * 3f
      
      particles = Particle(pos, Vec3(vx, vy, vz), Config.particleLifetime, Config.particleLifetime, color) :: particles
    }
  }
  
  def update(dt: Float): Unit = {
    particles = particles.flatMap { p =>
      val newPos = p.position + p.velocity * dt
      val newLife = p.lifetime - dt
      if (newLife > 0) Some(p.copy(position = newPos, lifetime = newLife))
      else None
    }
  }
  
  def getAll: List[Particle] = particles
  
  def clear(): Unit = particles = List()
}
