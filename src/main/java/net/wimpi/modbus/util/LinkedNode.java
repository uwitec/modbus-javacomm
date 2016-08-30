/***
 * Copyright 2002-2010 jamod development team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***/

/***
 * Copied with style from
 * Lea, Doug: "Concurrent Programming in Java: Design Principles and Patterns",
 * Second Edition, Addison-Wesley, ISBN 0-201-31009-0, November 1999
 ***/
package net.wimpi.modbus.util;

/**
 * Class defining a linked node element.
 *
 * @author Doug Lea, Dieter Wimberger
 * @version 1.2
 */
public class LinkedNode {

    /**
     * The M node.
     */
    protected Object m_Node;
    /**
     * The M next node.
     */
    protected LinkedNode m_NextNode = null;

    /**
     * Instantiates a new Linked node.
     *
     * @param node the node
     */
    public LinkedNode(Object node) {
    m_Node = node;
  }//constructor(Object)

    /**
     * Instantiates a new Linked node.
     *
     * @param node       the node
     * @param linkednode the linkednode
     */
    public LinkedNode(Object node, LinkedNode linkednode) {
    m_Node = node;
    m_NextNode = linkednode;
  }//constructor(Object,LinkedNode)

}//LinkedNode
