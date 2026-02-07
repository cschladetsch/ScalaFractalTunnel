import scala.io.Source
import java.io.PrintWriter

case class HealthPickup(position: Vec3, collected: Boolean)

object GameState {
  var health = 100f
  var distance = 0f
  var isAlive = true
  var baseSpeed = 4f  // Start slower
  var currentSpeed = 4f
  var lastCheckpoint = 0
  var pickups = List[HealthPickup]()
  
  def reset(): Unit = {
    health = 100f
    distance = 0f
    isAlive = true
    baseSpeed = 4f
    currentSpeed = 4f
    lastCheckpoint = 0
    pickups = List()
    spawnPickups()
  }
  
  def spawnPickups(): Unit = {
    // Spawn pickups every 30-50 units
    pickups = List()
    for (z <- 15 to 500 by 35) {
      val center = RayMarcher.getTunnelCenter(z.toFloat)
      val rng = new scala.util.Random(z * 131)
      val angle = rng.nextFloat() * math.Pi.toFloat * 2f
      val radius = rng.nextFloat() * 3f + 2f
      
      val x = center.x + radius * math.cos(angle).toFloat
      val y = center.y + radius * math.sin(angle).toFloat
      
      pickups = HealthPickup(Vec3(x, y, z.toFloat), false) :: pickups
    }
  }
  
  def update(playerPos: Vec3, dt: Float): Unit = {
    distance = playerPos.z
    
    // Speed increases over time (caps at 15)
    baseSpeed = math.min(4f + distance * 0.015f, 15f)
    currentSpeed = baseSpeed
    
    // Check for checkpoint
    val checkpoint = (distance / 100).toInt * 100
    if (checkpoint > lastCheckpoint && checkpoint > 0) {
      lastCheckpoint = checkpoint
      health = math.min(100f, health + 20f)
      // Show checkpoint message handled in Main
    }
    
    // Check pickup collection
    pickups = pickups.map { pickup =>
      if (!pickup.collected && (playerPos - pickup.position).length < 1.5f) {
        health = math.min(100f, health + 25f)
        pickup.copy(collected = true)
      } else {
        pickup
      }
    }
    
    // Damage when near walls
    val (distToWall, _) = RayMarcher.sceneSDF(playerPos)
    if (distToWall < 1.5f) {
      val damageRate = (1.5f - distToWall) * 20f
      health -= damageRate * dt
      if (health <= 0f) {
        health = 0f
        isAlive = false
        saveScore(distance.toInt)
      }
    }
  }
  
  def getActivePickups(): List[HealthPickup] = {
    pickups.filter(p => !p.collected && p.position.z > distance - 20 && p.position.z < distance + 60)
  }
  
  def getHighScores(): List[(String, Int)] = {
    try {
      val lines = Source.fromFile("scores.txt").getLines().toList
      lines.map { line =>
        val parts = line.split(",")
        (parts(0), parts(1).toInt)
      }.sortBy(-_._2).take(10)
    } catch {
      case _: Exception => List()
    }
  }
  
  def saveScore(distance: Int): Unit = {
    try {
      val timestamp = System.currentTimeMillis()
      val writer = new PrintWriter(new java.io.FileWriter("scores.txt", true))
      writer.println(s"$timestamp,$distance")
      writer.close()
    } catch {
      case _: Exception => ()
    }
  }
}
