import java.awt.event.{KeyAdapter, KeyEvent}
import scala.collection.mutable

class InputHandler extends KeyAdapter {
  private val keys = mutable.Set[Int]()
  
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
    
    // Movement
    if (isPressed(KeyEvent.VK_W)) pos = pos + camera.forward * moveSpeed
    if (isPressed(KeyEvent.VK_S)) pos = pos - camera.forward * moveSpeed
    if (isPressed(KeyEvent.VK_A)) pos = pos - camera.right * moveSpeed
    if (isPressed(KeyEvent.VK_D)) pos = pos + camera.right * moveSpeed
    if (isPressed(KeyEvent.VK_SPACE)) pos = pos + camera.up * moveSpeed
    if (isPressed(KeyEvent.VK_SHIFT)) pos = pos - camera.up * moveSpeed
    
    // Rotation
    if (isPressed(KeyEvent.VK_UP)) pitch -= rotSpeed
    if (isPressed(KeyEvent.VK_DOWN)) pitch += rotSpeed
    if (isPressed(KeyEvent.VK_LEFT)) yaw -= rotSpeed
    if (isPressed(KeyEvent.VK_RIGHT)) yaw += rotSpeed
    if (isPressed(KeyEvent.VK_Q)) roll -= rotSpeed
    if (isPressed(KeyEvent.VK_E)) roll += rotSpeed
    
    Camera(pos, pitch, yaw, roll)
  }
}
