import scala.io.Source
import java.io.PrintWriter

sealed trait PickupType
case object HealthPickup extends PickupType
case object ShieldPickup extends PickupType
case object SlowTimePickup extends PickupType

case class Pickup(position: Vec3, pickupType: PickupType, collected: Boolean)
case class Obstacle(position: Vec3)

object GameState {
  var health = 100f
  var distance = 0f
  var isAlive = true
  var baseSpeed = 3f
  var currentSpeed = 3f
  var lastCheckpoint = 0
  var pickups = List[Pickup]()
  var obstacles = List[Obstacle]()
  
  var shieldActive = false
  var shieldEndTime = 0L
  var slowTimeActive = false
  var slowTimeEndTime = 0L
  
  var comboTime = 0f
  var comboMultiplier = 1f
  var lastDamageTime = 0L
  
  var perfectSectionStart = 0f
  var perfectSectionActive = true
  
  var bestDistance = 0f
  var newRecord = false
  var countdownTime = 3f
  var gameStarted = false
  
  var damageShake = 0f
  
  def reset(): Unit = {
    health = 100f
    distance = 0f
    isAlive = true
    baseSpeed = 3f
    currentSpeed = 3f
    lastCheckpoint = 0
    pickups = List()
    obstacles = List()
    shieldActive = false
    slowTimeActive = false
    comboTime = 0f
    comboMultiplier = 1f
    perfectSectionStart = 0f
    perfectSectionActive = true
    newRecord = false
    countdownTime = 3f
    gameStarted = false
    damageShake = 0f
    ParticleSystem.clear()
    
    bestDistance = getHighScores().headOption.map(_._2.toFloat).getOrElse(0f)
    
    spawnPickupsAndObstacles()
  }
  
  def spawnPickupsAndObstacles(): Unit = {
    pickups = List()
    obstacles = List()
    val rng = new scala.util.Random(12345)
    
    for (z <- 20 to 800 by 25) {
      val center = RayMarcher.getTunnelCenter(z.toFloat)
      val angle = rng.nextFloat() * math.Pi.toFloat * 2f
      val radius = rng.nextFloat() * 3f + 2f
      
      val x = center.x + radius * math.cos(angle).toFloat
      val y = center.y + radius * math.sin(angle).toFloat
      
      val pickupType = rng.nextFloat() match {
        case f if f < 0.6f => HealthPickup
        case f if f < 0.8f => ShieldPickup
        case _ => SlowTimePickup
      }
      
      pickups = Pickup(Vec3(x, y, z.toFloat), pickupType, false) :: pickups
      
      if (z > 50 && rng.nextFloat() < 0.3f) {
        val oAngle = rng.nextFloat() * math.Pi.toFloat * 2f
        val oRadius = rng.nextFloat() * 4f + 1f
        val ox = center.x + oRadius * math.cos(oAngle).toFloat
        val oy = center.y + oRadius * math.sin(oAngle).toFloat
        obstacles = Obstacle(Vec3(ox, oy, z.toFloat)) :: obstacles
      }
    }
  }
  
