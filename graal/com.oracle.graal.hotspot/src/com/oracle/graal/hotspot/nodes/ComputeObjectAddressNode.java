/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.hotspot.nodes;

import com.oracle.graal.compiler.common.type.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.nodeinfo.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.jvmci.meta.*;

/**
 * A high-level intrinsic for getting an address inside of an object. During lowering it will be
 * moved next to any uses to avoid creating a derived pointer that is live across a safepoint.
 */
@NodeInfo
public final class ComputeObjectAddressNode extends FixedWithNextNode implements Lowerable {
    public static final NodeClass<ComputeObjectAddressNode> TYPE = NodeClass.create(ComputeObjectAddressNode.class);

    @Input ValueNode object;
    @Input ValueNode offset;

    public ComputeObjectAddressNode(ValueNode obj, ValueNode offset) {
        super(TYPE, StampFactory.forKind(Kind.Long));
        this.object = obj;
        this.offset = offset;
    }

    @NodeIntrinsic
    public static native long get(Object array, long offset);

    @Override
    public void lower(LoweringTool tool) {
        tool.getLowerer().lower(this, tool);
    }

    public ValueNode getObject() {
        return object;
    }

    public ValueNode getOffset() {
        return offset;
    }
}