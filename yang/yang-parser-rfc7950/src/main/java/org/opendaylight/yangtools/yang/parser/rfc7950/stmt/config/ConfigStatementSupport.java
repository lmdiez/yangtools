/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.rfc7950.stmt.config;

import org.opendaylight.yangtools.yang.model.api.YangStmtMapping;
import org.opendaylight.yangtools.yang.model.api.meta.EffectiveStatement;
import org.opendaylight.yangtools.yang.model.api.stmt.ConfigStatement;
import org.opendaylight.yangtools.yang.parser.rfc7950.stmt.ArgumentUtils;
import org.opendaylight.yangtools.yang.parser.spi.meta.AbstractStatementSupport;
import org.opendaylight.yangtools.yang.parser.spi.meta.StmtContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.SubstatementValidator;

public final class ConfigStatementSupport extends
    AbstractStatementSupport<Boolean, ConfigStatement, EffectiveStatement<Boolean, ConfigStatement>> {
    private static final SubstatementValidator SUBSTATEMENT_VALIDATOR = SubstatementValidator.builder(
        YangStmtMapping.CONFIG).build();
    private static final ConfigStatementSupport INSTANCE = new ConfigStatementSupport();

    private ConfigStatementSupport() {
        super(YangStmtMapping.CONFIG);
    }

    public static ConfigStatementSupport getInstance() {
        return INSTANCE;
    }

    @Override
    public Boolean parseArgumentValue(final StmtContext<?, ?, ?> ctx, final String value) {
        return ArgumentUtils.parseBoolean(ctx, value);
    }

    @Override
    public ConfigStatement createDeclared(final StmtContext<Boolean, ConfigStatement, ?> ctx) {
        final ConfigStatement ret = new ConfigStatementImpl(ctx);

        if (EmptyConfigStatement.FALSE.equals(ret)) {
            return EmptyConfigStatement.FALSE;
        } else if (EmptyConfigStatement.TRUE.equals(ret)) {
            return EmptyConfigStatement.TRUE;
        } else {
            return ret;
        }
    }

    @Override
    public EffectiveStatement<Boolean, ConfigStatement> createEffective(
            final StmtContext<Boolean, ConfigStatement, EffectiveStatement<Boolean, ConfigStatement>> ctx) {
        final EffectiveStatement<Boolean, ConfigStatement> ret = new ConfigEffectiveStatementImpl(ctx);
        final ConfigStatement declared = ret.getDeclared();
        if (declared instanceof EmptyConfigStatement && ret.effectiveSubstatements().isEmpty()) {
            return ((EmptyConfigStatement)declared).toEffective();
        }
        return ret;
    }

    @Override
    public String internArgument(final String rawArgument) {
        return ArgumentUtils.internBoolean(rawArgument);
    }

    @Override
    protected SubstatementValidator getSubstatementValidator() {
        return SUBSTATEMENT_VALIDATOR;
    }
}