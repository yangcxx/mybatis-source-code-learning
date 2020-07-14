/**
 * Copyright ${license.git.copyrightYears} the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.yangcx;

import cn.yangcx.entity.Person;
import cn.yangcx.mapper.DemoMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * TODO<br/>
 * Date: 2020/6/21 21:52 <br/>
 *
 * @author YANGCX
 */
public class InitTest {

  public static void main(String[] args) throws IOException {
    String resource = "mybatis-config.xml";
    InputStream inputStream = Resources.getResourceAsStream(resource);
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    // 默认情况下启用缓存
    SqlSession sqlSession = sqlSessionFactory.openSession();
    // 通过动态代理执行真正的业务代码
    DemoMapper mapper = sqlSession.getMapper(DemoMapper.class);
    //Map<String, Object> map = new HashMap<>(2);
    //map.put("id", 1);
    //map.put("name", "yang");
    //System.out.println(mapper.selectAll(map));
    Person person = mapper.findById(1L);
    System.out.println(person);
    //Map<String, Object> yangcx = mapper.findWith$("yangcx");
    //System.out.println(yangcx);
    //Person p = new Person();
    //p.setName("yangcx");
    //p.setAddress("chengdu");
    //p.setCard(123456L);
    //p.setSex(1);
    //mapper.add(p);
    sqlSession.close();
  }

}
