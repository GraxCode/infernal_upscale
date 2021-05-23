package me.nov.infernalupscale.exception;

import me.nov.infernalupscale.mat.MAT;

public class MalformedMatException extends RuntimeException {

  public MalformedMatException(MAT mat, String s) {
    super(mat.name + ": " + s);
  }
}
