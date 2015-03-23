/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.graal.graphbuilderconf;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.api.replacements.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;

/**
 * Used by a {@link GraphBuilderPlugin} to interface with a graph builder object.
 */
public interface GraphBuilderContext {

    /**
     * Information about a snippet or method substitution currently being processed by the graph
     * builder. When in the scope of a replacement, the graph builder does not check the value kinds
     * flowing through the JVM state since replacements can employ non-Java kinds to represent
     * values such as raw machine words and pointers.
     */
    public interface Replacement {

        /**
         * Gets the method being replaced.
         */
        ResolvedJavaMethod getOriginalMethod();

        /**
         * Gets the replacement method.
         */
        ResolvedJavaMethod getReplacementMethod();

        /**
         * Determines if this replacement is being inlined as a compiler intrinsic. A compiler
         * intrinsic is atomic with respect to deoptimization. Deoptimization within a compiler
         * intrinsic will restart the interpreter at the intrinsified call.
         */
        boolean isIntrinsic();
    }

    /**
     * Raw operation for adding a node to the graph when neither {@link #add},
     * {@link #addPush(ValueNode)} nor {@link #addPush(Kind, ValueNode)} can be used.
     */
    <T extends ValueNode> T append(T value);

    /**
     * Adds the given node to the graph and also adds recursively all referenced inputs.
     *
     * @param value the node to be added to the graph
     * @return either the node added or an equivalent node
     */
    <T extends ValueNode> T recursiveAppend(T value);

    /**
     * Pushes a given value to the frame state stack using an explicit kind. This should be used
     * when {@code value.getKind()} is different from the kind that the bytecode instruction
     * currently being parsed pushes to the stack.
     *
     * @param kind the kind to use when type checking this operation
     * @param value the value to push to the stack. The value must already have been
     *            {@linkplain #append(ValueNode) appended}.
     */
    void push(Kind kind, ValueNode value);

    /**
     * Appends a node with a void kind to the graph. If the node is a {@link StateSplit}, then its
     * {@linkplain StateSplit#stateAfter() frame state} is also set.
     */
    default <T extends ValueNode> T add(T value) {
        assert value.getKind() == Kind.Void;
        T appended = append(value);
        if (appended instanceof StateSplit) {
            StateSplit stateSplit = (StateSplit) appended;
            assert stateSplit.stateAfter() == null;
            stateSplit.setStateAfter(createStateAfter());
        }
        return appended;
    }

    /**
     * Adds a node with a non-void kind to the graph, pushes it to the stack. If the node is a
     * {@link StateSplit}, then its {@linkplain StateSplit#stateAfter() frame state} is also set.
     *
     * @param value the value to push to the stack. The value must already have been
     *            {@linkplain #append(ValueNode) appended}. The {@code value.getKind()} kind is used
     *            when type checking this operation.
     * @return the version of {@code value} in the graph which may be different than {@code value}
     */
    default <T extends ValueNode> T addPush(T value) {
        return addPush(value.getKind().getStackKind(), value);
    }

    /**
     * Adds a node with a non-void kind to the graph, pushes it to the stack. If the node is a
     * {@link StateSplit}, then its {@linkplain StateSplit#stateAfter() frame state} is also set.
     *
     * @param kind the kind to use when type checking this operation
     * @param value the value to push to the stack. The value must already have been
     *            {@linkplain #append(ValueNode) appended}.
     * @return the version of {@code value} in the graph which may be different than {@code value}
     */
    default <T extends ValueNode> T addPush(Kind kind, T value) {
        T appended = append(value);
        push(kind, appended);
        if (appended instanceof StateSplit) {
            StateSplit stateSplit = (StateSplit) appended;
            assert stateSplit.stateAfter() == null;
            stateSplit.setStateAfter(createStateAfter());
        }
        return appended;
    }

    StampProvider getStampProvider();

    MetaAccessProvider getMetaAccess();

    Assumptions getAssumptions();

    ConstantReflectionProvider getConstantReflection();

    SnippetReflectionProvider getSnippetReflection();

    StructuredGraph getGraph();

    /**
     * Creates a snap shot of the current frame state with the BCI of the instruction after the one
     * currently being parsed.
     */
    FrameState createStateAfter();

    /**
     * Gets the parsing context for the method that inlines the method being parsed by this context.
     */
    GraphBuilderContext getParent();

    /**
     * Gets the root method for the graph building process.
     */
    ResolvedJavaMethod getRootMethod();

    /**
     * Gets the method currently being parsed.
     */
    ResolvedJavaMethod getMethod();

    /**
     * Gets the index of the bytecode instruction currently being parsed.
     */
    int bci();

    /**
     * Gets the inline depth of this context. 0 implies this is the context for the
     * {@linkplain #getRootMethod() root method}.
     */
    int getDepth();

    /**
     * Determines if the current parsing context is a snippet or method substitution.
     */
    default boolean parsingReplacement() {
        return getReplacement() == null;
    }

    /**
     * Gets the replacement of the current parsing context or {@code null} if not
     * {@link #parsingReplacement() parsing a replacement}.
     */
    Replacement getReplacement();

    boolean eagerResolving();

    BailoutException bailout(String string);
}