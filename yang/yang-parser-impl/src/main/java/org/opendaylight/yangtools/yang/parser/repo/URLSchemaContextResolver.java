/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.yang.parser.repo;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.model.repo.api.MissingSchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaContextFactory;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaRepository;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaResolutionException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceException;
import org.opendaylight.yangtools.yang.model.repo.api.SchemaSourceFilter;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.PotentialSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistration;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceRegistry;
import org.opendaylight.yangtools.yang.parser.util.ASTSchemaSource;
import org.opendaylight.yangtools.yang.parser.util.TextToASTTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public class URLSchemaContextResolver implements SchemaSourceProvider<YangTextSchemaSource> {
    private static final Logger LOG = LoggerFactory.getLogger(URLSchemaContextResolver.class);

    private final Cache<SourceIdentifier, YangTextSchemaSource> sources = CacheBuilder.newBuilder().build();
    private final Collection<SourceIdentifier> requiredSources = new ConcurrentLinkedDeque<>();
    private final AtomicReference<Optional<SchemaContext>> currentSchemaContext =
            new AtomicReference<>(Optional.<SchemaContext>absent());
    private final SchemaSourceRegistry registry;
    private final SchemaRepository repository;
    private volatile Object version = new Object();
    private volatile Object contextVersion = version;

    private URLSchemaContextResolver(final SchemaRepository repository, final SchemaSourceRegistry registry) {
        this.repository = Preconditions.checkNotNull(repository);
        this.registry = Preconditions.checkNotNull(registry);
    }

    public static URLSchemaContextResolver create(final String name) {
        final SharedSchemaRepository sharedRepo = new SharedSchemaRepository(name);
        return new URLSchemaContextResolver(sharedRepo, sharedRepo);
    }

    /**
     * Register a URL hosting a YANG Text file.
     *
     * @param url URL
     * @throws YangSyntaxErrorException When the YANG file is syntactically invalid
     * @throws IOException when the URL is not readable
     * @throws SchemaSourceException When parsing encounters general error
     */
    public URLRegistration registerSource(final URL url) throws SchemaSourceException, IOException, YangSyntaxErrorException {
        checkArgument(url != null, "Supplied URL must not be null");

        final SourceIdentifier guessedId = new SourceIdentifier(url.getFile(), Optional.<String>absent());
        final YangTextSchemaSource text = new YangTextSchemaSource(guessedId) {
            @Override
            public InputStream openStream() throws IOException {
                return url.openStream();
            }

            @Override
            protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
                return toStringHelper.add("url", url);
            }
        };

        final ASTSchemaSource ast = TextToASTTransformer.TRANSFORMATION.apply(text).checkedGet();
        LOG.trace("Resolved URL {} to source {}", url, ast);

        final SourceIdentifier resolvedId = ast.getIdentifier();
        final SchemaSourceRegistration<YangTextSchemaSource> reg = registry.registerSchemaSource(this,
                PotentialSchemaSource.create(resolvedId, YangTextSchemaSource.class, 0));

        requiredSources.add(resolvedId);
        LOG.trace("Added source {} to schema context requirements", resolvedId);
        version = new Object();

        return new AbstractURLRegistration(text) {
            @Override
            protected void removeRegistration() {
                requiredSources.remove(resolvedId);
                LOG.trace("Removed source {} from schema context requirements", resolvedId);
                version = new Object();
                reg.close();
                sources.invalidate(resolvedId);
            }
        };
    }

    /**
     * Try to parse all currently available yang files and build new schema context.
     * @return new schema context iif there is at least 1 yang file registered and
     *         new schema context was successfully built.
     */
    public Optional<SchemaContext> getSchemaContext() {
        final SchemaContextFactory factory = repository.createSchemaContextFactory(SchemaSourceFilter.ALWAYS_ACCEPT);
        Optional<SchemaContext> sc;
        Object v;
        do {
            // Spin get stable context version
            Object cv;
            do {
                cv = contextVersion;
                sc = currentSchemaContext.get();
                if (version == cv) {
                    return sc;
                }
            } while (cv != contextVersion);

            // Version has been updated
            Collection<SourceIdentifier> sources;
            do {
                v = version;
                sources = ImmutableList.copyOf(requiredSources);
            } while (v != version);

            while (true) {
                final CheckedFuture<SchemaContext, SchemaResolutionException> f = factory.createSchemaContext(sources);
                try {
                    sc = Optional.of(f.checkedGet());
                    break;
                } catch (SchemaResolutionException e) {
                    LOG.info("Failed to fully assemble schema context for {}", sources, e);
                    sources = e.getResolvedSources();
                }
            }

            synchronized (this) {
                if (contextVersion == cv) {
                    currentSchemaContext.set(sc);
                    contextVersion = v;
                }
            }
        } while (version == v);

        return sc;
    }

    @Override
    public CheckedFuture<YangTextSchemaSource, SchemaSourceException> getSource(final SourceIdentifier sourceIdentifier) {
        final YangTextSchemaSource ret = sources.getIfPresent(sourceIdentifier);
        if (ret == null) {
            return Futures.<YangTextSchemaSource, SchemaSourceException>immediateFailedCheckedFuture(
                    new MissingSchemaSourceException("URL for " + sourceIdentifier + " not registered", sourceIdentifier));
        }

        return Futures.immediateCheckedFuture(ret);
    }
}