  def update(playerPos: Vec3, dt: Float, camera: Camera): (Camera, Boolean, String) = {
    var modifiedCamera = camera
    var checkpointReached = false
    var checkpointMessage = ""
    
    if (!gameStarted) {
      countdownTime -= dt
      if (countdownTime <= 0) gameStarted = true
      return (modifiedCamera, false, "")
    }
    
    distance = playerPos.z
    
    // SLOWER speed increase: 0.01 instead of 0.02 (half the acceleration rate)
    baseSpeed = math.min(3f + distance * 0.01f, 18f)
    
    val timeMultiplier = if (slowTimeActive) {
      if (System.currentTimeMillis() > slowTimeEndTime) {
        slowTimeActive = false
        1f
      } else 0.5f
    } else 1f
    
    currentSpeed = baseSpeed * timeMultiplier
    
    if (shieldActive && System.currentTimeMillis() > shieldEndTime) {
      shieldActive = false
    }
    
    val checkpoint = (distance / 100).toInt * 100
    if (checkpoint > lastCheckpoint && checkpoint > 0) {
      lastCheckpoint = checkpoint
      health = math.min(100f, health + 20f)
      checkpointReached = true
      checkpointMessage = s"CHECKPOINT ${checkpoint}m! +20 HP"
      ParticleSystem.spawn(playerPos, 30, (0, 255, 0))
    }
    
    if (perfectSectionActive && distance - perfectSectionStart >= 50f) {
      perfectSectionStart = distance
      comboMultiplier = math.min(comboMultiplier + 0.5f, 5f)
      checkpointReached = true
      checkpointMessage = "PERFECT SECTION! Multiplier increased!"
      ParticleSystem.spawn(playerPos, 20, (255, 255, 0))
    }
    
    pickups = pickups.map { pickup =>
      if (!pickup.collected && (playerPos - pickup.position).length < 1.5f) {
        pickup.pickupType match {
          case HealthPickup =>
            health = math.min(100f, health + 25f)
            ParticleSystem.spawn(pickup.position, 15, (0, 255, 0))
          case ShieldPickup =>
            shieldActive = true
            shieldEndTime = System.currentTimeMillis() + 5000
            ParticleSystem.spawn(pickup.position, 15, (0, 150, 255))
          case SlowTimePickup =>
            slowTimeActive = true
            slowTimeEndTime = System.currentTimeMillis() + 3000
            ParticleSystem.spawn(pickup.position, 15, (255, 255, 0))
        }
        pickup.copy(collected = true)
      } else pickup
    }
    
    for (obstacle <- obstacles if (playerPos - obstacle.position).length < 1.2f) {
      if (!shieldActive) {
        health -= 30f
        damageShake = 0.5f
        perfectSectionActive = false
        comboMultiplier = 1f
        ParticleSystem.spawn(playerPos, 20, (255, 0, 0))
      }
    }
    
    val (distToWall, _) = RayMarcher.sceneSDF(playerPos)
    if (distToWall < 1.5f && !shieldActive) {
      val damageRate = (1.5f - distToWall) * 15f
      health -= damageRate * dt
      lastDamageTime = System.currentTimeMillis()
      perfectSectionActive = false
      comboMultiplier = 1f
      comboTime = 0f
      damageShake = math.max(damageShake, 0.2f)
    } else {
      comboTime += dt
      if (comboTime >= 10f && comboMultiplier < 2f) {
        comboMultiplier = 2f
        ParticleSystem.spawn(playerPos, 10, (255, 200, 0))
      }
    }
    
    if (damageShake > 0) {
      val rng = new scala.util.Random()
      val shakeX = (rng.nextFloat() - 0.5f) * damageShake
      val shakeY = (rng.nextFloat() - 0.5f) * damageShake
      modifiedCamera = modifiedCamera.copy(
        position = modifiedCamera.position + Vec3(shakeX, shakeY, 0)
      )
      damageShake -= dt * 2f
    }
    
    if (health <= 0f) {
      health = 0f
      isAlive = false
      val finalDist = (distance * comboMultiplier).toInt
      if (finalDist > bestDistance) newRecord = true
      saveScore(finalDist)
    }
    
    ParticleSystem.update(dt)
    
    (modifiedCamera, checkpointReached, checkpointMessage)
  }
  
  def getActivePickups(): List[Pickup] = {
    pickups.filter(p => !p.collected && p.position.z > distance - 20 && p.position.z < distance + 60)
  }
  
  def getActiveObstacles(): List[Obstacle] = {
    obstacles.filter(o => o.position.z > distance - 20 && o.position.z < distance + 60)
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
  
  def getCurrentRank(): Int = {
    val currentScore = (distance * comboMultiplier).toInt
    val scores = getHighScores()
    scores.count(_._2 > currentScore) + 1
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
  
  def getDistanceToNextCheckpoint(): Int = {
    val nextCheckpoint = ((distance / 100).toInt + 1) * 100
    (nextCheckpoint - distance).toInt
  }
}
