package me.nov.infernalupscale.mat;

import me.nov.infernalupscale.Utils;
import me.nov.infernalupscale.exception.MalformedMatException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class Texture {

  private final byte[] b;
  private final MAT mat;


  private int texType;
  private int colorNum;
  private int[] pads;

  private int unk1th;
  private int unk2th;
  private int unk3th;
  private int unk4th;
  private int texNum;

  public int sizeX;
  public int sizeY;
  private int transparent;
  private int[] texDataPad;
  public int numMipMaps;

  protected BufferedImage img;

  public Texture(MAT mat, byte[] bytes) {
    this.mat = mat;
    this.b = bytes;
  }

  void readTexDataHeader(ByteBuffer buffer) {
    sizeX = buffer.getInt();
    sizeY = buffer.getInt();
    verifySize();

    transparent = buffer.getInt(); // 1 = transparent, 0 = not
    texDataPad = new int[]{buffer.getInt(),buffer.getInt()};
    numMipMaps = buffer.getInt(); // Number of mipmaps in texture largest one first.
  }

  private void verifySize() {
    if (sizeX < 0 || sizeY < 0 || sizeX > 16384 || sizeY > 16384) {
      throw new MalformedMatException(mat, "Invalid size. [" + sizeX + ", " + sizeY + "]");
    }
    if (sizeX % (1 << numMipMaps) != 0 || sizeY % (1 << numMipMaps) != 0)
      throw new MalformedMatException(mat, "Size not divisible by 2^numMipMaps. [" + sizeX + ", " + sizeY + "], numMipMaps=" + numMipMaps);
  }


  void readTextureHeader(ByteBuffer buffer) {
    texType = buffer.getInt(); // 0 = color, 8 = texture
    colorNum = buffer.getInt();
    pads = new int[]{buffer.getInt(),buffer.getInt(),buffer.getInt()};
    unk1th = buffer.getInt(); // actually two word values
    unk2th = buffer.getInt();
    unk3th = buffer.getInt();
    unk4th = buffer.getInt();
    texNum = buffer.getInt(); //0 for first texture. Increases for every texture in mat.
  }

  public void writeTextureHeader(ByteBuffer buffer) {
    buffer.putInt(texType);
    buffer.putInt(colorNum);
    for (int pad : pads)
      buffer.putInt(pad);
    buffer.putInt(unk1th);
    buffer.putInt(unk2th);
    buffer.putInt(unk3th);
    buffer.putInt(unk4th);
    buffer.putInt(texNum);
  }

  public void writeTexDataHeader(ByteBuffer buffer) {
    buffer.putInt(sizeX);
    buffer.putInt(sizeY);
    buffer.putInt(transparent);
    for (int pad : texDataPad)
      buffer.putInt(pad);
    buffer.putInt(numMipMaps);
  }

  public void writeTexData(ByteBuffer buffer) {
    BufferedImage lod = img;
    for (int lodIdx = 0; lodIdx < numMipMaps; lodIdx++) {
      writePixelData(buffer, lod);
      // SCALE_SMOOTH has been tested to look the most accurate
      lod = Utils.toBufImg(lod.getScaledInstance(lod.getWidth() / 2, lod.getHeight() / 2, Image.SCALE_SMOOTH));
    }
  }

  private void writePixelData(ByteBuffer buffer, BufferedImage img) {
    int bytesPerPixel = mat.bitsPerPixel / 8;
    for (int y = 0; y < img.getHeight(); y++) {
      for (int x = 0; x < img.getWidth(); x++) {
        int argb = img.getRGB(x, y);
        int convertedRGB = encodeARGB(argb);
        if (bytesPerPixel > 2)
          buffer.putInt(convertedRGB);
        else
          buffer.putShort((short) convertedRGB);
      }
    }
  }


  public void readImage(ByteBuffer buffer) {
    img = new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_ARGB);
    int bytesPerPixel = mat.bitsPerPixel / 8;

    for (int y = 0; y < sizeY; y++) {
      for (int x = 0; x < sizeX; x++) {
        int argb = bytesPerPixel > 2 ? buffer.getInt() : buffer.getShort();
        img.setRGB(x, y, decodeARGB(argb));
      }
    }
  }

  private int encodeARGB(int argb) {
    int red = (argb >> 16) & 0xff;
    int green = (argb >> 8) & 0xff;
    int blue = (argb >> 0) & 0xff;

    // don't ask me why i need 256 instead of 255 here, but it works for every color, so keep it!
    int redRange = (int) ((red / 256.0f) * (float) (1 << mat.redBits));
    int greenRange = (int) ((green / 256.0f) * (float) (1 << mat.greenBits));
    int blueRange = (int) ((blue / 256.0f) * (float) (1 << mat.blueBits));
    if (mat.alphaBits == 0)
      return (redRange << mat.redShift) | (greenRange << mat.greenShift) | (blueRange << mat.blueShift);

    int alpha = (argb >> 24) & 0xff;
    int alphaRange = (int) ((alpha / 256.0f) * (float) (1 << mat.alphaBits));

    return (alphaRange << mat.alphaShift) | (redRange << mat.redShift) | (greenRange << mat.greenShift) | (blueRange << mat.blueShift);
  }

  private int decodeARGB(int argb) {
    int alpha = decodeColor(argb, mat.alphaBits, mat.alphaShift);
    int red = decodeColor(argb, mat.redBits, mat.redShift);
    int green = decodeColor(argb, mat.greenBits, mat.greenShift);
    int blue = decodeColor(argb, mat.blueBits, mat.blueShift);

    return (alpha << 24) | (red << 16) | (green << 8) | blue;
  }

  public int decodeColor(int argb, int bitCount, int shift) {
    int max = ((1 << bitCount) - 1);
    if (bitCount == 0)
      return 0xFF;
    if (bitCount == 1)
      return (max & (argb >> shift)) != 0 ? 0xFF : 0;
    return (int) ((max & (argb >> shift)) / (float) max * 255.0f);
  }

  public BufferedImage getImg() {
    return img;
  }

  public void setImg(BufferedImage img) {
    sizeX = img.getWidth();
    sizeY = img.getHeight();
    this.img = img;
  }
}
