import javax.sound.sampled._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}

object AudioSystem {
  private val SAMPLE_RATE = 44100
  private var line: Option[SourceDataLine] = None
  private var running = false
  private var cameraPos = Vec3(0, 0, 0)
  
  def init(): Unit = {
    Try {
      val format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false)
      val info = new DataLine.Info(classOf[SourceDataLine], format)
      val l = javax.sound.sampled.AudioSystem.getLine(info).asInstanceOf[SourceDataLine]
      l.open(format)
      l.start()
      line = Some(l)
      running = true
      
      Future {
        audioLoop()
      }
    } match {
      case Success(_) => println("Audio initialized")
      case Failure(e) => println(s"Audio not available (WSL): ${e.getMessage}")
    }
  }
  
  def updatePosition(pos: Vec3): Unit = {
    cameraPos = pos
  }
  
  def stop(): Unit = {
    running = false
    line.foreach { l =>
      l.stop()
      l.close()
    }
  }
  
  private def audioLoop(): Unit = {
    line.foreach { l =>
      val bufferSize = 512
      val buffer = new Array[Byte](bufferSize * 2)
      var phase1 = 0.0
      var phase2 = 0.0
      var phase3 = 0.0
      
      while (running) {
        for (i <- 0 until bufferSize) {
          val (dist, _) = RayMarcher.sceneSDF(cameraPos)
          
          val baseFreq = 60 + math.sin(cameraPos.z * 0.05).toFloat * 20
          val proximityFreq = 200 + (5f - math.min(dist, 5f)) * 80
          val proximityVolume = math.max(0, (3f - dist) / 3f) * 0.3f
          val movementFreq = 400 + math.sin(cameraPos.x * 0.1 + cameraPos.y * 0.1).toFloat * 100
          
          val ambient = math.sin(phase1).toFloat * 0.15f
          val proximity = math.sin(phase2).toFloat * proximityVolume
          val movement = math.sin(phase3).toFloat * 0.08f
          
          val sample = (ambient + proximity + movement) * 32767
          val clipped = math.max(-32767, math.min(32767, sample.toInt))
          
          buffer(i * 2) = (clipped & 0xFF).toByte
          buffer(i * 2 + 1) = ((clipped >> 8) & 0xFF).toByte
          
          phase1 += 2.0 * math.Pi * baseFreq / SAMPLE_RATE
          phase2 += 2.0 * math.Pi * proximityFreq / SAMPLE_RATE
          phase3 += 2.0 * math.Pi * movementFreq / SAMPLE_RATE
        }
        
        l.write(buffer, 0, buffer.length)
      }
    }
  }
}
