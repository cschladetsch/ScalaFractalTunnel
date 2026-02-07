```markdown
# Descent 2 Clone - Ray Marching Implementation in Scala

## Project Overview
A minimal 6DOF space shooter inspired by Descent 2, implemented using ray marching/sphere tracing techniques instead of traditional polygon rendering. Target: 640x480, 30-60 FPS on JVM, playable demo in 48 hours.

## Technical Approach

### Rendering: Ray Marching with SDFs
- Pure CPU ray marching (no GPU dependencies for simplicity)
- Signed Distance Functions (SDFs) define all geometry
- Per-pixel ray casting from camera through inverse projection
- Distance-based fog for depth perception
- Phong lighting calculated from SDF normals

### Platform
- Scala 3.x (or 2.13 if needed for compatibility)
- Java AWT/Swing for window and pixel buffer (BufferedImage)
- No external game libraries (minimal dependencies)
- Target resolution: 640x480

## Core Systems

### 1. Mathematics Foundation

**Vec3 class:**
```scala
case class Vec3(x: Float, y: Float, z: Float) {
  def +(other: Vec3): Vec3
  def -(other: Vec3): Vec3
  def *(scalar: Float): Vec3
  def dot(other: Vec3): Float
  def cross(other: Vec3): Vec3
  def length: Float
  def normalized: Vec3
}
```

**Mat4 class:**
- 4x4 transformation matrices
- Support translation, rotation (pitch/yaw/roll), perspective projection
- Matrix multiplication, inverse (for camera ray generation)

**Quaternion (optional but recommended for 6DOF):**
- Smooth rotation without gimbal lock
- Convert to rotation matrix for ray generation

### 2. Ray Marching Engine

**Core algorithm:**
```scala
def raymarch(origin: Vec3, direction: Vec3, maxDist: Float = 100f, maxSteps: Int = 100): Option[Hit] = {
  var t = 0f
  var steps = 0
  
  while (t < maxDist && steps < maxSteps) {
    val p = origin + direction * t
    val d = sceneSDF(p)
    
    if (d < EPSILON) {
      return Some(Hit(p, t, steps))
    }
    
    t += d * 0.9f  // slight dampening for stability
    steps += 1
  }
  
  None  // ray missed
}
```

**SDF primitives needed:**
- `sphereSDF(p: Vec3, center: Vec3, radius: Float): Float`
- `boxSDF(p: Vec3, center: Vec3, size: Vec3): Float`
- `cylinderSDF(p: Vec3, radius: Float): Float` (infinite along Z)
- `capsuleSDF(p: Vec3, a: Vec3, b: Vec3, radius: Float): Float` (for missiles)

**SDF operations:**
- `union(d1: Float, d2: Float): Float = math.min(d1, d2)`
- `subtraction(d1: Float, d2: Float): Float = math.max(d1, -d2)`
- `intersection(d1: Float, d2: Float): Float = math.max(d1, d2)`

**Lighting:**
```scala
def getNormal(p: Vec3, epsilon: Float = 0.001f): Vec3 = {
  val dx = sceneSDF(p + Vec3(epsilon, 0, 0)) - sceneSDF(p - Vec3(epsilon, 0, 0))
  val dy = sceneSDF(p + Vec3(0, epsilon, 0)) - sceneSDF(p - Vec3(0, epsilon, 0))
  val dz = sceneSDF(p + Vec3(0, 0, epsilon)) - sceneSDF(p - Vec3(0, 0, epsilon))
  Vec3(dx, dy, dz).normalized
}

