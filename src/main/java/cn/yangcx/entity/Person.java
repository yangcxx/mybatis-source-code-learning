package cn.yangcx.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.ibatis.annotations.Param;

/**
 * TODO<br/>
 * Date: 2020/6/27 11:57 <br/>
 *
 * @author YANGCX
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Person {

  private Long id;

  private String name;

  private Integer sex;

  private String address;

  private Long card;

  public Person(Long id, String name, Integer sex) {
    this.id = id;
    this.name = name;
    this.sex = sex;
  }

  public Person(@Param(value = "id") Long id, @Param(value = "name") String name) {
    this.id = id;
    this.name = name;
  }
}
