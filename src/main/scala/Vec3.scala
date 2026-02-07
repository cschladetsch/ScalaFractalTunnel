case class Vec3(x: Float, y: Float, z: Float) {
  def +(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)
  def -(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)
  def *(scalar: Float): Vec3 = Vec3(x * scalar, y * scalar, z * scalar)
  def /(scalar: Float): Vec3 = Vec3(x / scalar, y / scalar, z / scalar)
  
  def dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z
  
  def cross(other: Vec3): Vec3 = Vec3(
    y * other.z - z * other.y,
    z * other.x - x * other.z,
    x * other.y - y * other.x
  )
  
  def length: Float = math.sqrt(x*x + y*y + z*z).toFloat
  def lengthSquared: Float = x*x + y*y + z*z
  def normalized: Vec3 = {
    val len = length
    if (len > 0) this / len else this
  }
}
