import java.awt.event.{KeyAdapter, KeyEvent}
import scala.collection.mutable

class InputHandler extends KeyAdapter {
  private val keys = mutable.Set[Int]()
  private var lastFireTime = 0L
  
  override def keyPressed(e: KeyEvent): Unit = {
    keys += e.getKeyCode
  }
  
  override def keyReleased(e: KeyEvent): Unit = {
    keys -= e.getKeyCode
  }
  
  def isPressed(keyCode: Int): Boolean = keys.contains(keyCode)
  
  def updateCamera(camera: Camera, dt: Float): Camera = {
    val moveSpeed = 5f * dt
    val rotSpeed = 1.5f * dt
    
    var pos = camera.position
    var pitch = camera.pitch
    var yaw = camera.yaw
    var roll = camera.roll
    
    var newPos = pos
    
    // Movement in local frame
    if (isPressed(KeyEvent.VK_W)) newPos = tryMove(newPos, camera.forward * moveSpeed)
    if (isPressed(KeyEvent.VK_S)) newPos = tryMove(newPos, camera.forward * -moveSpeed)
    if (isPressed(KeyEvent.VK_A)) newPos = tryMove(newPos, camera.right * -moveSpeed)
    if (isPressed(KeyEvent.VK_D)) newPos = tryMove(newPos, camera.right * moveSpeed)
    if (isPressed(KeyEvent.VK_R)) newPos = tryMove(newPos, camera.up * moveSpeed)      // Up in local frame
    if (isPressed(KeyEvent.VK_F)) newPos = tryMove(newPos, camera.up * -moveSpeed)     // Down in local frame
    
    // Rotation
    if (isPressed(KeyEvent.VK_UP)) pitch -= rotSpeed
    if (isPressed(KeyEvent.VK_DOWN)) pitch += rotSpeed
    if (isPressed(KeyEvent.VK_LEFT)) yaw -= rotSpeed
    if (isPressed(KeyEvent.VK_RIGHT)) yaw += rotSpeed
    if (isPressed(KeyEvent.VK_Q)) roll -= rotSpeed
    if (isPressed(KeyEvent.VK_E)) roll += rotSpeed
    
    // Fire projectile (rate limited)
    if (isPressed(KeyEvent.VK_SPACE)) {
      val now = System.currentTimeMillis()
      if (now - lastFireTime > 250) {  // 250ms cooldown
        ProjectileSystem.fire(newPos, camera.forward)
        lastFireTime = now
      }
    }
    
    Camera(newPos, pitch, yaw, roll)
  }
  
  private def tryMove(currentPos: Vec3, delta: Vec3): Vec3 = {
    val newPos = currentPos + delta
    val collisionRadius = 0.15f
    
    val dist = RayMarcher.sceneSDF(newPos)._1
    
    if (dist > collisionRadius) {
      newPos
    } else {
      currentPos
    }
  }
}
