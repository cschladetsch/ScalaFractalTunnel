import javax.sound.midi._

object InteractiveMusicSystem {
  private var sequencer: Sequencer = _
  private var synthesizer: Synthesizer = _
  private var receiver: Receiver = _
  
  private val DRUM_CHANNEL = 9
  private val BASS_CHANNEL = 0
  private val SYNTH_CHANNEL = 1
  private val PAD_CHANNEL = 2
  
  // Drum MIDI notes (General MIDI)
  private val KICK = 36
  private val SNARE = 38
  private val CLOSED_HH = 42
  private val OPEN_HH = 46
  private val CRASH = 49
  private val RIDE = 51
  
  private var isPlaying = false
  private var currentIntensity = 0f
  private var baseTempo = 100
  private var beatCount = 0L
  private var lastBeatTime = 0L
  
  def init(): Unit = {
    try {
      sequencer = MidiSystem.getSequencer(false)
      sequencer.open()
      
      synthesizer = MidiSystem.getSynthesizer()
      synthesizer.open()
      
      receiver = synthesizer.getReceiver()
      
      // Setup instruments
      setInstrument(BASS_CHANNEL, 38)  // Synth bass
      setInstrument(SYNTH_CHANNEL, 80) // Square lead
      setInstrument(PAD_CHANNEL, 91)   // Pad 3 (polysynth)
      
      // Start the music thread
      startMusicLoop()
      
      isPlaying = true
      println("Interactive music system initialized")
    } catch {
      case e: Exception =>
        println(s"Music system failed to initialize: ${e.getMessage}")
        isPlaying = false
    }
  }
  
  def stop(): Unit = {
    isPlaying = false
    if (receiver != null) receiver.close()
    if (synthesizer != null) synthesizer.close()
    if (sequencer != null) sequencer.close()
  }
  
