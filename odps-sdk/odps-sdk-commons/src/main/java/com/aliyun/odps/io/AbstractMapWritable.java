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

package com.aliyun.odps.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.aliyun.odps.conf.Configurable;
import com.aliyun.odps.conf.Configuration;

/**
 * {@link MapWritable} 和 {@link SortedMapWritable} 的基类。
 *
 * 在一个map实例中，能够添加的{@link Writable}的类型最多是127。
 */
public abstract class AbstractMapWritable implements Writable, Configurable {

  private AtomicReference<Configuration> conf;

  /* Class to id mappings */
  Map<Class, Byte> classToIdMap = new ConcurrentHashMap<Class, Byte>();

  /* Id to Class mappings */
  Map<Byte, Class> idToClassMap = new ConcurrentHashMap<Byte, Class>();

  /* The number of new classes (those not established by the constructor) */
  private volatile byte newClasses = 0;

  /**
   * @return 已添加Class的种类数
   */
  byte getNewClasses() {
    return newClasses;
  }

  /**
   * Used to add "predefined" classes and by Writable to copy "new" classes.
   */
  private synchronized void addToMap(Class clazz, byte id) {
    if (classToIdMap.containsKey(clazz)) {
      byte b = classToIdMap.get(clazz);
      if (b != id) {
        throw new IllegalArgumentException("Class " + clazz.getName()
                                           + " already registered but maps to " + b + " and not "
                                           + id);
      }
    }
    if (idToClassMap.containsKey(id)) {
      Class c = idToClassMap.get(id);
      if (!c.equals(clazz)) {
        throw new IllegalArgumentException("Id " + id + " exists but maps to "
                                           + c.getName() + " and not " + clazz.getName());
      }
    }
    classToIdMap.put(clazz, id);
    idToClassMap.put(id, clazz);
  }

  /**
   * Add a Class to the maps if it is not already present.
   */
  protected synchronized void addToMap(Class clazz) {
    if (classToIdMap.containsKey(clazz)) {
      return;
    }
    if (newClasses + 1 > Byte.MAX_VALUE) {
      throw new IndexOutOfBoundsException("adding an additional class would"
                                          + " exceed the maximum number allowed");
    }
    byte id = ++newClasses;
    addToMap(clazz, id);
  }

  /**
   * @return the Class class for the specified id
   */
  protected Class getClass(byte id) {
    return idToClassMap.get(id);
  }

  /**
   * @return the id for the specified Class
   */
  protected byte getId(Class clazz) {
    return classToIdMap.containsKey(clazz) ? classToIdMap.get(clazz) : -1;
  }

  /**
   * Used by child copy constructors.
   */
  protected synchronized void copy(Writable other) {
    if (other != null) {
      try {
        DataOutputBuffer out = new DataOutputBuffer();
        other.write(out);
        DataInputBuffer in = new DataInputBuffer();
        in.reset(out.getData(), out.getLength());
        readFields(in);

      } catch (IOException e) {
        throw new IllegalArgumentException("map cannot be copied: "
                                           + e.getMessage());
      }

    } else {
      throw new IllegalArgumentException("source map cannot be null");
    }
  }

  /**
   * constructor.
   */
  protected AbstractMapWritable() {
    this.conf = new AtomicReference<Configuration>();

    addToMap(BooleanWritable.class,
             Byte.valueOf(Integer.valueOf(-126).byteValue()));
    addToMap(BytesWritable.class,
             Byte.valueOf(Integer.valueOf(-125).byteValue()));
    addToMap(DatetimeWritable.class,
             Byte.valueOf(Integer.valueOf(-124).byteValue()));
    addToMap(DoubleWritable.class,
             Byte.valueOf(Integer.valueOf(-123).byteValue()));
    addToMap(IntWritable.class, Byte.valueOf(Integer.valueOf(-122).byteValue()));
    addToMap(LongWritable.class,
             Byte.valueOf(Integer.valueOf(-121).byteValue()));
    addToMap(MapWritable.class, Byte.valueOf(Integer.valueOf(-120).byteValue()));
    addToMap(NullWritable.class,
             Byte.valueOf(Integer.valueOf(-119).byteValue()));
    addToMap(SortedMapWritable.class,
             Byte.valueOf(Integer.valueOf(-118).byteValue()));
    addToMap(Text.class, Byte.valueOf(Integer.valueOf(-117).byteValue()));
    addToMap(Tuple.class, Byte.valueOf(Integer.valueOf(-116).byteValue()));
  }

  /**
   * @return the conf
   */
  @Override
  public Configuration getConf() {
    return conf.get();
  }

  /**
   * @param conf
   *     the conf to set
   */
  @Override
  public void setConf(Configuration conf) {
    this.conf.set(conf);
  }

  @Override
  public void write(DataOutput out) throws IOException {

    // First write out the size of the class table and any classes that are
    // "unknown" classes

    out.writeByte(newClasses);

    for (byte i = 1; i <= newClasses; i++) {
      out.writeByte(i);
      out.writeUTF(getClass(i).getName());
    }
  }

  @Override
  public void readFields(DataInput in) throws IOException {

    // Get the number of "unknown" classes

    newClasses = in.readByte();

    // Then read in the class names and add them to our tables

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    for (int i = 0; i < newClasses; i++) {
      byte id = in.readByte();
      String className = in.readUTF();
      try {
        if (classLoader != null) {
          addToMap(Class.forName(className, false, classLoader), id);
        } else {
          addToMap(Class.forName(className), id);
        }
      } catch (ClassNotFoundException e) {
        throw new IOException("can't find class: " + className + " because "
                              + e.getMessage());
      }
    }
  }
}
