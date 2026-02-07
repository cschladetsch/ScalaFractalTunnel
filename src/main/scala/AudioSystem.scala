import scala.util.{Try, Success, Failure}

object AudioSystem {
  private var initialized = false
  
  def init(): Unit = {
    Try {
      InteractiveMusicSystem.init()
      initialized = true
    } match {
      case Success(_) => println("Audio system initialized")
      case Failure(e) => println(s"Audio not available: ${e.getMessage}")
    }
  }
  
  def stop(): Unit = {
    if (initialized) {
      InteractiveMusicSystem.stop()
    }
  }
  
  def updatePosition(pos: Vec3): Unit = {
    if (initialized) {
      val wallDist = RayMarcher.sceneSDF(pos)._1
      InteractiveMusicSystem.update(
        GameState.currentSpeed,
        GameState.distance,
        GameState.health,
        wallDist
      )
    }
  }
  
  def onCheckpoint(): Unit = {
    if (initialized) InteractiveMusicSystem.onCheckpoint()
  }
  
  def onDamage(): Unit = {
    if (initialized) InteractiveMusicSystem.onDamage()
  }
  
  def onCombo(): Unit = {
    if (initialized) InteractiveMusicSystem.onCombo()
  }
  
  def onPickup(): Unit = {
    if (initialized) InteractiveMusicSystem.onPickup()
  }
}