def shade(hitPoint: Vec3, normal: Vec3, viewDir: Vec3): Color = {
  val lightDir = Vec3(0.3f, -0.5f, -0.8f).normalized  // directional light
  val diffuse = math.max(0f, normal.dot(lightDir))
  
  val ambient = 0.2f
  val intensity = ambient + diffuse * 0.8f
  
  // Color based on material (can check object ID)
  Color((intensity * 255).toInt, (intensity * 200).toInt, (intensity * 180).toInt)
}
```

### 3. Scene Definition

**Map: Simple Mine Tunnel**
```scala
def sceneSDF(p: Vec3): Float = {
  // Main tunnel - cylinder along Z axis, radius 8 units
  val tunnelRadius = 8f
  val tunnel = math.sqrt(p.x*p.x + p.y*p.y).toFloat - tunnelRadius
  
  // Walls at segments (every 20 units)
  val wallThickness = 0.5f
  val wallSpacing = 20f
  val wallZ = (p.z / wallSpacing).floor * wallSpacing
  val wall = boxSDF(p, Vec3(0, 0, wallZ + wallSpacing/2), Vec3(7, 7, wallThickness))
  
  // Doorway in each wall (4x4 opening)
  val doorway = boxSDF(p, Vec3(0, 0, wallZ + wallSpacing/2), Vec3(2, 2, wallThickness + 0.1f))
  val wallWithDoor = subtraction(wall, doorway)
  
  // Random obstacles (boxes) - deterministic based on Z position
  val obstacleId = (p.z / 10f).floor.toInt
  val rng = new scala.util.Random(obstacleId)
  val obsX = (rng.nextFloat() - 0.5f) * 6f
  val obsY = (rng.nextFloat() - 0.5f) * 6f
  val obsZ = obstacleId * 10f + 5f
  val obstacle = boxSDF(p, Vec3(obsX, obsY, obsZ), Vec3(1, 1, 1))
  
  union(tunnel, union(wallWithDoor, obstacle))
}
```

**Material/Object IDs:**
Modify `sceneSDF` to return a tuple `(Float, Int)` where Int is object type:
- 0: tunnel walls
- 1: obstacles
- 2: enemies
- 3: projectiles

### 4. 6DOF Camera System

**Camera state:**
```scala
case class Camera(
  position: Vec3,
  orientation: Quaternion,  // or pitch/yaw/roll floats
  fov: Float = 90f
) {
  def forward: Vec3 = orientation.rotate(Vec3(0, 0, 1))
  def right: Vec3 = orientation.rotate(Vec3(1, 0, 0))
  def up: Vec3 = orientation.rotate(Vec3(0, 1, 0))
  
  def getRayDirection(screenX: Int, screenY: Int, screenWidth: Int, screenHeight: Int): Vec3 = {
    val aspect = screenWidth.toFloat / screenHeight
    val fovRad = math.toRadians(fov).toFloat
    
    // NDC coordinates [-1, 1]
    val ndcX = (2f * screenX / screenWidth - 1f) * aspect * math.tan(fovRad / 2).toFloat
    val ndcY = (1f - 2f * screenY / screenHeight) * math.tan(fovRad / 2).toFloat
    
    // Ray in camera space, then rotate to world space
    val rayDir = Vec3(ndcX, ndcY, 1f).normalized
    orientation.rotate(rayDir)
  }
}
```

**Input handling:**
```scala
// Per-frame update
def updateCamera(camera: Camera, input: InputState, dt: Float): Camera = {
  val moveSpeed = 10f * dt
  val rotSpeed = 2f * dt
  
  var newPos = camera.position
  var newOrient = camera.orientation
  
  // Translation
  if (input.forward) newPos += camera.forward * moveSpeed
  if (input.back) newPos -= camera.forward * moveSpeed
  if (input.left) newPos -= camera.right * moveSpeed
  if (input.right) newPos += camera.right * moveSpeed
  if (input.up) newPos += camera.up * moveSpeed
  if (input.down) newPos -= camera.up * moveSpeed
  
  // Rotation (mouse delta or keys)
  val pitch = input.mouseDY * rotSpeed
  val yaw = input.mouseDX * rotSpeed
  val roll = (if (input.rollLeft) -rotSpeed else 0f) + (if (input.rollRight) rotSpeed else 0f)
  
  newOrient = newOrient.rotate(pitch, yaw, roll)
  
  camera.copy(position = newPos, orientation = newOrient)
}
```

### 5. Enemy System

**Enemy type: Hovering Mine**
```scala
case class Enemy(
  id: Int,
  position: Vec3,
  velocity: Vec3,
  health: Int = 100,
  lastShotTime: Long = 0,
  state: EnemyState = Patrol
)

