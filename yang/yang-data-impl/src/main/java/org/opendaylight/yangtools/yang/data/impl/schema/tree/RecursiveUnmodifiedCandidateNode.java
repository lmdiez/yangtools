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

final class RecursiveUnmodifiedCandidateNode extends AbstractRecursiveCandidateNode {
    protected RecursiveUnmodifiedCandidateNode(final NormalizedNodeContainer<?, PathArgument, NormalizedNode<?, ?>> data) {
        super(data);
    }

    @Override
    @Nonnull
    public ModificationType getModificationType() {
        return ModificationType.UNMODIFIED;
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getDataAfter() {
        return dataOptional();
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getDataBefore() {
        return dataOptional();
    }

    @Override
    protected DataTreeCandidateNode createContainer(final NormalizedNodeContainer<?, PathArgument, NormalizedNode<?, ?>> childData) {
        return new RecursiveUnmodifiedCandidateNode(childData);
    }

    @Override
    protected DataTreeCandidateNode createLeaf(final NormalizedNode<?, ?> childData) {
        return new UnmodifiedLeafCandidateNode(childData);
    }
}
