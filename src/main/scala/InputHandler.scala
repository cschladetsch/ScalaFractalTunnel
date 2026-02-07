import java.awt.event.{KeyEvent, KeyListener}

class InputHandler extends KeyListener {
  var keys = Set[Int]()  // Made public
  private var lastShotTime = 0L
  private val shotCooldown = 250L
  
  val baseSpeed = 8f
  val strafeSpeed = 5f
  
  override def keyPressed(e: KeyEvent): Unit = {
    keys = keys + e.getKeyCode
  }
  
  override def keyReleased(e: KeyEvent): Unit = {
    keys = keys - e.getKeyCode
  }
  
  override def keyTyped(e: KeyEvent): Unit = {}
  
  def updateCamera(camera: Camera, dt: Float): Camera = {
    var newCamera = camera
    
    // SPACE = recenter
    if (keys.contains(KeyEvent.VK_SPACE)) {
      val tunnelCenter = RayMarcher.getTunnelCenter(camera.position.z)
      val toCenter = (tunnelCenter - camera.position) * 0.1f
      newCamera = newCamera.copy(position = camera.position + toCenter)
    }
    
    // Auto forward
    val forwardMove = camera.forward * baseSpeed * dt
    
    // Steering
    var strafeMove = Vec3(0, 0, 0)
    if (keys.contains(KeyEvent.VK_A)) strafeMove = strafeMove - camera.right * strafeSpeed * dt
    if (keys.contains(KeyEvent.VK_D)) strafeMove = strafeMove + camera.right * strafeSpeed * dt
    if (keys.contains(KeyEvent.VK_W)) strafeMove = strafeMove + camera.forward * strafeSpeed * dt
    if (keys.contains(KeyEvent.VK_S)) strafeMove = strafeMove - camera.forward * (strafeSpeed * 0.5f) * dt
    if (keys.contains(KeyEvent.VK_R)) strafeMove = strafeMove + camera.up * strafeSpeed * dt
    if (keys.contains(KeyEvent.VK_F)) strafeMove = strafeMove - camera.up * strafeSpeed * dt
    
    val testForward = tryMoveWithSlide(newCamera.position + forwardMove, newCamera.position)
    val finalPos = tryMoveWithSlide(testForward + strafeMove, testForward)
    
    newCamera = newCamera.copy(position = finalPos)
    
    // Rotation
    val rotSpeed = 1.5f * dt
    if (keys.contains(KeyEvent.VK_UP)) newCamera = newCamera.copy(pitch = newCamera.pitch + rotSpeed)
    if (keys.contains(KeyEvent.VK_DOWN)) newCamera = newCamera.copy(pitch = newCamera.pitch - rotSpeed)
    if (keys.contains(KeyEvent.VK_LEFT)) newCamera = newCamera.copy(yaw = newCamera.yaw + rotSpeed)
    if (keys.contains(KeyEvent.VK_RIGHT)) newCamera = newCamera.copy(yaw = newCamera.yaw - rotSpeed)
    if (keys.contains(KeyEvent.VK_Q)) newCamera = newCamera.copy(roll = newCamera.roll + rotSpeed)
    if (keys.contains(KeyEvent.VK_E)) newCamera = newCamera.copy(roll = newCamera.roll - rotSpeed)
    
    newCamera
  }
  
  def tryMoveWithSlide(newPos: Vec3, oldPos: Vec3): Vec3 = {
    val collisionRadius = 0.6f
    val (dist, _) = RayMarcher.sceneSDF(newPos)
    
    if (dist > collisionRadius) {
      newPos
    } else if (dist > 0.1f) {
      val normal = RayMarcher.getNormal(newPos)
      val moveDir = (newPos - oldPos)
      val slideVec = moveDir - normal * moveDir.dot(normal) * 1.2f
      val slidePos = oldPos + slideVec
      
      val (slideDist, _) = RayMarcher.sceneSDF(slidePos)
      if (slideDist > 0.3f) slidePos else oldPos
    } else {
      val normal = RayMarcher.getNormal(newPos)
      oldPos + normal * 0.1f
    }
  }
}
