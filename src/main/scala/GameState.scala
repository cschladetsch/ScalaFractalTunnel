import scala.io.Source
import java.io.PrintWriter

object GameState {
  var health = 100f
  var distance = 0f
  var isAlive = true
  
  def reset(): Unit = {
    health = 100f
    distance = 0f
    isAlive = true
  }
  
  def update(playerPos: Vec3, dt: Float): Unit = {
    distance = playerPos.z
    
    // Damage when near walls
    val (distToWall, _) = RayMarcher.sceneSDF(playerPos)
    if (distToWall < 1.5f) {
      val damageRate = (1.5f - distToWall) * 20f  // More damage the closer you are
      health -= damageRate * dt
      if (health <= 0f) {
        health = 0f
        isAlive = false
        saveScore(distance.toInt)
      }
    }
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
      case _: Exception => // Ignore write errors
    }
  }
}
