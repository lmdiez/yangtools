/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.data.impl.schema.tree;

import javax.annotation.Nonnull;
import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;

final class RecursiveWriteCandidateNode extends AbstractRecursiveCandidateNode {

    RecursiveWriteCandidateNode(final NormalizedNodeContainer<?, PathArgument, NormalizedNode<?, ?>> data) {
        super(data);
    }

    @Override
    @Nonnull
    public ModificationType getModificationType() {
        return ModificationType.WRITE;
    }

    @Override
    protected DataTreeCandidateNode createContainer(
            final NormalizedNodeContainer<?, PathArgument, NormalizedNode<?, ?>> childData) {
        return new RecursiveWriteCandidateNode(childData);
    }

    @Override
    protected DataTreeCandidateNode createLeaf(final NormalizedNode<?, ?> childData) {
        return new WriteLeafCandidateNode(childData);
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getDataAfter() {
        return dataOptional();
    }

    @Override
    @Nonnull
    public Optional<NormalizedNode<?, ?>> getDataBefore() {
        return Optional.absent();
    }
}