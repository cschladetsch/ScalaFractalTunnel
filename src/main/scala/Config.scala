import scala.io.Source

object Config {
  private val lines = Source.fromFile("config.json").getLines().mkString
  
  private def getNumber(key: String): Float = {
    val pattern = s""""$key"\\s*:\\s*([0-9.]+)""".r
    pattern.findFirstMatchIn(lines).map(_.group(1).toFloat).getOrElse(0f)
  }
  
  private def getInt(key: String): Int = getNumber(key).toInt
  
  // Game
  val startHealth = getNumber("startHealth")
  val startSpeed = getNumber("startSpeed")
  val maxSpeed = getNumber("maxSpeed")
  val speedAcceleration = getNumber("speedAcceleration")
  val countdownTime = getNumber("countdownTime")
  val checkpointInterval = getInt("checkpointInterval")
  val checkpointHealthBonus = getNumber("checkpointHealthBonus")
  val perfectSectionDistance = getNumber("perfectSectionDistance")
  val comboTimeThreshold = getNumber("comboTimeThreshold")
  val comboMultiplierMax = getNumber("comboMultiplierMax")
  val comboMultiplierIncrement = getNumber("comboMultiplierIncrement")
  
  // Damage
  val wallDamageRate = getNumber("wallDamageRate")
  val wallDamageDistance = getNumber("wallDamageDistance")
  val obstacleDamage = getNumber("obstacleDamage")
  val obstacleRadius = getNumber("obstacleRadius")
  val shakeIntensity = getNumber("shakeIntensity")
  val shakeDecayRate = getNumber("shakeDecayRate")
  
  // Pickups
  val healthRestore = getNumber("healthRestore")
  val shieldDuration = getNumber("shieldDuration").toLong
  val slowTimeDuration = getNumber("slowTimeDuration").toLong
  val slowTimeMultiplier = getNumber("slowTimeMultiplier")
  val collectionRadius = getNumber("collectionRadius")
  val spawnInterval = getInt("spawnInterval")
  val spawnStartDistance = getInt("spawnStartDistance")
  val spawnEndDistance = getInt("spawnEndDistance")
  val healthSpawnChance = getNumber("healthSpawnChance")
  val shieldSpawnChance = getNumber("shieldSpawnChance")
  val obstacleStartDistance = getInt("obstacleStartDistance")
  val obstacleSpawnChance = getNumber("obstacleSpawnChance")
  
  // Ray marching
  val epsilon = getNumber("epsilon")
  val maxDistance = getNumber("maxDistance")
  val maxSteps = getInt("maxSteps")
  val startOffset = getNumber("startOffset")
  val stepMultiplier = getNumber("stepMultiplier")
  
  // Tunnel
  val curveFreqX = getNumber("curveFrequencyX")
  val curveFreqY = getNumber("curveFrequencyY")
  val curveAmpX = getNumber("curveAmplitudeX")
  val curveAmpY = getNumber("curveAmplitudeY")
  val baseRadius = getNumber("baseRadius")
  val radiusWave1Freq = getNumber("radiusWave1Freq")
  val radiusWave1Amp = getNumber("radiusWave1Amp")
  val radiusWave2Freq = getNumber("radiusWave2Freq")
  val radiusWave2Amp = getNumber("radiusWave2Amp")
  val radiusWave3Freq = getNumber("radiusWave3Freq")
  val radiusWave3Amp = getNumber("radiusWave3Amp")
  
  val fractalFreqs = Array(
    getInt("fractalFreq1"),
    getInt("fractalFreq2"),
    getInt("fractalFreq3"),
    getInt("fractalFreq4"),
    getInt("fractalFreq5")
  )
  
  val fractalAmps = Array(
    getNumber("fractalAmp1"),
    getNumber("fractalAmp2"),
    getNumber("fractalAmp3"),
    getNumber("fractalAmp4"),
    getNumber("fractalAmp5")
  )
  
  val fractalPhasesZ = Array(
    getNumber("fractalPhase1Z"),
    getNumber("fractalPhase2Z"),
    getNumber("fractalPhase3Z"),
    getNumber("fractalPhase4Z"),
    getNumber("fractalPhase5Z")
  )
  
  // Rendering
  val renderWidth = getInt("renderWidth")
  val renderHeight = getInt("renderHeight")
  val displayWidth = getInt("displayWidth")
  val displayHeight = getInt("displayHeight")
  val fov = getNumber("fov")
  val colorPhaseMultiplier = getNumber("colorPhaseMultiplier")
  
  // Movement
  val strafeSpeed = getNumber("strafeSpeed")
  val rotationSpeed = getNumber("rotationSpeed")
  val collisionRadius = getNumber("collisionRadius")
  val slideMultiplier = getNumber("slideMultiplier")
  val pushbackDistance = getNumber("pushbackDistance")
  val recenterSpeed = getNumber("recenterSpeed")
  
  // Particles
  val particleCounts = Map(
    "checkpoint" -> getInt("checkpointCount"),
    "perfectSection" -> getInt("perfectSectionCount"),
    "healthPickup" -> getInt("healthPickupCount"),
    "shieldPickup" -> getInt("shieldPickupCount"),
    "slowTimePickup" -> getInt("slowTimePickupCount"),
    "damage" -> getInt("damageCount"),
    "combo" -> getInt("comboCount")
  )
  
  val particleLifetime = getNumber("lifetime")
  val particleMinSpeed = getNumber("minSpeed")
  val particleMaxSpeed = getNumber("maxSpeed")
}
