import scala.io.Source
import java.io.PrintWriter

sealed trait PickupType
case object HealthPickup extends PickupType
case object ShieldPickup extends PickupType
case object SlowTimePickup extends PickupType

case class Pickup(position: Vec3, pickupType: PickupType, collected: Boolean)
case class Obstacle(position: Vec3)

object GameState {
  var health = Config.startHealth
  var distance = 0f
  var isAlive = true
  var baseSpeed = Config.startSpeed
  var currentSpeed = Config.startSpeed
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
  var countdownTime = Config.countdownTime
  var gameStarted = false
  var showTitleScreen = true
  
  var damageShake = 0f
  
  def reset(): Unit = {
    health = Config.startHealth
    distance = 0f
    isAlive = true
    baseSpeed = Config.startSpeed
    currentSpeed = Config.startSpeed
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
    countdownTime = Config.countdownTime
    gameStarted = false
    showTitleScreen = false
    damageShake = 0f
    ParticleSystem.clear()
    
    bestDistance = getHighScores().headOption.map(_._2.toFloat).getOrElse(0f)
    
    spawnPickupsAndObstacles()
  }
  
  def spawnPickupsAndObstacles(): Unit = {
    pickups = List()
    obstacles = List()
    val rng = new scala.util.Random(12345)
    
    for (z <- Config.spawnStartDistance to Config.spawnEndDistance by Config.spawnInterval) {
      val center = RayMarcher.getTunnelCenter(z.toFloat)
      val angle = rng.nextFloat() * math.Pi.toFloat * 2f
      val radius = rng.nextFloat() * 3f + 2f
      
      val x = center.x + radius * math.cos(angle).toFloat
      val y = center.y + radius * math.sin(angle).toFloat
      
      val pickupType = rng.nextFloat() match {
        case f if f < Config.healthSpawnChance => HealthPickup
        case f if f < Config.shieldSpawnChance => ShieldPickup
        case _ => SlowTimePickup
      }
      
      pickups = Pickup(Vec3(x, y, z.toFloat), pickupType, false) :: pickups
      
      if (z > Config.obstacleStartDistance && rng.nextFloat() < Config.obstacleSpawnChance) {
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
    
    baseSpeed = math.min(Config.startSpeed + distance * Config.speedAcceleration, Config.maxSpeed)
    
    val timeMultiplier = if (slowTimeActive) {
      if (System.currentTimeMillis() > slowTimeEndTime) {
        slowTimeActive = false
        1f
      } else Config.slowTimeMultiplier
    } else 1f
    
    currentSpeed = baseSpeed * timeMultiplier
    
    if (shieldActive && System.currentTimeMillis() > shieldEndTime) {
      shieldActive = false
    }
    
    val checkpoint = (distance / Config.checkpointInterval).toInt * Config.checkpointInterval
    if (checkpoint > lastCheckpoint && checkpoint > 0) {
      lastCheckpoint = checkpoint
      health = math.min(Config.startHealth, health + Config.checkpointHealthBonus)
      checkpointReached = true
      checkpointMessage = s"CHECKPOINT ${checkpoint}m! +${Config.checkpointHealthBonus.toInt} HP"
      ParticleSystem.spawn(playerPos, Config.particleCounts("checkpoint"), (0, 255, 0))
      AudioSystem.onCheckpoint()
    }
    
    if (perfectSectionActive && distance - perfectSectionStart >= Config.perfectSectionDistance) {
      perfectSectionStart = distance
      comboMultiplier = math.min(comboMultiplier + Config.comboMultiplierIncrement, Config.comboMultiplierMax)
      checkpointReached = true
      checkpointMessage = "PERFECT SECTION! Multiplier increased!"
      ParticleSystem.spawn(playerPos, Config.particleCounts("perfectSection"), (255, 255, 0))
      AudioSystem.onCombo()
    }
    
    pickups = pickups.map { pickup =>
      if (!pickup.collected && (playerPos - pickup.position).length < Config.collectionRadius) {
        pickup.pickupType match {
          case HealthPickup =>
            health = math.min(Config.startHealth, health + Config.healthRestore)
            ParticleSystem.spawn(pickup.position, Config.particleCounts("healthPickup"), (0, 255, 0))
          case ShieldPickup =>
            shieldActive = true
            shieldEndTime = System.currentTimeMillis() + Config.shieldDuration
            ParticleSystem.spawn(pickup.position, Config.particleCounts("shieldPickup"), (0, 150, 255))
          case SlowTimePickup =>
            slowTimeActive = true
            slowTimeEndTime = System.currentTimeMillis() + Config.slowTimeDuration
            ParticleSystem.spawn(pickup.position, Config.particleCounts("slowTimePickup"), (255, 255, 0))
        }
        AudioSystem.onPickup()
        pickup.copy(collected = true)
      } else pickup
    }
    
    for (obstacle <- obstacles if (playerPos - obstacle.position).length < Config.obstacleRadius) {
      if (!shieldActive) {
        health -= Config.obstacleDamage
        damageShake = Config.shakeIntensity
        perfectSectionActive = false
        comboMultiplier = 1f
        ParticleSystem.spawn(playerPos, Config.particleCounts("damage"), (255, 0, 0))
        AudioSystem.onDamage()
      }
    }
    
    val (distToWall, _) = RayMarcher.sceneSDF(playerPos)
    if (distToWall < Config.wallDamageDistance && !shieldActive) {
      val damageRate = (Config.wallDamageDistance - distToWall) * Config.wallDamageRate
      health -= damageRate * dt
      lastDamageTime = System.currentTimeMillis()
      perfectSectionActive = false
      comboMultiplier = 1f
      comboTime = 0f
      damageShake = math.max(damageShake, Config.shakeIntensity * 0.4f)
      
      val timeSinceDamage = System.currentTimeMillis() - lastDamageTime
      if (timeSinceDamage > 200) {
        AudioSystem.onDamage()
        lastDamageTime = System.currentTimeMillis()
      }
    } else {
      comboTime += dt
      if (comboTime >= Config.comboTimeThreshold && comboMultiplier < 2f) {
        comboMultiplier = 2f
        ParticleSystem.spawn(playerPos, Config.particleCounts("combo"), (255, 200, 0))
        AudioSystem.onCombo()
      }
    }
    
    if (damageShake > 0) {
      val rng = new scala.util.Random()
      val shakeX = (rng.nextFloat() - 0.5f) * damageShake
      val shakeY = (rng.nextFloat() - 0.5f) * damageShake
      modifiedCamera = modifiedCamera.copy(
        position = modifiedCamera.position + Vec3(shakeX, shakeY, 0)
      )
      damageShake -= dt * Config.shakeDecayRate
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
    val nextCheckpoint = ((distance / Config.checkpointInterval).toInt + 1) * Config.checkpointInterval
    (nextCheckpoint - distance).toInt
  }
}
