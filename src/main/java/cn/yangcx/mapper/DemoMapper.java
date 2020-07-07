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
package cn.yangcx.mapper;

import cn.yangcx.entity.Person;

import java.util.List;
import java.util.Map;

/**
 * TODO<br/>
 * Date: 2020/6/21 20:21 <br/>
 *
 * @author YANGCX
 */
public interface DemoMapper {

  /**
   * #{} 占位符
   * @param map
   * @return
   */
  List<Map<String, Object>> selectAll(Map<String, Object> map);

  /**
   * id精确查找
   * @param id id
   * @return person
   */
  Person findById(Long id);

  /**
   * ${} 占位符
   * @param name
   * @return
   */
  Map<String, Object> findWith$(String name);

}
