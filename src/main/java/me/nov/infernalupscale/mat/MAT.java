package me.nov.infernalupscale.mat;

import me.nov.infernalupscale.Utils;
import me.nov.infernalupscale.exception.MalformedMatException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MAT {
  public static final int MAT_HEADER_LEN = 19;
  public static final int TEX_HEADER_LEN = 10;
  public static final int TEXDATA_HEADER_LEN = 6;

  public final String name;
  public final List<Texture> textures;

  private int numTexOrColors;
  private int numTexOrZero;
  private int colorMode;
  public int bitsPerPixel;

  public int redBits;
  public int greenBits;
  public int blueBits;

  public int redShift;
  public int greenShift;
  public int blueShift;

  public int redBitDif;
  public int greenBitDif;
  public int blueBitDif;

  public int alphaBits;
  public int alphaShift;
  public int alphaBitDif;

  /*
   * STRUCTURE
   *
   * -- MAT HEADER --
   * -- TEXTURE HEADERS --
   *
   * each texture:
   * -- TEX DATA HEADER --
   * -- PIXEL DATA --
   *
   * EOF
   */

  public MAT(String name, byte[] bytes) {
    this.name = name;

    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    readMatHeader(buffer);

    textures = new ArrayList<>();
    for (int texIdx = 0; texIdx < numTexOrColors; texIdx++) {
      Texture tx = new Texture(this, bytes);
      tx.readTextureHeader(buffer);
      textures.add(tx);
    }

    int bytesPerPix = bitsPerPixel / 8;
    for (Texture tx : textures) {
      tx.readTexDataHeader(buffer);
      tx.readImage(buffer);

      // skip everything after LOD 0 mipmap, don't read them
      for (int mmIdx = 1; mmIdx < tx.numMipMaps; mmIdx++) {
        buffer.position(buffer.position() + bytesPerPix * (tx.sizeX / (1 << mmIdx)) * (tx.sizeY / (1 << mmIdx)));
      }
    }
    if (buffer.remaining() != 0)
      throw new MalformedMatException(this, "Unexpected offset ending, " + buffer.remaining() + " remaining.");
  }


  private void readMatHeader(ByteBuffer buffer) {
    int magic = buffer.getInt();
    if (magic != Integer.reverseBytes(0x4D415420))
      throw new MalformedMatException(this, "Invalid magic. Expected 'MAT ' but got " + Utils.toHex(magic));

    int version = buffer.getInt();
    if (version != 0x32)
      throw new MalformedMatException(this, "Unsupported version, expected 0x32 but got " + Utils.toHex(version));

    int type = buffer.getInt();
    if (type != 0x2)
      throw new MalformedMatException(this, "Unsupported type, expected 2 but got " + Utils.toHex(type));

    numTexOrColors = buffer.getInt(); // number of textures or colors
    numTexOrZero = buffer.getInt(); // In color MATs, it's 0, in TX ones, it's equal to numTexOrColors
    colorMode = buffer.getInt(); // either 1 or 2, i.e. Indexed, RGB, RGBA
    bitsPerPixel = buffer.getInt();
    if (bitsPerPixel % 8 != 0)
      throw new MalformedMatException(this, "Bits per pixel not convertible to bytes");

    redBits = buffer.getInt();
    greenBits = buffer.getInt();
    blueBits = buffer.getInt();

    redShift = buffer.getInt();
    greenShift = buffer.getInt();
    blueShift = buffer.getInt();

    redBitDif = buffer.getInt();
    greenBitDif = buffer.getInt();
    blueBitDif = buffer.getInt();

    alphaBits = buffer.getInt();
    alphaShift = buffer.getInt();
    alphaBitDif = buffer.getInt();
  }

  public byte[] toByteArray() {
    ByteBuffer buffer = ByteBuffer.allocate(getByteLength());
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    writeMATHeader(buffer);
    for (Texture tx : textures) {
      tx.writeTextureHeader(buffer);
    }
    for (Texture tx : textures) {
      tx.writeTexDataHeader(buffer);
      tx.writeTexData(buffer);
    }
    return buffer.array();
  }

  private int getByteLength() {
    int texCount = textures.size();
    int size = MAT_HEADER_LEN * 4;
    size += TEX_HEADER_LEN * 4 * texCount;
    for (Texture tx : textures) {
      int bytesPerPix = bitsPerPixel / 8;
      for (int mmIdx = 0; mmIdx < tx.numMipMaps; mmIdx++) {
        size += bytesPerPix * (tx.sizeX / (1 << mmIdx)) * (tx.sizeY / (1 << mmIdx));
      }
      size += TEXDATA_HEADER_LEN * 4;
    }
    return size;
  }

  private void writeMATHeader(ByteBuffer buffer) {
    buffer.putInt(Integer.reverseBytes(0x4D415420));
    buffer.putInt(0x32);
    buffer.putInt(0x2);
    buffer.putInt(numTexOrColors);
    buffer.putInt(numTexOrZero);
    buffer.putInt(colorMode);
    buffer.putInt(bitsPerPixel);

    buffer.putInt(redBits);
    buffer.putInt(greenBits);
    buffer.putInt(blueBits);

    buffer.putInt(redShift);
    buffer.putInt(greenShift);
    buffer.putInt(blueShift);

    buffer.putInt(redBitDif);
    buffer.putInt(greenBitDif);
    buffer.putInt(blueBitDif);

    buffer.putInt(alphaBits);
    buffer.putInt(alphaShift);
    buffer.putInt(alphaBitDif);
  }
}
