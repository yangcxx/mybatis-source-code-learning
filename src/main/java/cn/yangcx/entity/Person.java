package cn.yangcx.entity;

import cn.yangcx.GenderEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;

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
public class Person implements Serializable {

  private static final long serialVersionUID = -904540000351777676L;
  private Long id;

  private String name;

  private Integer age;

  private GenderEnum sex;

  private String address;

  private Long card;

  public Person(Long id, String name, GenderEnum sex) {
    this.id = id;
    this.name = name;
    this.sex = sex;
  }

  // fixme 不知道为啥，不加注解，MyBatis无法获取真实的入参名称
  public Person(@Param(value = "id") Long id, @Param(value = "name") String name) {
    this.id = id;
    this.name = name;
  }
}