sealed trait EnemyState
case object Patrol extends EnemyState
case object Tracking extends EnemyState
case object Attacking extends EnemyState
```

**AI behavior:**
```scala
def updateEnemy(enemy: Enemy, playerPos: Vec3, currentTime: Long, dt: Float): Enemy = {
  val toPlayer = playerPos - enemy.position
  val distance = toPlayer.length
  
  val newState = distance match {
    case d if d < 15f => Attacking
    case d if d < 30f => Tracking
    case _ => Patrol
  }
  
  val newVelocity = newState match {
    case Patrol =>
      // Slow drift in a circle
      val orbit = Vec3(
        math.sin(currentTime * 0.001).toFloat * 2f,
        math.cos(currentTime * 0.001).toFloat * 2f,
        0f
      )
      orbit
      
    case Tracking =>
      // Move toward player slowly
      toPlayer.normalized * 3f
      
    case Attacking =>
      // Strafe around player
      val strafe = toPlayer.cross(Vec3(0, 0, 1)).normalized
      strafe * 4f
  }
  
  val newPosition = enemy.position + newVelocity * dt
  
  // Check if should fire
  val shouldFire = newState == Attacking && 
                   distance < 20f && 
                   currentTime - enemy.lastShotTime > 1000
  
  enemy.copy(
    position = newPosition,
    velocity = newVelocity,
    state = newState,
    lastShotTime = if (shouldFire) currentTime else enemy.lastShotTime
  )
}
```

**Enemy SDF contribution:**
```scala
def enemySDF(p: Vec3, enemies: List[Enemy]): Float = {
  enemies.map(e => sphereSDF(p, e.position, 1.5f)).minOption.getOrElse(Float.MaxValue)
}
```

### 6. Projectile System

**Missile definition:**
```scala
case class Projectile(
  id: Int,
  position: Vec3,
  velocity: Vec3,
  lifetime: Float = 5f,  // seconds
  fromPlayer: Boolean = false
)
```

**Physics:**
```scala
def updateProjectile(proj: Projectile, dt: Float): Option[Projectile] = {
  val newPos = proj.position + proj.velocity * dt
  val newLifetime = proj.lifetime - dt
  
  if (newLifetime <= 0) None
  else Some(proj.copy(position = newPos, lifetime = newLifetime))
}
```

**Collision detection:**
```scala
def checkProjectileCollisions(
  projectiles: List[Projectile],
  enemies: List[Enemy],
  playerPos: Vec3
): (List[Projectile], List[Enemy], Boolean) = {
  
  var activeProj = projectiles
  var activeEnemies = enemies
  var playerHit = false
  
  for (proj <- projectiles) {
    // Check enemy hits
    for (enemy <- activeEnemies) {
      if ((proj.position - enemy.position).length < 2f && proj.fromPlayer) {
        // Remove projectile and damage enemy
        activeProj = activeProj.filterNot(_.id == proj.id)
        val damaged = enemy.copy(health = enemy.health - 50)
        if (damaged.health <= 0) {
          activeEnemies = activeEnemies.filterNot(_.id == enemy.id)
        } else {
          activeEnemies = activeEnemies.map(e => if (e.id == enemy.id) damaged else e)
        }
      }
    }
    
    // Check player hit
    if (!proj.fromPlayer && (proj.position - playerPos).length < 1.5f) {
      playerHit = true
      activeProj = activeProj.filterNot(_.id == proj.id)
    }
    
    // Check wall collision (if sceneSDF returns < 0.5)
    if (sceneSDF(proj.position) < 0.5f) {
      activeProj = activeProj.filterNot(_.id == proj.id)
    }
  }
  
  (activeProj, activeEnemies, playerHit)
}
```

**Projectile SDF:**
```scala
def projectileSDF(p: Vec3, projectiles: List[Projectile]): Float = {
  projectiles.map { proj =>
    sphereSDF(p, proj.position, 0.3f)  // Small glowing sphere
  }.minOption.getOrElse(Float.MaxValue)
}
```

**Firing mechanics:**
```scala
def fireProjectile(
  from: Vec3,
  direction: Vec3,
  fromPlayer: Boolean,
  nextId: Int
): Projectile = {
  Projectile(
    id = nextId,
    position = from + direction * 2f,  // spawn slightly ahead
    velocity = direction.normalized * 20f,  // fast moving
    fromPlayer = fromPlayer
  )
}
```

### 7. Game State Management

```scala
case class GameState(
  camera: Camera,
  enemies: List[Enemy],
  projectiles: List[Projectile],
  playerHealth: Int = 100,
  score: Int = 0,
  nextEntityId: Int = 0,
  time: Long = System.currentTimeMillis()
)