  private def setInstrument(channel: Int, program: Int): Unit = {
    val msg = new ShortMessage()
    msg.setMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0)
    receiver.send(msg, -1)
  }
  
  private def noteOn(channel: Int, note: Int, velocity: Int): Unit = {
    if (!isPlaying) return
    try {
      val msg = new ShortMessage()
      msg.setMessage(ShortMessage.NOTE_ON, channel, note, velocity)
      receiver.send(msg, -1)
    } catch {
      case _: Exception => // Ignore
    }
  }
  
  private def noteOff(channel: Int, note: Int): Unit = {
    if (!isPlaying) return
    try {
      val msg = new ShortMessage()
      msg.setMessage(ShortMessage.NOTE_OFF, channel, note, 0)
      receiver.send(msg, -1)
    } catch {
      case _: Exception => // Ignore
    }
  }
  
  def update(speed: Float, distance: Float, health: Float, wallProximity: Float): Unit = {
    // Map speed to intensity (0.0 - 1.0)
    currentIntensity = math.min((speed - 3f) / 15f, 1f)
    
    // Tempo increases with speed
    baseTempo = (100 + currentIntensity * 40).toInt
  }
  
  def onCheckpoint(): Unit = {
    if (!isPlaying) return
    noteOn(DRUM_CHANNEL, CRASH, 110)
    Thread.sleep(100)
    noteOff(DRUM_CHANNEL, CRASH)
  }
  
  def onDamage(): Unit = {
    if (!isPlaying) return
    // Harsh distorted hit
    noteOn(SYNTH_CHANNEL, 30, 127)
    noteOn(DRUM_CHANNEL, SNARE, 127)
    Thread.sleep(50)
    noteOff(SYNTH_CHANNEL, 30)
    noteOff(DRUM_CHANNEL, SNARE)
  }
  
  def onCombo(): Unit = {
    if (!isPlaying) return
    // Rising arpeggio
    for (i <- 0 until 3) {
      noteOn(SYNTH_CHANNEL, 60 + i * 4, 90)
      Thread.sleep(50)
      noteOff(SYNTH_CHANNEL, 60 + i * 4)
    }
  }
  
  def onPickup(): Unit = {
    if (!isPlaying) return
    noteOn(SYNTH_CHANNEL, 72, 100)
    Thread.sleep(80)
    noteOff(SYNTH_CHANNEL, 72)
  }
  
  private def startMusicLoop(): Unit = {
    val musicThread = new Thread(() => {
      while (isPlaying) {
        try {
          val beatInterval = (60000 / baseTempo).toLong
          val currentTime = System.currentTimeMillis()
          
          if (currentTime - lastBeatTime >= beatInterval) {
            playBeat()
            beatCount += 1
            lastBeatTime = currentTime
          }
          
          Thread.sleep(10)
        } catch {
          case _: InterruptedException => 
          case e: Exception => println(s"Music loop error: ${e.getMessage}")
        }
      }
    })
    
    musicThread.setDaemon(true)
    musicThread.start()
  }
  
  private def playBeat(): Unit = {
    val beat = (beatCount % 16).toInt
    val intensity = currentIntensity
    
    // KICK DRUM - Basic 4/4 industrial pattern
    if (beat % 4 == 0) {
      noteOn(DRUM_CHANNEL, KICK, (90 + intensity * 37).toInt)
      scheduleNoteOff(DRUM_CHANNEL, KICK, 100)
    }
    
    // Add more kicks at high intensity
    if (intensity > 0.6f && (beat == 2 || beat == 6 || beat == 10)) {
      noteOn(DRUM_CHANNEL, KICK, (70 + intensity * 30).toInt)
      scheduleNoteOff(DRUM_CHANNEL, KICK, 80)
    }
    
    // SNARE - Backbeat
    if (beat == 4 || beat == 12) {
      noteOn(DRUM_CHANNEL, SNARE, (80 + intensity * 40).toInt)
      scheduleNoteOff(DRUM_CHANNEL, SNARE, 100)
    }
    
    // HI-HAT - Increases with intensity
    if (intensity > 0.3f) {
      if (beat % 2 == 0) {
        noteOn(DRUM_CHANNEL, CLOSED_HH, (50 + intensity * 30).toInt)
        scheduleNoteOff(DRUM_CHANNEL, CLOSED_HH, 40)
      }
      
      // 16th notes at high intensity
      if (intensity > 0.7f && beat % 2 == 1) {
        noteOn(DRUM_CHANNEL, CLOSED_HH, (40 + intensity * 20).toInt)
        scheduleNoteOff(DRUM_CHANNEL, CLOSED_HH, 30)
      }
    }
    
    // BASS LINE - Dark industrial bass
    if (intensity > 0.2f) {
      val bassPattern = Array(36, 36, 43, 36, 41, 36, 43, 36) // Root, 5th, 4th pattern
      val bassNote = bassPattern(beat % 8)
      
      if (beat % 2 == 0) {
        noteOn(BASS_CHANNEL, bassNote, (60 + intensity * 40).toInt)
        scheduleNoteOff(BASS_CHANNEL, bassNote, 180)
      }
    }
    
    // SYNTH PAD - Atmospheric drone at medium-high intensity
    if (intensity > 0.5f && beat % 8 == 0) {
      val padNotes = Array(48, 52, 55) // Minor triad
      for (note <- padNotes) {
        noteOn(PAD_CHANNEL, note, (30 + intensity * 20).toInt)
      }
      
      // Hold for 2 beats
      new Thread(() => {
        Thread.sleep(500)
        for (note <- padNotes) {
          noteOff(PAD_CHANNEL, note)
        }
      }).start()
    }
    
    // RIDE CYMBAL - At very high intensity
    if (intensity > 0.8f && beat % 4 == 2) {
      noteOn(DRUM_CHANNEL, RIDE, (50 + intensity * 30).toInt)
      scheduleNoteOff(DRUM_CHANNEL, RIDE, 150)
    }
  }
  
  private def scheduleNoteOff(channel: Int, note: Int, delayMs: Long): Unit = {
    new Thread(() => {
      Thread.sleep(delayMs)
      noteOff(channel, note)
    }).start()
  }
}
