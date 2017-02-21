/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.odps.tunnel;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.odps.Column;
import com.aliyun.odps.TableSchema;
import com.aliyun.odps.type.TypeInfo;
import com.aliyun.odps.type.TypeInfoParser;

class TunnelTableSchema extends TableSchema {

  public TunnelTableSchema(JSONObject node) {
    JSONArray columns = node.getJSONArray("columns");
    for (int i = 0; i < columns.size(); ++i) {
      JSONObject column = columns.getJSONObject(i);
      Column col = parseColumn(column);
      addColumn(col);
    }

    columns = node.getJSONArray("partitionKeys");
    for (int i = 0; i < columns.size(); ++i) {
      JSONObject column = columns.getJSONObject(i);
      Column col = parseColumn(column);
      addPartitionColumn(col);
    }
  }

  private Column parseColumn(JSONObject column) {
    String name = column.getString("name");
    String type = column.getString("type");
    String comment = column.getString("comment");
    Column col = null;
    TypeInfo typeInfo = TypeInfoParser.getTypeInfoFromTypeString(type);
    col = new Column(name, typeInfo, comment);

    return col;
  }
}
