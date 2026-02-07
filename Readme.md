# Scala - Fractal Tunnel Runner

An endless runner game through procedurally-generated fractal tunnels, built entirely with ray marching. Created as a technical demo showcasing real-time SDF (Signed Distance Field) rendering in Scala 3.

## Demo

I cannot play well:

![Demo](/resources/Demo1.gif)

## What Makes This Different

Unlike traditional polygon-based 3D games, this uses **pure mathematical ray marching** to render impossible geometry:

- **No meshes or triangles** - everything is defined by distance functions
- **True fractals** - multi-octave displacement creates organic, infinitely-detailed surfaces
- **Morphing geometry** - tunnel radius and shape vary procedurally with depth
- **Ray-marched effects** - all visuals computed per-pixel through sphere tracing

## Features

### Gameplay
- **Progressive difficulty** - speed increases from 3 to 18 units/sec
- **Combo multiplier** - stay centered for 10+ seconds to earn 2x distance
- **Three pickup types:**
  - 🟢 Health (+25 HP)
  - 🔵 Shield (5 sec invincibility)
  - 🟡 Slow Time (3 sec slow motion)
- **Checkpoints** - every 100m restores 20 HP
- **Obstacles** - red hazards deal 30 damage on contact
- **Perfect section bonuses** - navigate 50m without damage for multiplier boost

### Visual Polish
- **Speed lines** - motion blur intensifies at high velocity
- **Color shifting** - tunnel hues shift blue→purple→red with speed
- **Particle effects** - burst effects on pickups, damage, and checkpoints
- **Screen shake** - dynamic camera shake on collision
- **Health vignette** - red border pulses when critically wounded
- **Countdown timer** - "3-2-1-GO!" game start

### Meta Features
- **Live leaderboard** - see your current rank during play
- **High score tracking** - top 10 saved to disk
- **"NEW RECORD!"** celebration on personal best
- **Distance to checkpoint** indicator
- **Quick restart** - press R to retry immediately

## Technical Implementation

### Ray Marching Engine
```scala
def march(origin: Vec3, direction: Vec3): Option[Hit] = {
  var t = 0.1f
  var steps = 0
  
  while (t < MAX_DIST && steps < MAX_STEPS) {
    val p = origin + direction * t
    val (d, mat) = sceneSDF(p)
    
    if (d < EPSILON) return Some(Hit(p, t, steps, mat))
    t += d
    steps += 1
  }
  None
}
```

### Fractal Surface Displacement
5-octave multi-frequency displacement creates organic tunnel surfaces:
```scala
val freq1 = sin(angle * 8 + z * 0.6 + time * 0.3) * 1.0
val freq2 = sin(angle * 16 - z * 1.2) * 0.5
val freq3 = cos(angle * 32 + z * 2.4) * 0.25
val freq4 = sin(angle * 64 + z * 4.8) * 0.125
val freq5 = cos(angle * 128 - z * 9.6) * 0.0625
```

### Curved Tunnel Path
Tunnel centerline follows serpentine 3D curve:
```scala
def getTunnelCenter(z: Float): Vec3 = {
  val x = sin(z * 0.15) * 6
  val y = cos(z * 0.12) * 5
  Vec3(x, y, z)
}
```

### Architecture
- **Vec3.scala** - 3D vector math (dot, cross, normalize)
- **Camera.scala** - 6DOF camera with pitch/yaw/roll
- **RayMarcher.scala** - Core ray marching + SDF scene definition
- **RayRenderer.scala** - Renders image buffer from camera
- **GameState.scala** - Game logic, pickups, obstacles, scoring
- **InputHandler.scala** - Keyboard input + collision sliding
- **Particle.scala** - Particle system for visual effects
- **AudioSystem.scala** - Procedural audio (optional in WSL)

## Requirements

- **Java 21+** (OpenJDK recommended)
- **SBT 1.12+**
- **X11 display** (for WSL: WSLg or VcXsrv)

## Running
```bash
git clone <repo>
cd ScalaDescent
sbt run
```

Performance note: Renders at 213×160 scaled to 640×480 for ~30 FPS on modest hardware.

## Controls

### Movement
- **W/S** - Boost forward / Brake
- **A/D** - Strafe left/right
- **R/F** - Move up/down
- **Arrow Keys** - Pitch/Yaw (look around)
- **Q/E** - Roll (bank turns)
- **SPACE** - Recenter to tunnel axis

### Meta
- **R** (on game over) - Restart

## Gameplay Tips

1. **Stay centered** - Build combo multiplier by avoiding walls
2. **Collect shields early** - Save them for narrow sections
3. **Use slow-time strategically** - Best during high-speed narrow gaps
4. **Watch checkpoint countdown** - Plan health pickups around it
5. **Perfect sections matter** - 50m clean = permanent multiplier increase

## Technical Highlights

- **No external 3D libraries** - pure mathematical rendering
- **Collision with surface sliding** - allows glancing wall contact
- **Dynamic speed ramping** - smooth acceleration curve
- **Material-based rendering** - different visual treatment per object type
- **Procedural audio synthesis** - position-reactive sound (when available)

## Performance Optimizations

- **Adaptive step size** in ray marching (0.9× damping)
- **Early culling** of distant objects (60 unit max distance)
- **Step count limit** (64 max iterations)
- **Reduced render resolution** with scaling
- **Cached enemy positions** for faster SDF queries

## Development Context

Built in ~12 hours as a Scala technical demo showcasing:
- Functional programming patterns (immutable state, case classes)
- Real-time graphics algorithms
- Game loop architecture
- Mathematical/algorithmic problem solving

The ray marching approach was chosen specifically to demonstrate algorithmic depth beyond typical polygon rendering.

Purposfuly used software only (no GPU, CUDA etc).

## Future Ideas

- Branching tunnel paths (forks/choices)
- Enemy AI entities
- Multiplayer ghost racing
- Procedural music generation
- VR support
- CUDA acceleration for 4K rendering

## License

MIT

## Credits

Ray marching technique inspired by Íñigo Quílez's work on ShaderToy.
Built entirely during a late-night coding session fueled by determination and coffee.

EOF
