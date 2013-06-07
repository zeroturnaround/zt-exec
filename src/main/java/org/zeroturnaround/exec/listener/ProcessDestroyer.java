/*
 * Copyright (C) 2013 ZeroTurnaround <support@zeroturnaround.com>
 * Contains fragments of code from Apache Commons Exec, rights owned
 * by Apache Software Foundation (ASF).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * NOTICE: This file originates from the Apache Commons Exec package.
 * It has been modified to fit our needs.
 * 
 * The following is the original header of the file in Apache Commons Exec:  
 * 
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.zeroturnaround.exec.listener;

/**
 * Destroys all registered {@link java.lang.Process} after a certain event,
 * typically when the VM exits 
 * @see ShutdownHookProcessDestroyer
 */
public interface ProcessDestroyer {

  /**
   * Returns <code>true</code> if the specified 
   * {@link java.lang.Process} was
   * successfully added to the list of processes to be destroy.
   * 
   * @param process
   *      the process to add
   * @return <code>true</code> if the specified 
   * 		{@link java.lang.Process} was
   *      successfully added
   */
  boolean add(Process process);

  /**
   * Returns <code>true</code> if the specified 
   * {@link java.lang.Process} was
   * successfully removed from the list of processes to be destroy.
   * 
   * @param process
   *            the process to remove
   * @return <code>true</code> if the specified 
   * 		{@link java.lang.Process} was
   *      successfully removed
   */
  boolean remove(Process process);

  /**
   * Returns the number of registered processes.
   *
   * @return the number of register process
   */
  int size();
}