def update(state: GameState, input: InputState, dt: Float): GameState = {
  val currentTime = System.currentTimeMillis()
  
  // Update camera
  val newCamera = updateCamera(state.camera, input, dt)
  
  // Update enemies
  val updatedEnemies = state.enemies.map(e => 
    updateEnemy(e, newCamera.position, currentTime, dt)
  )
  
  // Spawn enemy projectiles
  val newEnemyProjectiles = updatedEnemies.flatMap { enemy =>
    if (currentTime - enemy.lastShotTime < 50) {  // just fired
      val direction = (newCamera.position - enemy.position).normalized
      Some(fireProjectile(enemy.position, direction, false, state.nextEntityId))
    } else None
  }
  
  // Player firing
  val newPlayerProjectiles = if (input.fire && currentTime - state.time > 200) {
    List(fireProjectile(newCamera.position, newCamera.forward, true, state.nextEntityId + newEnemyProjectiles.size))
  } else List.empty
  
  // Update projectiles
  val updatedProjectiles = (state.projectiles ++ newEnemyProjectiles ++ newPlayerProjectiles)
    .flatMap(p => updateProjectile(p, dt))
  
  // Collision detection
  val (finalProjectiles, finalEnemies, playerWasHit) = 
    checkProjectileCollisions(updatedProjectiles, updatedEnemies, newCamera.position)
  
  val newHealth = if (playerWasHit) state.playerHealth - 20 else state.playerHealth
  val enemiesKilled = updatedEnemies.size - finalEnemies.size
  val newScore = state.score + enemiesKilled * 100
  
  state.copy(
    camera = newCamera,
    enemies = finalEnemies,
    projectiles = finalProjectiles,
    playerHealth = newHealth,
    score = newScore,
    nextEntityId = state.nextEntityId + newEnemyProjectiles.size + newPlayerProjectiles.size,
    time = currentTime
  )
}
```

### 8. Rendering Pipeline

```scala
def render(state: GameState, width: Int, height: Int): BufferedImage = {
  val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
  
  for (y <- 0 until height; x <- 0 until width) {
    val rayDir = state.camera.getRayDirection(x, y, width, height)
    
    raymarch(state.camera.position, rayDir) match {
      case Some(hit) =>
        val normal = getNormal(hit.point)
        val color = shade(hit.point, normal, -rayDir)
        
        // Fog based on distance
        val fogAmount = math.min(1f, hit.distance / 50f)
        val fogColor = Color.BLACK
        val finalColor = interpolateColor(color, fogColor, fogAmount)
        
        image.setRGB(x, y, finalColor.getRGB)
        
      case None =>
        // Sky/background
        image.setRGB(x, y, Color.BLACK.getRGB)
    }
  }
  
  // Render HUD overlay
  val g = image.getGraphics
  g.setColor(Color.GREEN)
  g.drawString(s"Health: ${state.playerHealth}", 10, 20)
  g.drawString(s"Score: ${state.score}", 10, 40)
  g.drawString(s"Enemies: ${state.enemies.size}", 10, 60)
  g.dispose()
  
  image
}
```

**Combined scene SDF:**
```scala
def sceneSDF(p: Vec3, state: GameState): (Float, Int) = {
  val tunnel = tunnelSDF(p)
  val enemies = enemySDF(p, state.enemies)
  val projectiles = projectileSDF(p, state.projectiles)
  
  val minDist = math.min(tunnel, math.min(enemies, projectiles))
  
  val objType = minDist match {
    case d if d == tunnel => 0
    case d if d == enemies => 2
    case d if d == projectiles => 3
    case _ => 0
  }
  
  (minDist, objType)
}
```

## Implementation Phases

### Phase 1: Core Ray Marcher (4-6 hours)
1. Vec3, Mat4 math library
2. Basic ray marching loop
3. Simple sphere SDF
4. Render to BufferedImage in JFrame
5. Verify rendering works

### Phase 2: Camera & Input (2-3 hours)
1. Camera class with position/orientation
2. Keyboard input handling (WASD, QE for up/down, ZC for roll)
3. Mouse look (optional, can use keys)
4. Movement integration

### Phase 3: Tunnel Geometry (2-3 hours)
1. Cylinder SDF for tunnel
2. Box SDFs for walls with doorways
3. Random obstacles
4. Lighting and normals
5. Distance fog

### Phase 4: Enemy & Combat (3-4 hours)
1. Enemy entity with AI states
2. Enemy SDF rendering
3. Projectile system
4. Collision detection
5. Health/score tracking

### Phase 5: Polish (2-3 hours)
1. HUD rendering
2. Performance optimization (multi-threading per scanline?)
3. Tuning (movement speed, enemy behavior, colors)
4. Win/lose conditions

## Performance Considerations

- **640x480 = 307,200 rays per frame**
- **Target 30 FPS = ~10,000 rays/ms**
- Each ray ~50-100 SDF evaluations average
- **~30-60M SDF calls per second**

Optimizations:
1. Early ray termination
2. Reduce max steps (100 -> 64)
3. Adaptive epsilon based on distance
4. Parallel scanline rendering (Scala parallel collections)
5. Fast math (avoid sqrt where possible)
6. Bounding volumes for enemies/projectiles

## Controls

- **W/S**: Forward/Back
- **A/D**: Strafe Left/Right
- **Q/E**: Up/Down
- **Z/C**: Roll Left/Right
- **Mouse**: Pitch/Yaw (or arrow keys)
- **Space**: Fire
- **ESC**: Quit

## Victory Conditions

- Destroy all enemies in tunnel segment
- Reach end of tunnel (Z > 100)
- Simple "You Win" overlay

## File Structure

```
descent-clone/
├── build.sbt
├── src/main/scala/
│   ├── Main.scala              // Entry point, game loop
│   ├── math/
│   │   ├── Vec3.scala
│   │   ├── Mat4.scala
│   │   └── Quaternion.scala
│   ├── rendering/
│   │   ├── RayMarcher.scala
│   │   ├── SDF.scala           // All SDF primitives
│   │   └── Shader.scala
│   ├── entities/
│   │   ├── Camera.scala
│   │   ├── Enemy.scala
│   │   └── Projectile.scala
│   ├── GameState.scala
│   └── InputHandler.scala
```

## Extensions (If Time Permits)

- Multiple enemy types
- Power-ups (health, weapons)
- Sound effects (Java Sound API)
- Mini-map
- Particle effects for explosions
- Better tunnel generation (curved segments)
- Save high scores

## Demo Script for Interview

1. Show tunnel fly-through (no enemies)
2. Demonstrate 6DOF movement (pitch, yaw, roll, translation)
3. Spawn enemy, show AI behavior
4. Combat demonstration
5. Discuss ray marching technique
6. Show code structure (SDF composition, parallel rendering)
7. Performance metrics (FPS, ray count)

## Key Talking Points

- "Chose ray marching over polygon rendering for mathematical elegance"
- "SDFs compose naturally - union, subtraction, intersection"
- "JVM performance challenges - discuss GC tuning, allocation avoidance"
- "Functional approach to entity updates (immutable state)"
- "Parallelize per-scanline for multi-core utilization"
- "6DOF quaternions avoid gimbal lock"
```
