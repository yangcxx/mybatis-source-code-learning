package cn.yangcx;

/**
 * TODO<br/>
 * Date: 2020/6/23 7:23 <br/>
 *
 * @author YANGCX
 */
public enum GenderEnum {

  /**
   * MALE
   */
  MALE(0),

  /**
   * FEMALE
   */
  FEMALE(1);

  private int code;

  GenderEnum(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public static GenderEnum get(int code) {
    for (GenderEnum value : GenderEnum.values()) {
      if (code == value.getCode()) {
        return value;
      }
    }
    throw new IllegalArgumentException();
  }
}
