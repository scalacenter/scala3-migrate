package implicits

import zio.blocking.{Blocking, effectBlockingIO}

import java.io.IOException
import zio.{Chunk, ZIO, ZOutputStream}

object PrivateImplicits {

  def fromOutputStream(os: java.io.OutputStream): ZOutputStream = new ZOutputStream {
    def write(chunk: Chunk[Byte]): ZIO[Blocking, IOException, Unit] =
      effectBlockingIO[Unit] {
        os.write(chunk.toArray[Byte])
      }
  }

}