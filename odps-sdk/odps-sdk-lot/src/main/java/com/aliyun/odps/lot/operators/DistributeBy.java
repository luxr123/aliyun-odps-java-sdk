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

package com.aliyun.odps.lot.operators;

import java.util.ArrayList;
import java.util.List;

import com.aliyun.odps.lot.common.ArgumentNullException;
import com.aliyun.odps.lot.common.Reference;

import apsara.odps.lot.DistributeByProtos;
import apsara.odps.lot.Lot;

public class DistributeBy extends Operator {

  List<Reference> columns = new ArrayList<Reference>();

  public List<Reference> getColumns() {
    return columns;
  }

  @Override
  public Lot.LogicalOperator toProtoBuf() {
    assert (getParents().size() == 1);

    Lot.LogicalOperator.Builder builder = Lot.LogicalOperator.newBuilder();
    DistributeByProtos.DistributeBy.Builder db = DistributeByProtos.DistributeBy.newBuilder();
    db.setId(getId());
    db.setParentId(getParents().get(0).getId());

    for (Reference col : columns) {
      db.addColumns(col.toProtoBuf().getReference());
    }

    builder.setDistributeBy(db.build());
    return builder.build();
  }

  public DistributeBy(List<Reference> columns) {
    if (columns == null) {
      throw new ArgumentNullException("columns");
    }

    if (columns.size() == 0) {
      throw new IllegalArgumentException("You have to specify one column at least.");
    }
    this.columns = columns;
  }
}
